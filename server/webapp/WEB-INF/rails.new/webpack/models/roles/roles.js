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
  Mixins.HasUUID.call(this);
};

Roles.API_VERSION = 'v1';

CrudMixins.Index({
  type:     Roles,
  indexUrl: Routes.apiv1AdminSecurityRolesPath(),
  version:  Roles.API_VERSION,
  dataPath: '_embedded.roles'
});


Roles.Role = function (type, data) {
  const role                 = this;
  role.constructor.modelType = 'role';

  role.name   = Stream(s.defaultToIfBlank(data.name, ''));
  role.type   = Stream(type);
  role.parent = Mixins.GetterSetter();
  role.etag   = Mixins.GetterSetter();

  Mixins.HasUUID.call(this);
  Validatable.call(this, ErrorsFromJSON(data));

  role.validatePresenceOf('name');
  role.validatePresenceOf('type');
  role.validateFormatOf('name', {
    format:  /^[a-zA-Z0-9_\-]{1}[a-zA-Z0-9_\-.]*$/,
    message: 'Invalid name. This must be alphanumeric and can contain underscores and periods (however, it cannot start ' +
             'with a period). The maximum allowed length is 255 characters.'
  });

  role.isPluginRole = function () {
    return role.type() === 'plugin';
  };

  this.toJSON = () => {
    return {
      type:       role.type(),
      name:       role.name(),
      attributes: this._attributesToJSON()
    };
  };

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
  return new Roles.Role.GoCD({name}).refresh();
};

const ErrorsFromJSON = function (data) {
  if (_.isEmpty(data.errors)) {
    return {};
  }

  return {
    errors : {
      name:         data.errors.name,
      authConfigId: data.errors.auth_config_id,
      users:        data.errors.users
    }
  };
};

Roles.Role.fromJSON = function (data = {}) {
  return Roles.Types[data.type].fromJSON(data);
};

Roles.Role.GoCD = function (data) {
  Roles.Role.call(this, "gocd", data);
  this.users = Stream(s.defaultToIfBlank(data.users, []));

  this.hasUsers = function () {
    return !_.isEmpty(this.users());
  };

  this.addUser = function (username) {
    if (_.isEmpty(username) || this.hasUser(username)) {
      return;
    }

    this.users().push(username);
    this.sortUsers();
  };

  this.deleteUser = function (username) {
    _.remove(this.users(), (user) => user === username);
  };

  this.sortUsers = function () {
    this.users(_.sortBy(this.users(), (user) => user.toLowerCase()));
  };

  this.hasUser = function (username) {
    return _.some(this.users(), (user) => username.toLowerCase() === user.toLowerCase());
  };

  this.sortUsers();

  this._attributesToJSON = function () {
    return {
      users: this.users()
    };
  };
};

Roles.Role.GoCD.fromJSON = (data = {}) => new Roles.Role.GoCD({
  name:   data.name,
  type:   data.type,
  users:  data.attributes.users,
  errors: data.errors
});

Roles.Role.Plugin = function (data) {
  Roles.Role.call(this, "plugin", data);
  this.authConfigId = Stream(s.defaultToIfBlank(data.authConfigId, ''));
  this.properties   = s.collectionToJSON(Stream(s.defaultToIfBlank(data.properties, new PluginConfigurations())));

  this._attributesToJSON = function () {
    /* eslint-disable camelcase */
    return {
      auth_config_id: this.authConfigId(),
      properties:     this.properties()
    };
    /* eslint-enable camelcase */
  };
};

Roles.Role.Plugin.fromJSON = (data = {}) => new Roles.Role.Plugin({
  name:         data.name,
  type:         data.type,
  authConfigId: data.attributes.auth_config_id,
  errors:       data.errors,
  properties:   PluginConfigurations.fromJSON(data.attributes.properties)
});

Roles.Types = {
  'gocd':   Roles.Role.GoCD,
  'plugin': Roles.Role.Plugin
};

Mixins.fromJSONCollection({
  parentType: Roles,
  childType:  Roles.Role,
  via:        'addRole'
});

module.exports = Roles;
