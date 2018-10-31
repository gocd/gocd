/*
 * Copyright 2018 ThoughtWorks, Inc.
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
const Validatable          = require('models/mixins/validatable_mixin');
const CrudMixins           = require('models/mixins/crud_mixins');

import SparkRoutes from "helpers/spark_routes";

const ArtifactStores = function (data) {
  Mixins.HasMany.call(this, {
    factory:    ArtifactStores.ArtifactStore.create,
    as:         'ArtifactStore',
    collection: data,
    uniqueOn:   'id'
  });
};

ArtifactStores.API_VERSION = 'v1';

CrudMixins.Index({
  type:     ArtifactStores,
  indexUrl: SparkRoutes.artifactStoresPath(),
  version:  ArtifactStores.API_VERSION,
  dataPath: '_embedded.artifact_stores'
});


ArtifactStores.ArtifactStore = function (data) {
  this.id         = Stream(s.defaultToIfBlank(data.id, ''));
  this.pluginId   = Stream(s.defaultToIfBlank(data.pluginId, ''));
  this.properties = s.collectionToJSON(Stream(s.defaultToIfBlank(data.properties, new PluginConfigurations())));
  this.parent     = Mixins.GetterSetter();
  this.etag       = Mixins.GetterSetter();
  Mixins.HasUUID.call(this);

  Validatable.call(this, data);

  this.validatePresenceOf('id');
  this.validatePresenceOf('pluginId');
  this.validateFormatOf('id', Validatable.DefaultOptions.forId());

  CrudMixins.AllOperations.call(this, ['refresh', 'update', 'delete', 'create'],
    {
      type:     ArtifactStores.ArtifactStore,
      indexUrl: SparkRoutes.artifactStoresPath(),
      version:  ArtifactStores.API_VERSION,
      resourceUrl(store) {
        return SparkRoutes.artifactStoresPath(store.id());
      }
    }
  );
};

ArtifactStores.ArtifactStore.get = (id) => new ArtifactStores.ArtifactStore({id}).refresh();

ArtifactStores.ArtifactStore.create = (data) => new ArtifactStores.ArtifactStore(data);

ArtifactStores.ArtifactStore.fromJSON = ({id, plugin_id, errors, properties}) => new ArtifactStores.ArtifactStore({ //eslint-disable-line camelcase
  id,
  pluginId:   plugin_id, //eslint-disable-line camelcase
  errors,
  properties: PluginConfigurations.fromJSON(properties)
});

Mixins.fromJSONCollection({
  parentType: ArtifactStores,
  childType:  ArtifactStores.ArtifactStore,
  via:        'addArtifactStore'
});


module.exports = ArtifactStores;
