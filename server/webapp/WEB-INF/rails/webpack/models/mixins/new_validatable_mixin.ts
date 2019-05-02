/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as _ from "lodash";
import * as stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {ErrorMessages} from "models/mixins/validatable";
import * as s from "underscore.string";

export interface ValidatorOptions {
  message?: string;
  condition?: () => boolean;
}

export abstract class Validator {
  protected readonly options: ValidatorOptions;

  constructor(options: ValidatorOptions = {}) {
    this.options = options;
  }

  validate(entity: any, attr: string): void {
    if (this.options.condition && !this.options.condition()) {
      return;
    }

    this.doValidate(entity, attr);
  }

  protected abstract doValidate(entity: any, attr: string): void;
}

class AssociatedListValidator extends Validator {
  protected doValidate(entity: any, attr: string): void {
    _.forEach(entity[attr](), (p) => {
      if (!p.isValid()) {
        entity.errors().add(attr, this.options.message);
      }
    });
  }
}

class PasswordPresenceValidator extends Validator {
  protected doValidate(entity: any, attr: string): void {
    if (s.isBlank(entity[attr]().value())) {
      entity.errors().add(attr, this.options.message || ErrorMessages.mustBePresent(attr));
    }
  }
}

class PresenceValidator extends Validator {
  protected doValidate(entity: any, attr: string): void {
    if (s.isBlank(entity[attr]())) {
      entity.errors().add(attr, this.options.message || ErrorMessages.mustBePresent(attr));
    }
  }
}

class MaxLengthValidator extends Validator {
  private readonly maxLength: number;

  constructor(maxLength: number, options?: ValidatorOptions) {
    super(options);
    this.maxLength = maxLength;
  }

  protected doValidate(entity: any, attr: string): void {
    if (s.isBlank(entity[attr]())) {
      return;
    }

    if (entity[attr]().length > this.maxLength) {
      entity.errors().add(attr, this.options.message || ErrorMessages.mustNotExceedMaxLength(attr, this.maxLength));
    }
  }
}

class UniquenessValidator extends Validator {
  private otherElements: () => any[];

  constructor(otherElements: () => any[], options: ValidatorOptions = {}) {
    super(options);
    this.otherElements = otherElements;
  }

  protected doValidate(entity: any, attrName: string): void {
    if (s.isBlank(entity[attrName]())) {
      return;
    }
    const duplicate = (this.otherElements)().find((element: any) => element[attrName]() === entity[attrName]());
    if (!_.isEmpty(duplicate)) {
      entity.errors().add(attrName, this.options.message || ErrorMessages.duplicate(attrName));
    }
  }
}

class FormatValidator extends Validator {
  private readonly format: RegExp;

  constructor(format: RegExp, options?: ValidatorOptions) {
    super(options);
    this.format = format;
  }

  protected doValidate(entity: any, attrName: string): void {
    if (s.isBlank(entity[attrName]())) {
      return;
    }

    if (!entity[attrName]().match(this.format)) {
      entity.errors().add(attrName, this.options.message || `${s.humanize(attrName)} format is invalid`);
    }
  }
}

class UrlPatternValidator extends Validator {
  private static URL_REGEX = /^http(s)?:\/\/.+/;

  protected doValidate(entity: any, attrName: string): void {
    if (s.isBlank(entity[attrName]())) {
      return;
    }

    if (!entity[attrName]().match(UrlPatternValidator.URL_REGEX)) {
      entity.errors().add(attrName, this.options.message || ErrorMessages.mustBeAUrl(attrName));
    }
  }
}

export interface Validatable {
  errors: (container?: Errors) => Errors;
  isValid: () => boolean;
  validate: (attr?: string) => Errors;
}

export class ValidatableMixin implements Validatable {
  private __errors                           = stream(new Errors());
  private __attrToValidators: any            = {};
  private __associationsToValidate: string[] = [];

  errors(newVal?: Errors): Errors {
    if (arguments.length > 0) {
      this.__errors(newVal as Errors);
    }
    return this.__errors();
  }

  clearErrors(attr?: string) {
    return attr ? this.errors().clear(attr) : this.errors().clear();
  }

  validate(attr?: string) {
    const attrs = attr ? [attr] : _.keys(this.__attrToValidators);

    this.clearErrors(attr);

    _.forEach(attrs, (attr: string) => {
      _.forEach(this.__attrToValidators[attr], (validator: Validator) => {
        validator.validate(this, attr);
      });
    });

    return this.errors();
  }

  isValid(): boolean {
    this.validate();
    // run these before composing with && because we DON'T want to guard
    // the association validations if there are attribute errors; we want to
    // run ALL validations up front.
    const attributeErrors = _.isEmpty(this.errors().errors());
    const descendantErrors = this.allAssociationsValid();

    return attributeErrors && descendantErrors;
  }

  allAssociationsValid(): boolean {
    // internally use _.map() to ensure we run ALL validations, instead of stopping on
    // the first failure, then wrap with _.every() to output the result
    return _.every(_.map(this.__associationsToValidate, (association: string) => {
      const property = (this as any)[association];
      return (property && property()) ? property().isValid() : true;
    }), Boolean);
  }

  validateWith<T extends Validator>(validator: T, attr: string): void {
    if (_.has(this.__attrToValidators, attr)) {
      this.__attrToValidators[attr].push(validator);
    } else {
      this.__attrToValidators[attr] = [validator];
    }
  }

  validatePresenceOf(attr: string, options?: ValidatorOptions): void {
    this.validateWith(new PresenceValidator(options), attr);
  }

  validatePresenceOfPassword(attr: string, options?: ValidatorOptions): void {
    this.validateWith(new PasswordPresenceValidator(options), attr);
  }

  validateUniquenessOf(attr: string, siblings: () => any[], options?: ValidatorOptions): void {
    this.validateWith(new UniquenessValidator(siblings, options), attr);
  }

  validateFormatOf(attr: string, format: RegExp, options?: ValidatorOptions): void {
    this.validateWith(new FormatValidator(format, options), attr);
  }

  validateUrlPattern(attr: string, options?: ValidatorOptions): void {
    this.validateWith(new UrlPatternValidator(options), attr);
  }

  validateIdFormat(attr: string, options?: ValidatorOptions): void {
    const defaultMessage = `Invalid ${attr}. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.`;
    if (!options) {
      options = {message: defaultMessage};
    }
    if (!options.message) {
      options.message = defaultMessage;
    }
    this.validateFormatOf(attr, /(^[-a-zA-Z0-9_])([-a-zA-Z0-9_.]{0,254})$/, options);
  }

  validateMaxLength(attr: string, maxAllowedLength: number, options?: ValidatorOptions) {
    this.validateWith(new MaxLengthValidator(maxAllowedLength, options), attr);
  }

  validateEach(attr: string) {
    this.validateWith(new AssociatedListValidator(), attr);
  }

  validateAssociated(association: string): void {
    this.__associationsToValidate.push(association);
  }
}
