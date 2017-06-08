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

const ElasticProfiles = function (data) {
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
  this.validateFormatOf('id', {
    format:  /^[a-zA-Z0-9_\-]{1}[a-zA-Z0-9_\-.]*$/,
    message: 'Invalid id. This must be alphanumeric and can contain underscores and periods (however, it cannot start ' +
             'with a period). The maximum allowed length is 255 characters.'
  });

  CrudMixins.AllOperations.call(this, ['refresh', 'update', 'delete', 'create'],
    {
      type:     ElasticProfiles.Profile,
      indexUrl: Routes.apiv1ElasticProfilesPath(),
      version:  ElasticProfiles.API_VERSION,
      resourceUrl(profile) {
        return Routes.apiv1ElasticProfilePath(profile.id());
      }
    }
  );
};

ElasticProfiles.Profile.get = (id) => new ElasticProfiles.Profile({id}).refresh();

ElasticProfiles.Profile.create = (data) => new ElasticProfiles.Profile(data);

ElasticProfiles.Profile.fromJSON = ({id, plugin_id, errors, properties}) => new ElasticProfiles.Profile({ //eslint-disable-line camelcase
  id,
  pluginId:   plugin_id, //eslint-disable-line camelcase
  errors,
  properties: PluginConfigurations.fromJSON(properties)
});

Mixins.fromJSONCollection({
  parentType: ElasticProfiles,
  childType:  ElasticProfiles.Profile,
  via:        'addProfile'
});


module.exports = ElasticProfiles;
