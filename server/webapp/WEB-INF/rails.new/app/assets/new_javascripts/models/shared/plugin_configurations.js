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

define([
  'mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/validatable_mixin'
], function (m, _, s, Mixins, Validatable) {

  var PluginConfigurations = function (data) {
    Mixins.HasMany.call(this, {
      factory:    PluginConfigurations.Configuration.create,
      as:         'Configuration',
      collection: data
    });

    function configForKey(key) {
      return this.findConfiguration(function (config) {
        return config.key() === key;
      });
    }

    this.valueFor = function (key) {
      var config = configForKey.call(this, key);
      if (config) {
        return config.value();
      }
    };

    this.setConfiguration = function (key, value) {
      var existingConfig = configForKey.call(this, key);

      if (!existingConfig) {
        this.createConfiguration({key: key, value: value});
      } else {
        existingConfig.value(value);
      }
    };
  };

  PluginConfigurations.Configuration = function (data) {
    this.parent = Mixins.GetterSetter();

    this.key   = m.prop(s.defaultToIfBlank(data.key, ''));
    this.value = m.prop(s.defaultToIfBlank(data.value, ''));
    Validatable.call(this, data);

    this.toJSON = function () {
      return {
        key:   this.key(),
        value: this.value()
      };
    };

  };

  PluginConfigurations.Configuration.create = function (data) {
    return new PluginConfigurations.Configuration(data);
  };

  PluginConfigurations.Configuration.fromJSON = function (data) {
    return new PluginConfigurations.Configuration(data);
  };

  Mixins.fromJSONCollection({
    parentType: PluginConfigurations,
    childType:  PluginConfigurations.Configuration,
    via:        'addConfiguration'
  });

  return PluginConfigurations;
});

