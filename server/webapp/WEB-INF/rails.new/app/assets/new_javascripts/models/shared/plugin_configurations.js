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
  'mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/validatable_mixin', 'models/pipeline_configs/encrypted_value'
], function (m, _, s, Mixins, Validatable, EncryptedValue) {

  function plainOrCipherValue(data) {
    if (data.encrypted_value) {
      return new EncryptedValue({cipherText: s.defaultToIfBlank(data.encrypted_value, '')});
    } else {
      return new EncryptedValue({clearText: s.defaultToIfBlank(data.value, '')});
    }
  }

  var PluginConfigurations = function (data) {
    this.constructor.modelType = 'plugin-configurations';

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
    Validatable.call(this, data);
    this.parent = Mixins.GetterSetter();
    this.key    = m.prop(s.defaultToIfBlank(data.key, ''));
    var _value  = m.prop(plainOrCipherValue(data));

    Mixins.HasEncryptedAttribute.call(this, {attribute: _value, name: 'value'});

    this.toJSON = function () {
      var valueHash = this.isPlainValue() ? {value: this.value()} : {encrypted_value: this.value()};  //eslint-disable-line camelcase

      return _.merge({key: this.key()}, valueHash);
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
