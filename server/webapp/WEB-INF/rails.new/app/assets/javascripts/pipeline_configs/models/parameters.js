/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'string-plus', './model_mixins'], function (m, _, s, Mixins) {

  var Parameters = function (data) {
    Mixins.HasMany.call(this, {
      factory:    Parameters.Parameter.create,
      as:         'Parameter',
      collection: data,
      uniqueOn:   'name'
    });
  };

  Parameters.Parameter = function (data) {
    this.constructor.modelType = 'parameter';
    Mixins.HasUUID.call(this);

    this.parent = Mixins.GetterSetter();

    this.name  = m.prop(s.defaultToIfBlank(data.name, ''));
    this.value = m.prop(s.defaultToIfBlank(data.value, ''));

    this.isBlank = function () {
      return s.isBlank(this.name()) && s.isBlank(this.value());
    };

    this.validate = function () {
      var errors = new Mixins.Errors();

      if (this.isBlank()) {
        return errors;
      }

      if (s.isBlank(this.name())) {
        if (!s.isBlank(this.value())) {
          errors.add('name', Mixins.ErrorMessages.mustBePresent('name'));
        }
      } else {
        this.parent().validateUniqueParameterName(this, errors);
      }

      return errors;
    };
  };

  Parameters.Parameter.create = function (data) {
    return new Parameters.Parameter(data);
  };

  Mixins.fromJSONCollection({
    parentType: Parameters,
    childType:  Parameters.Parameter,
    via:        'addParameter'
  });

  Parameters.Parameter.fromJSON = function (data) {
    return new Parameters.Parameter(_.pick(data, ['name', 'value']));
  };

  return Parameters;
});
