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
  'mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/shared/plugin_configurations', 'helpers/mrequest', 'js-routes',
], function (m, _, s, Mixins, PluginConfigurations, mrequest, Routes) {
  var ElasticProfiles = function (data) {
    Mixins.HasMany.call(this, {
      factory:    ElasticProfiles.Profile.create,
      as:         'Profile',
      collection: data,
      uniqueOn:   'id'
    });
  };

  ElasticProfiles.Profile = function (data) {
    this.id         = m.prop(data.id);
    this.pluginId   = m.prop(data.pluginId);
    this.properties = s.collectionToJSON(m.prop(data.properties));

    this.parent = Mixins.GetterSetter();

    this.update = function () {
      return m.request({
        method: 'PUT',
        url:    Routes.apiv1ElasticProfilePath(this.id()),
        config: mrequest.xhrConfig.v1,
        data:   JSON.parse(JSON.stringify(this, s.snakeCaser)),
      });
    };

    this.delete = function () {
      return m.request({
        method: "DELETE",
        url:    Routes.apiv1ElasticProfilePath(this.id()),
        config: mrequest.xhrConfig.v1,
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

  ElasticProfiles.Profile.find   = function (id) {
    return m.request({
      method:        'GET',
      url:           Routes.apiv1ElasticProfilePath(id),
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: function (data) {
        return ElasticProfiles.Profile.fromJSON(data);
      }
    });
  };
  ElasticProfiles.Profile.create = function (data) {
    return new ElasticProfiles.Profile(data);
  };

  ElasticProfiles.Profile.fromJSON = function (data) {
    return new ElasticProfiles.Profile({
      id:         data.id,
      pluginId:   data.plugin_id,
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