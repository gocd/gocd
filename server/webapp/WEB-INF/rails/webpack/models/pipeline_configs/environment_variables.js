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

const Stream         = require('mithril/stream');
const s              = require('string-plus');
const Mixins         = require('models/mixins/model_mixins');
const EncryptedValue = require('models/pipeline_configs/encrypted_value');
const Validatable    = require('models/mixins/validatable_mixin');

const EnvironmentVariables = function (data) {
  Mixins.HasMany.call(this, {
    factory:    EnvironmentVariables.Variable.create,
    as:         'Variable',
    collection: data,
    uniqueOn:   'name'
  });

  this.secureVariables = function () {
    return this.filterVariable((variable) => variable.isSecureValue());
  };

  this.plainVariables = function () {
    return this.filterVariable((variable) => !variable.isSecureValue());
  };
};

function plainOrCipherValue({secure, encryptedValue, value}) {
  if (secure) {
    return new EncryptedValue({cipherText: s.defaultToIfBlank(encryptedValue, '')});
  } else {
    return new EncryptedValue({clearText: s.defaultToIfBlank(value, '')});
  }
}

EnvironmentVariables.Variable = function (data) {
  this.constructor.modelType = 'environmentVariable';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.parent = Mixins.GetterSetter();

  this.name  = Stream(s.defaultToIfBlank(data.name, ''));
  const _value = Stream(plainOrCipherValue(data));
  Mixins.HasEncryptedAttribute.call(this, {attribute: _value, name: 'value'});

  this.toJSON = function () {
    if (this.isPlainValue()) {
      return {
        name:   this.name(),
        secure: false,
        value:  this.value()
      };
    } else {
      if (this.isDirtyValue()) {
        return {
          name:   this.name(),
          secure: true,
          value:  this.value()
        };
      } else {
        return {
          name:           this.name(),
          secure:         true,
          encryptedValue: this.value()
        };
      }
    }
  };

  this.isBlank = function () {
    return s.isBlank(this.name()) && s.isBlank(this.value());
  };

  this.validatePresenceOf('name', {
    condition(property) {
      return (!s.isBlank(property.value()));
    }
  });
  this.validateUniquenessOf('name');
};

EnvironmentVariables.Variable.create = (data) => new EnvironmentVariables.Variable(data);

Mixins.fromJSONCollection({
  parentType: EnvironmentVariables,
  childType:  EnvironmentVariables.Variable,
  via:        'addVariable'
});

EnvironmentVariables.Variable.fromJSON = ({name, value, secure, encrypted_value}) => new EnvironmentVariables.Variable({ //eslint-disable-line camelcase
  name,
  value,
  secure,
  encryptedValue: encrypted_value //eslint-disable-line camelcase
});

module.exports = EnvironmentVariables;
