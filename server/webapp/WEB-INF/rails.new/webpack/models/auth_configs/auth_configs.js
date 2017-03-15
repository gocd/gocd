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

const Stream               = require('mithril/stream');
const s                    = require('string-plus');
const Mixins               = require('models/mixins/model_mixins');
const PluginConfigurations = require('models/shared/plugin_configurations');
const Routes               = require('gen/js-routes');
const Validatable          = require('models/mixins/validatable_mixin');
const CrudMixins           = require('models/mixins/crud_mixins');

const AuthConfigs = function (data) {
  Mixins.HasMany.call(this, {
    factory:    AuthConfigs.AuthConfig.create,
    as:         'AuthConfig',
    collection: data,
    uniqueOn:   'id'
  });

  this.findById = function (authConfigId) {
    return this.findAuthConfig((authConfig) => authConfig.id() === authConfigId);
  };

};

AuthConfigs.API_VERSION = 'v1';

CrudMixins.Index({
  type:     AuthConfigs,
  indexUrl: Routes.apiv1AdminSecurityAuthConfigsPath(),
  version:  AuthConfigs.API_VERSION,
  dataPath: '_embedded.auth_configs'
});

AuthConfigs.AuthConfig = function (data) {
  this.id         = Stream(s.defaultToIfBlank(data.id, ''));
  this.pluginId   = Stream(s.defaultToIfBlank(data.pluginId, ''));
  this.properties = s.collectionToJSON(Stream(s.defaultToIfBlank(data.properties, new PluginConfigurations())));
  this.parent     = Mixins.GetterSetter();
  this.etag       = Mixins.GetterSetter();
  Mixins.HasUUID.call(this);

  Validatable.call(this, data);

  this.validatePresenceOf('id');
  this.validatePresenceOf('pluginId');

  CrudMixins.AllOperations.call(this, ['refresh', 'update', 'delete', 'create'], {
    type:     AuthConfigs.AuthConfig,
    indexUrl: Routes.apiv1AdminSecurityAuthConfigsPath(),
    resourceUrl (id) {
      return Routes.apiv1AdminSecurityAuthConfigPath(id);
    },
    version:  AuthConfigs.API_VERSION
  });
};

AuthConfigs.AuthConfig.get = function (id) {
  return new AuthConfigs.AuthConfig({id}).refresh();
};

AuthConfigs.AuthConfig.create = function (data) {
  return new AuthConfigs.AuthConfig(data);
};

AuthConfigs.AuthConfig.fromJSON = function (data) {
  return new AuthConfigs.AuthConfig({
    id:         data.id,
    pluginId:   data.plugin_id,
    errors:     data.errors,
    properties: PluginConfigurations.fromJSON(data.properties)
  });
};

Mixins.fromJSONCollection({
  parentType: AuthConfigs,
  childType:  AuthConfigs.AuthConfig,
  via:        'addAuthConfig'
});

module.exports = AuthConfigs;
