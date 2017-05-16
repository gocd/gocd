/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const _                 = require('lodash');
const s                 = require('string-plus');
const Errors            = require('models/mixins/errors');
const Mixins            = require('models/mixins/model_mixins');
const PresenceValidator = function (options) {
  this.validate = (entity, attr) => {
    if (options.condition && (!options.condition(entity))) {
      return;
    }

    if (s.isBlank(entity[attr]())) {
      entity.errors().add(attr, Validatable.ErrorMessages.mustBePresent(attr));
    }
  };
};

const UniquenessValidator = function () {
  this.validate = (entity, attr) => {
    if (_.isNil(entity.parent()) || s.isBlank(entity[attr]())) {
      return;
    }

    if (!entity.parent().isUnique(entity, attr)) {
      entity.errors().add(attr, Validatable.ErrorMessages.duplicate(attr));
    }
  };
};

const UrlPatternValidator = function () {
  const URL_REGEX = /^http(s)?:\/\/.+/;

  this.validate = (entity, attr) => {
    if (s.isBlank(entity[attr]())) {
      return;
    }

    if (!entity[attr]().match(URL_REGEX)) {
      entity.errors().add(attr, Validatable.ErrorMessages.mustBeAUrl(attr));
    }
  };
};

const FormatValidator = function({format, message}) {
  this.validate = (entity, attr) => {
    if (s.isBlank(entity[attr]())) {
      return;
    }

    if (!entity[attr]().match(format)) {
      entity.errors().add(attr, message || (`${s.humanize(attr)} format is in valid`));
    }
  };
};

const Validatable = function({errors}) {
  const self                   = this;
  const attrToValidators       = {};
  const associationsToValidate = [];
  self.errors                  = Mixins.GetterSetter(new Errors(errors));

  const validateWith = (validator, attr) => {
    _.has(attrToValidators, attr) ? attrToValidators[attr].push(validator) : attrToValidators[attr] = [validator];
  };

  const clearErrors = (attr) => {
    attr ? self.errors().clear(attr) : self.errors().clear();
  };

  self.validatePresenceOf = (attr, options) => {
    validateWith(new PresenceValidator(options || {}), attr);
  };

  self.validateUniquenessOf = (attr, options) => {
    validateWith(new UniquenessValidator(options || {}), attr);
  };

  self.validateFormatOf = (attr, options) => {
    validateWith(new FormatValidator(options || {}), attr);
  };

  self.validateUrlPattern = (attr, options) => {
    validateWith(new UrlPatternValidator(options || {}), attr);
  };

  self.validateWith = (attr, validator) => {
    validateWith(new validator(), attr);
  };

  self.validateAssociated = (association) => {
    associationsToValidate.push(association);
  };

  self.validate = (attr) => {
    const attrs = attr ? [attr] : _.keys(attrToValidators);

    clearErrors(attr);

    _.forEach(attrs, (attr) => {
      _.forEach(attrToValidators[attr], (validator) => {
        validator.validate(self, attr);
      });
    });

    return self.errors();
  };

  self.isValid = () => {
    self.validate();

    return _.isEmpty(self.errors().errors()) && _.every(associationsToValidate, (association) => self[association]() ? self[association]().isValid() : true);
  };
};

Validatable.ErrorMessages = {
  duplicate(attribute) {
    return `${s.humanize(attribute)} is a duplicate`;
  },
  mustBePresent(attribute) {
    return `${s.humanize(attribute).replace(/\bxpath\b/i, 'XPath').replace(/\burl\b/i, 'URL')} must be present`;
  },
  mustBeAUrl(attribute) {
    return `${s.humanize(attribute)} must be a valid http(s) url`;
  },
  mustBePositiveNumber(attribute) {
    return `${s.humanize(attribute)} must be a positive integer`;
  },
  mustContainString(attribute, string) {
    return `${s.humanize(attribute)} must contain the string '${string}'`;
  }
};
module.exports            = Validatable;
