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

  var EnvironmentVariables = function (data) {
    Mixins.HasMany.call(this, {
      factory:    EnvironmentVariables.Variable.create,
      as:         'Variable',
      collection: data,
      uniqueOn:   'name'
    });
  };

  EnvironmentVariables.Variable = function (data) {
    this.constructor.modelType = 'environmentVariable';
    Mixins.HasUUID.call(this);

    this.parent = Mixins.GetterSetter();

    this.name   = m.prop(s.defaultToIfBlank(data.name, ''));
    this.value  = m.prop(s.defaultToIfBlank(data.value, ''));
    this.secure = m.prop(data.secure);

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
        this.parent().validateUniqueVariableName(this, errors);
      }

      return errors;
    };
  };

  EnvironmentVariables.Variable.create = function (data) {
    return new EnvironmentVariables.Variable(data);
  };

  Mixins.fromJSONCollection({
    parentType: EnvironmentVariables,
    childType:  EnvironmentVariables.Variable,
    via:        'addVariable'
  });

  EnvironmentVariables.Variable.fromJSON = function (data) {
    return new EnvironmentVariables.Variable(_.pick(data, ['name', 'value', 'secure']));
  };

  return EnvironmentVariables;

});
