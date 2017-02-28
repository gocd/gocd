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

var Stream               = require('mithril/stream');
var s                    = require('string-plus');
var Mixins               = require('models/mixins/model_mixins');
var PluginConfigurations = require('models/shared/plugin_configurations');
var Routes               = require('gen/js-routes');
var Validatable          = require('models/mixins/validatable_mixin');
var CrudMixins           = require('models/mixins/crud_mixins');

var ElasticProfiles = function (data) {
  Mixins.HasMany.call(this, {
    factory:    ElasticProfiles.Profile.create,
    as:         'Profile',
    collection: data,
    uniqueOn:   'id'
  });
};

ElasticProfiles.API_VERSION = 'v1';

CrudMixins.Index({
  type:     ElasticProfiles,
  indexUrl: Routes.apiv1ElasticProfilesPath(),
  version:  ElasticProfiles.API_VERSION,
  dataPath: '_embedded.profiles'
});


ElasticProfiles.Profile = function (data) {
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
    type:        ElasticProfiles.Profile,
    indexUrl:    Routes.apiv1ElasticProfilesPath(),
    resourceUrl: function (id) {
      return Routes.apiv1ElasticProfilePath(id);
    },
    version:     ElasticProfiles.API_VERSION
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


module.exports = ElasticProfiles;