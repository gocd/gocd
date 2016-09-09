/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['lodash', 'string-plus', 'mithril', 'models/errors', 'models/model_mixins'], function (_, s, m, Errors, Mixins) {
  var PresenceValidator = function (options) {
    this.validate = function (entity, attr) {
      if (options.condition && (!options.condition(entity))) {
        return;
      }

      if (s.isBlank(entity[attr]())) {
        entity.errors().add(attr, Validatable.ErrorMessages.mustBePresent(attr));
      }
    };
  };

  var UniquenessValidator = function () {
    this.validate = function (entity, attr) {
      if (_.isNil(entity.parent()) || s.isBlank(entity[attr]())) {
        return;
      }

      if (!entity.parent().isUnique(entity, attr)) {
        entity.errors().add(attr, Validatable.ErrorMessages.duplicate(attr));
      }
    };
  };

  var UrlPatternValidator = function () {
    var URL_REGEX = /^http(s)?:\/\/.+/;

    this.validate = function (entity, attr) {
      if (s.isBlank(entity[attr]())) {
        return;
      }

      if (!entity[attr]().match(URL_REGEX)) {
        entity.errors().add(attr, Validatable.ErrorMessages.mustBeAUrl(attr));
      }
    };
  };

  var FormatValidator = function (options) {
    this.validate = function (entity, attr) {
      if (s.isBlank(entity[attr]())) {
        return;
      }

      if (!entity[attr]().match(options.format)) {
        entity.errors().add(attr, options.message || (s.humanize(attr) + ' format is in valid'));
      }
    };
  };

  var Validatable = function (data) {
    var self                   = this;
    var attrToValidators       = {};
    var associationsToValidate = [];
    self.errors                = Mixins.GetterSetter(new Errors(data.errors));

    var validateWith = function (validator, attr) {
      _.has(attrToValidators, attr) ? attrToValidators[attr].push(validator) : attrToValidators[attr] = [validator];
    };

    var clearErrors = function (attr) {
      attr ? self.errors().clear(attr) : self.errors().clear();
    };

    self.validatePresenceOf = function (attr, options) {
      validateWith(new PresenceValidator(options || {}), attr);
    };

    self.validateUniquenessOf = function (attr, options) {
      validateWith(new UniquenessValidator(options || {}), attr);
    };

    self.validateFormatOf = function (attr, options) {
      validateWith(new FormatValidator(options || {}), attr);
    };

    self.validateUrlPattern = function (attr, options) {
      validateWith(new UrlPatternValidator(options || {}), attr);
    };

    self.validateWith = function (attr, validator) {
      validateWith(new validator(), attr);
    };

    self.validateAssociated = function (association) {
      associationsToValidate.push(association);
    };

    self.validate = function (attr) {
      var attrs = attr ? [attr] : _.keys(attrToValidators);

      clearErrors(attr);

      _.forEach(attrs, function (attr) {
        _.forEach(attrToValidators[attr], function (validator) {
          validator.validate(self, attr);
        });
      });

      return self.errors();
    };

    self.isValid = function () {
      self.validate();

      return _.isEmpty(self.errors().errors()) && _.every(associationsToValidate, function (association) {
        return self[association]() ? self[association]().isValid() : true;
      });
    };
  };

  Validatable.ErrorMessages = {
    duplicate:            function (attribute) {
      return s.humanize(attribute) + " is a duplicate";
    },
    mustBePresent:        function (attribute) {
      return s.humanize(attribute).replace(/\bxpath\b/i, 'XPath').replace(/\burl\b/i, 'URL') + " must be present";
    },
    mustBeAUrl:           function (attribute) {
      return s.humanize(attribute) + " must be a valid http(s) url";
    },
    mustBePositiveNumber: function (attribute) {
      return s.humanize(attribute) + " must be a positive integer";
    },
    mustContainString:    function (attribute, string) {
      return s.humanize(attribute) + " must contain the string '" + string + "'";
    }
  };
  return Validatable;
});
