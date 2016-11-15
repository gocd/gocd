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
  'mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/shared/plugin_configurations', 'helpers/mrequest', 'js-routes', 'models/validatable_mixin'
], function (m, _, s, Mixins, PluginConfigurations, mrequest, Routes, Validatable) {

  var ElasticProfiles = function (data) {
    Mixins.HasMany.call(this, {
      factory:    ElasticProfiles.Profile.create,
      as:         'Profile',
      collection: data,
      uniqueOn:   'id'
    });
  };

  ElasticProfiles.Profile = function (data) {
    this.id         = m.prop(s.defaultToIfBlank(data.id, ''));
    this.pluginId   = m.prop(s.defaultToIfBlank(data.pluginId, ''));
    this.properties = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.properties, new PluginConfigurations())));
    Validatable.call(this, data);

    this.validatePresenceOf('id');
    this.validatePresenceOf('pluginId');

    this.parent = Mixins.GetterSetter();

    this.etag = Mixins.GetterSetter();

    this.update = function () {
      var profile = this;
      return m.request({
        method: 'PUT',
        url:    Routes.apiv1ElasticProfilePath(this.id()),
        config: function (xhr) {
          mrequest.xhrConfig.v1(xhr);
          xhr.setRequestHeader('If-Match', profile.etag());
        },
        data:   JSON.parse(JSON.stringify(profile, s.snakeCaser)),
      });
    };

    this.delete = function () {
      return m.request({
        method:        "DELETE",
        url:           Routes.apiv1ElasticProfilePath(this.id()),
        config:        mrequest.xhrConfig.v1,
        unwrapSuccess: function (data, xhr) {
          if (xhr.status === 200) {
            return data.message;
          }
        },
        unwrapError:   function (data, xhr) {
          if (xhr.status === 422 || xhr.status === 404 || xhr.status === 401) {
            return data.message;
          } else {
            return "There was an unknown error attempting to delete the profile, please try again in some time.";
          }
        }
      });
    };

    this.create = function () {
      return m.request({
        method: 'POST',
        url:    Routes.apiv1ElasticProfilesPath(),
        config: mrequest.xhrConfig.v1,
        data:   JSON.parse(JSON.stringify(this, s.snakeCaser)),
      });
    };
  };

  ElasticProfiles.Profile.get = function (id) {
    return m.request({
      method:        'GET',
      url:           Routes.apiv1ElasticProfilePath(id),
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: function (data, xhr) {
        var profile = ElasticProfiles.Profile.fromJSON(data);
        profile.etag(xhr.getResponseHeader('ETag'));
        return profile;
      },
    });
  };

  ElasticProfiles.Profile.create = function (data) {
    return new ElasticProfiles.Profile(data);
  };

  ElasticProfiles.Profile.fromJSON = function (data) {
    return new ElasticProfiles.Profile({
      id:         data.id,
      pluginId:   data.plugin_id,
      errors:     data.errors,
      properties: PluginConfigurations.fromJSON(data.properties)
    });
  };

  ElasticProfiles.all = function () {
    return m.request({
      method:        "GET",
      url:           Routes.apiv1ElasticProfilesPath(),
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: function (data) {
        return ElasticProfiles.fromJSON(data['_embedded']['profiles']);
      }
    });
  };

  Mixins.fromJSONCollection({
    parentType: ElasticProfiles,
    childType:  ElasticProfiles.Profile,
    via:        'addProfile'
  });

  return ElasticProfiles;
});