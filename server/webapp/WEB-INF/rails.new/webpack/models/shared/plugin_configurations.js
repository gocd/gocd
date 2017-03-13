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

const Stream         = require('mithril/stream');
const s              = require('string-plus');
const Mixins         = require('models/mixins/model_mixins');
const Validatable    = require('models/mixins/validatable_mixin');
const EncryptedValue = require('models/pipeline_configs/encrypted_value');

const plainOrCipherValue = data => {
  if (data.encrypted_value) {
    return new EncryptedValue({cipherText: s.defaultToIfBlank(data.encrypted_value, '')});
  } else {
    return new EncryptedValue({clearText: s.defaultToIfBlank(data.value, '')});
  }
};

const PluginConfigurations = function (data) {
  this.constructor.modelType = 'plugin-configurations';

  Mixins.HasMany.call(this, {
    factory:    PluginConfigurations.Configuration.create,
    as:         'Configuration',
    collection: data
  });

  function configForKey(key) {
    return this.findConfiguration(config => config.key() === key);
  }

  this.valueFor = function (key) {
    const config = configForKey.call(this, key);
    if (config) {
      return config.value();
    }
  };

  this.setConfiguration = function (key, value) {
    const existingConfig = configForKey.call(this, key);

    if (!existingConfig) {
      this.createConfiguration({key, value});
    } else {
      existingConfig.value(value);
    }
  };
};

PluginConfigurations.Configuration = function (data) {
  this.parent                = Mixins.GetterSetter();
  this.constructor.modelType = 'plugin-configuration';

  this.key   = Stream(s.defaultToIfBlank(data.key, ''));
  const _value = Stream(plainOrCipherValue(data));

  Mixins.HasEncryptedAttribute.call(this, {attribute: _value, name: 'value'});

  Validatable.call(this, data);

  this.toJSON = function () {
    if (this.isPlainValue()) {
      return {
        key:   this.key(),
        value: this.value()
      };
    } else {
      return {
        key:               this.key(),
        'encrypted_value': this.value()
      };
    }
  };

};

PluginConfigurations.Configuration.create = data => new PluginConfigurations.Configuration(data);

PluginConfigurations.Configuration.fromJSON = data => new PluginConfigurations.Configuration(data);

Mixins.fromJSONCollection({
  parentType: PluginConfigurations,
  childType:  PluginConfigurations.Configuration,
  via:        'addConfiguration'
});

module.exports = PluginConfigurations;

