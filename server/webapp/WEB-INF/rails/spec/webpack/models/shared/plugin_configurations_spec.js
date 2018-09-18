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

describe('Plugin Configuration', () => {

  const s = require('string-plus');

  const PluginConfigurations = require('models/shared/plugin_configurations');

  const plainTextConfigJSON = () => ({
    "key":   "Url",
    "value": "ldap://your.ldap.server.url:port"
  });

  const encryptedConfigJSON = () => ({
    "key":             "Password",
    "encrypted_value": "secret"
  });

  const pluginConfigurationJSON = () => ([plainTextConfigJSON(), encryptedConfigJSON()]);

  it('should deserialize a plugin configuration from JSON', () => {
    const pluginConfigurations = new PluginConfigurations(pluginConfigurationJSON());

    expect(pluginConfigurations.countConfiguration()).toEqual(2);
    expect(pluginConfigurations.firstConfiguration().key).toEqual('Url');
    expect(pluginConfigurations.firstConfiguration().value).toEqual('ldap://your.ldap.server.url:port');

    expect(pluginConfigurations.lastConfiguration().key).toEqual('Password');
    expect(pluginConfigurations.lastConfiguration().encrypted_value).toEqual('secret');
  });

  it('should deserialize a unsecure plugin configuration from JSON', () => {
    const pluginConfigurations = PluginConfigurations.Configuration.fromJSON(plainTextConfigJSON());

    expect(pluginConfigurations.key()).toEqual('Url');
    expect(pluginConfigurations.isDirtyValue()).toEqual(false);
    expect(pluginConfigurations.isEditingValue()).toEqual(true);
    expect(pluginConfigurations.isPlainValue()).toEqual(true);
    expect(pluginConfigurations.isSecureValue()).toEqual(false);
    expect(pluginConfigurations.displayValue()).toEqual("ldap://your.ldap.server.url:port");
    expect(pluginConfigurations.value()).toEqual("ldap://your.ldap.server.url:port");
  });

  it('should deserialize a secure plugin configuration from JSON', () => {
    const pluginConfigurations = PluginConfigurations.Configuration.fromJSON(encryptedConfigJSON());

    expect(pluginConfigurations.key()).toEqual('Password');
    expect(pluginConfigurations.isDirtyValue()).toEqual(false);
    expect(pluginConfigurations.isEditingValue()).toEqual(false);
    expect(pluginConfigurations.isPlainValue()).toEqual(false);
    expect(pluginConfigurations.isSecureValue()).toEqual(true);
    expect(pluginConfigurations.displayValue()).toEqual("******");
    expect(pluginConfigurations.value()).toEqual("secret");
  });

  it('should serialize a plugin configuration to JSON', () => {
    const pluginConfigurations = PluginConfigurations.Configuration.fromJSON(plainTextConfigJSON());
    expect(JSON.parse(JSON.stringify(pluginConfigurations, s.snakeCaser))).toEqual(plainTextConfigJSON());
  });

  it('should able to update secure value', () => {
    const pluginConfigurations = new PluginConfigurations([]);
    pluginConfigurations.addConfiguration(PluginConfigurations.Configuration.fromJSON(encryptedConfigJSON()));
    const passwordConfig = pluginConfigurations.configForKey("Password");

    expect(passwordConfig.value()).toEqual("secret");
    expect(passwordConfig.isDirtyValue()).toEqual(false);
    expect(passwordConfig.isSecureValue()).toEqual(true);
    expect(passwordConfig.toJSON()).toEqual({key: 'Password', "encrypted_value": 'secret'});

    pluginConfigurations.setConfiguration("Password", "changed");

    expect(passwordConfig.value()).toEqual("changed");
    expect(passwordConfig.isDirtyValue()).toEqual(true);
    expect(passwordConfig.isSecureValue()).toEqual(true);
    expect(passwordConfig.toJSON()).toEqual({key: 'Password', value: 'changed'});
  });

});
