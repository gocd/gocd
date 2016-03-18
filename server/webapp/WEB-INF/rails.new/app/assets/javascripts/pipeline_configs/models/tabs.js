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

define(['mithril', 'lodash', 'string-plus', './model_mixins', './errors'], function (m, _, s, Mixins, Errors) {
  var Tabs = function (data) {
    Mixins.HasMany.call(this, {factory: Tabs.Tab.create, as: 'Tab', collection: data, uniqueOn: 'name'});
  };

  Tabs.Tab = function (data) {
    this.constructor.modelType = 'tab';
    Mixins.HasUUID.call(this);

    this.parent = Mixins.GetterSetter();

    this.name = m.prop(s.defaultToIfBlank(data.name, ''));
    this.path = m.prop(s.defaultToIfBlank(data.path, ''));
    this.errors      = m.prop(s.defaultToIfBlank(data.errors, new Errors()));

    this.isBlank = function () {
      return s.isBlank(this.name()) && s.isBlank(this.path());
    };

    this.validate = function () {
      var errors = new Errors();

      if (this.isBlank()) {
        return errors;
      }

      if (s.isBlank(this.name())) {
        if (!s.isBlank(this.path())) {
          errors.add('name', Mixins.ErrorMessages.mustBePresent('name'));
        }
      } else {
        this.parent().validateUniqueTabName(this, errors);
      }

      return errors;
    };
  };

  Tabs.Tab.create = function (data) {
    return new Tabs.Tab(data);
  };

  Mixins.fromJSONCollection({
    parentType: Tabs,
    childType:  Tabs.Tab,
    via:        'addTab'
  });

  Tabs.Tab.fromJSON = function (data) {
    return new Tabs.Tab({
      name:   data.name,
      path:   data.path,
      errors: Errors.fromJson(data)
    });
  };

  return Tabs;
});
