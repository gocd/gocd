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

define(['models/shared/plugin_configurations'], function (PluginConfigurations) {

  describe('PluginConfigurations', function () {
    describe('new config with keys', function () {
      it('should remove configuration properties no longer supported by a plugin', function () {
        var configurations = PluginConfigurations.fromJSON([{key: 'key1', value: 'value1'}, {
          key:               'key2',
          'encrypted_value': 'value2'
        }]);
        var keys           = ['key2'];

        var config = configurations.newConfigurationWithKeys(keys);

        expect(config.collectConfigurationProperty('key')).toEqual(['key2']);
        expect(config.collectConfigurationProperty('value')).toEqual(['value2']);
      });
    });
  });
});