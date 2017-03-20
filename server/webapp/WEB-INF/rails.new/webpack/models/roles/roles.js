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
const _                    = require('lodash');

const Roles = function (data) {
  Mixins.HasMany.call(this, {
    factory:    Roles.Role.create,
    as:         'Role',
    collection: data,
    uniqueOn:   'name'
  });
};

Roles.API_VERSION = 'v1';

CrudMixins.Index({
  type:     Roles,
  indexUrl: Routes.apiv1AdminSecurityRolesPath(),
  version:  Roles.API_VERSION,
  dataPath: '_embedded.roles'
});


Roles.Role = function (data) {
  const role        = this;
  role.name         = Stream(s.defaultToIfBlank(data.name, ''));
  role.users        = Stream(s.defaultToIfBlank(data.users, undefined));
  role.authConfigId = Stream(s.defaultToIfBlank(data.authConfigId, undefined));
  role.properties   = s.collectionToJSON(Stream(s.defaultToIfBlank(data.properties, new PluginConfigurations())));
  role.parent       = Mixins.GetterSetter();
  role.etag         = Mixins.GetterSetter();

  Mixins.HasUUID.call(this);

  Validatable.call(this, data);

  role.validatePresenceOf('name');

  CrudMixins.AllOperations.call(this, ['refresh', 'update', 'delete', 'create'], {
    type:     Roles.Role,
    indexUrl: Routes.apiv1AdminSecurityRolesPath(),
    resourceUrl (role) {
      return Routes.apiv1AdminSecurityRolePath(role.name());
    },
    version:  Roles.API_VERSION
  });
};

Roles.Role.get = function (name) {
  return new Roles.Role({name}).refresh();
};

Roles.Role.create = function (data) {
  return new Roles.Role(data);
};

const ErrorsFromJSON = function (data) {
  if (_.isEmpty(data.errors)) {
    return {};
  }

  return {
    name:         data.errors.name,
    authConfigId: data.errors.auth_config_id,
    users:        data.errors.users
  };
};

Roles.Role.fromJSON = function (data) {
  return new Roles.Role({
    name:         data.name,
    authConfigId: data.auth_config_id,
    users:        data.users,
    errors:       ErrorsFromJSON(data),
    properties:   PluginConfigurations.fromJSON(data.properties)
  });
};

Mixins.fromJSONCollection({
  parentType: Roles,
  childType:  Roles.Role,
  via:        'addRole'
});

module.exports = Roles;
