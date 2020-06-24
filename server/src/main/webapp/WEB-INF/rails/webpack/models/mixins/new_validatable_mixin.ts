/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import _ from "lodash";
import {Errors} from "models/mixins/errors";
import {BaseErrorsConsumer, ErrorsConsumer} from "models/mixins/errors_consumer";
import {ErrorMessages} from "models/mixins/error_messages";
import {applyMixins} from "models/mixins/mixins";
import s from "underscore.string";

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
      const isValid = ("function" === typeof p) ? p().isValid() : p.isValid();
      if (!isValid) {
        entity.errors().add(attr, this.options.message);
      }
    });
  }
}

class NonEmptyCollectionValidator<T = {}> extends Validator {
  protected doValidate(entity: any, attr: string): void {
    const property: Iterable<T> = ("function" === typeof entity[attr] ? entity[attr]() : entity[attr]) as Iterable<T>;

    if (isEmpty(property)) {
      entity.errors().add(attr, this.options.message || `${attr} cannot be empty`);
    }
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

class MutualExclusivityValidator extends Validator {
  private attrTwo: string;

  constructor(attrTwo: string, options: ValidatorOptions = {}) {
    super(options);
    this.attrTwo = attrTwo;
  }

  protected doValidate(entity: any, attrName: string): void {
    if (isEmpty(entity[attrName]()) && isEmpty(entity[this.attrTwo]())) {
      entity.errors().add(attrName, this.options.message || ErrorMessages.mutuallyExclusive(attrName, this.attrTwo));
      return;
    }

    if (!isEmpty(entity[attrName]()) && !isEmpty(entity[this.attrTwo]())) {
      entity.errors().add(attrName, this.options.message || ErrorMessages.mutuallyExclusive(attrName, this.attrTwo));
    }
  }
}

class ChildAttrUniquenessValidator extends Validator {
  private childAttr: string;
  constructor(childAttr: string, options: ValidatorOptions = {}) {
    super(options);
    this.childAttr = childAttr;
  }

  protected doValidate(entity: any, attrName: string): void {
    _.forEach(entity[attrName](), (child) => {
      const duplicates = _.filter(entity[attrName](), (c) => {
        return (c[this.childAttr]() === child[this.childAttr]() &&
          c !== child);
      });

      if (!_.isEmpty(duplicates)) {
        //Need to add at the entity level because we clear the child errors
        //when we validate the children
        entity.errors().add(attrName, this.options.message || ErrorMessages.duplicate(this.childAttr));
        //Need to add the error to the child because we want the error to be
        //visible from the UI
        child.errors().add(this.childAttr, this.options.message || ErrorMessages.duplicate(this.childAttr));
      }

    });
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

class PositiveNumberValidator extends Validator {
  protected doValidate(entity: any, attr: string): void {
    const value = +entity[attr]();
    if (isNaN(value)) {
      entity.errors().add(attr, this.options.message || ErrorMessages.mustBePositiveNumber(attr));
    } else {
      if (value < 0) {
        entity.errors().add(attr, this.options.message || ErrorMessages.mustBePositiveNumber(attr));
      }
    }
  }
}

/** Works for Set and Map instances too */
function isEmpty<T>(i: Iterable<T>): boolean {
  if (void 0 === i) { return true; }

  if ("string" === typeof i || "length" in i) {
    return (i as { length: number }).length === 0;
  }

  if ("size" in i) {
    return (i as { size: number }).size === 0;
  }

  // last resort, but if `i` does not implement Iterable, this test will return true
  return Array.from(i).length === 0;
}

export interface Validatable {
  errors: (container?: Errors) => Errors;
  isValid: () => boolean;
  validate: (attr?: string) => Errors;
}

export class ValidatableMixin extends BaseErrorsConsumer implements Validatable, ErrorsConsumer {
  private __attrToValidators: any            = {};
  private __associationsToValidate: string[] = [];

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

  validateNonEmptyCollection(attr: string, options?: ValidatorOptions): void {
    this.validateWith(new NonEmptyCollectionValidator(options), attr);
  }

  validateFormatOf(attr: string, format: RegExp, options?: ValidatorOptions): void {
    this.validateWith(new FormatValidator(format, options), attr);
  }

  validateUrlPattern(attr: string, options?: ValidatorOptions): void {
    this.validateWith(new UrlPatternValidator(options), attr);
  }

  validateIdFormat(attr: string, options?: ValidatorOptions): void {
    const defaultMessage = ErrorMessages.idFormat(attr);
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

  validatePositiveNumber(attr: string, options?: ValidatorOptions) {
    this.validateWith(new PositiveNumberValidator(options), attr);
  }

  validateEach(attr: string) {
    this.validateWith(new AssociatedListValidator(), attr);
  }

  validateChildAttrIsUnique(attr: string, childAttr: string, options?: ValidatorOptions): void {
    this.validateWith(new ChildAttrUniquenessValidator(childAttr, options), attr);
  }

  validateMutualExclusivityOf(attrOne: string, attrTwo: string, options?: ValidatorOptions): void {
    this.validateWith(new MutualExclusivityValidator(attrTwo, options), attrOne);
  }

  validateAssociated(association: string): void {
    this.__associationsToValidate.push(association);
  }
}

// Without this, classes that do mix in rather than extend this class will not
// get the ErrorsConsumer methods, and typescript won't catch it.
applyMixins(ValidatableMixin, BaseErrorsConsumer);
