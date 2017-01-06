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
  'mithril', 'string-plus', 'models/model_mixins', 'models/shared/plugin_configurations', 'helpers/mrequest', 'js-routes', 'models/validatable_mixin'
], function (m, s, Mixins, PluginConfigurations, mrequest, Routes, Validatable) {

  var unwrapMessageOrEntity = function (originalEtag) {
    return function (data, xhr) {
      if (xhr.status === 422) {
        var fromJSON = new Roles.Role.fromJSON(data.data);
        fromJSON.etag(originalEtag);
        return fromJSON;
      } else {
        return mrequest.unwrapErrorExtractMessage(data, xhr);
      }
    };
  };

  var Roles = function (data) {
    Mixins.HasMany.call(this, {
      factory:    Roles.Role.create,
      as:         'Role',
      collection: data,
      uniqueOn:   'name'
    });
  };

  Roles.all = function () {
    return m.request({
      method:        "GET",
      url:           Routes.apiv1AdminSecurityRolesPath(),
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: function (data) {
        return Roles.fromJSON(data['_embedded']['roles']);
      },
      unwrapError:   mrequest.unwrapErrorExtractMessage
    });
  };

  Roles.Role = function (data) {
    this.name         = m.prop(s.defaultToIfBlank(data.name, ''));
    this.authConfigId   = m.prop(s.defaultToIfBlank(data.authConfigId, ''));
    this.properties = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.properties, new PluginConfigurations())));
    this.parent     = Mixins.GetterSetter();
    this.etag       = Mixins.GetterSetter();
    Mixins.HasUUID.call(this);

    Validatable.call(this, data);

    this.validatePresenceOf('name');
    this.validatePresenceOf('authConfigId');

    this.update = function () {
      var self = this;
      return m.request({
        method:      'PUT',
        url:         Routes.apiv1AdminSecurityRolePath(this.name()),
        config:      function (xhr) {
          mrequest.xhrConfig.v1(xhr);
          xhr.setRequestHeader('If-Match', self.etag());
        },
        data:        JSON.parse(JSON.stringify(self, s.snakeCaser)),
        unwrapError: unwrapMessageOrEntity(self.etag())
      });
    };

    this.delete = function () {
      return m.request({
        method:        "DELETE",
        url:           Routes.apiv1AdminSecurityRolePath(this.name()),
        config:        mrequest.xhrConfig.v1,
        unwrapSuccess: function (data, xhr) {
          if (xhr.status === 200) {
            return data.message;
          }
        },
        unwrapError:   mrequest.unwrapErrorExtractMessage
      });
    };

    this.create = function () {
      return m.request({
        method:      'POST',
        url:         Routes.apiv1AdminSecurityRolesPath(),
        config:      mrequest.xhrConfig.v1,
        data:        JSON.parse(JSON.stringify(this, s.snakeCaser)),
        unwrapError: unwrapMessageOrEntity()
      });
    };
  };

  Roles.Role.get = function (name) {
    return m.request({
      method:        'GET',
      url:           Routes.apiv1AdminSecurityRolePath(name),
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: function (data, xhr) {
        var entity = Roles.Role.fromJSON(data);
        entity.etag(xhr.getResponseHeader('ETag'));
        return entity;
      },
      unwrapError:   mrequest.unwrapErrorExtractMessage
    });
  };

  Roles.Role.create = function (data) {
    return new Roles.Role(data);
  };

  Roles.Role.fromJSON = function (data) {
    return new Roles.Role({
      name:         data.name,
      authConfigId:   data.auth_config_id,
      errors:     data.errors,
      properties: PluginConfigurations.fromJSON(data.properties)
    });
  };

  Mixins.fromJSONCollection({
    parentType: Roles,
    childType:  Roles.Role,
    via:        'addRole'
  });

  return Roles;
});