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

interface ValidatorOptions {
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

  protected abstract doValidate(entity: any, attrName: string): void;
}

class PresenceValidator extends Validator {
  protected doValidate(entity: any, attr: string): void {
    if (s.isBlank(entity[attr])) {
      entity.errors().add(attr, this.options.message || ErrorMessages.mustBePresent(attr));
    }
  }
}

export class ValidatableMixin {
  errors                = stream(new Errors());
  attrToValidators: any = {};

  clearErrors(attr?: string) {
    return attr ? this.errors().clear(attr) : this.errors().clear();
  }

  validate(attr?: string) {
    const attrs = attr ? [attr] : _.keys(this.attrToValidators);

    this.clearErrors(attr);

    _.forEach(attrs, (attr: string) => {
      _.forEach(this.attrToValidators[attr], (validator: Validator) => {
        validator.validate(this, attr);
      });
    });

    return this.errors();
  }

  validateWith(validator: Validator, attr: string): void {
    if (_.has(this.attrToValidators, attr)) {
      this.attrToValidators[attr].push(validator);
    } else {
      this.attrToValidators[attr] = [validator];
    }
  }

  validatePresenceOf(attr: string, options?: ValidatorOptions): void {
    this.validateWith(new PresenceValidator(options), attr);
  }
}
