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
  'mithril', 'string-plus', 'models/model_mixins', 'models/shared/plugin_configurations', 'js-routes', 'models/validatable_mixin', 'models/crud_mixins'
], function (m, s, Mixins, PluginConfigurations, Routes, Validatable, CrudMixins) {

  var ElasticProfiles = function (data) {
    Mixins.HasMany.call(this, {
      factory:    ElasticProfiles.Profile.create,
      as:         'Profile',
      collection: data,
      uniqueOn:   'id'
    });
  };

  CrudMixins.Index({
    type:     ElasticProfiles,
    indexUrl: Routes.apiv1ElasticProfilesPath(),
    version:  'v1',
    dataPath: '_embedded.profiles'
  });

  ElasticProfiles.Profile = function (data) {
    this.id         = m.prop(s.defaultToIfBlank(data.id, ''));
    this.pluginId   = m.prop(s.defaultToIfBlank(data.pluginId, ''));
    this.properties = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.properties, new PluginConfigurations())));
    this.parent     = Mixins.GetterSetter();
    this.etag       = Mixins.GetterSetter();
    Mixins.HasUUID.call(this);

    Validatable.call(this, data);

    this.validatePresenceOf('id');
    this.validatePresenceOf('pluginId');

    CrudMixins.AllOperations.call(this, ['refresh', 'update', 'delete', 'create'], {
      type:        ElasticProfiles.Profile,
      indexUrl:    Routes.apiv1ElasticProfilesPath(),
      resourceUrl: function (id) {
        return Routes.apiv1ElasticProfilePath(id);
      },
      version:     'v1'
    });
  };

  ElasticProfiles.Profile.get = function (id) {
    return new ElasticProfiles.Profile({id: id}).refresh();
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

  Mixins.fromJSONCollection({
    parentType: ElasticProfiles,
    childType:  ElasticProfiles.Profile,
    via:        'addProfile'
  });


  return ElasticProfiles;
});