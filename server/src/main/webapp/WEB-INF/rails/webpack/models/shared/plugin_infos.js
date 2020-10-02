/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import _ from "lodash";
import {SparkRoutes} from "helpers/spark_routes";
import Stream from "mithril/stream";
import {Mixins} from "models/mixins/model_mixins";
import {CrudMixins} from "models/mixins/crud_mixins";
import {PluggableInstanceSettings} from "models/shared/plugin_infos/pluggable_instance_settings";
import {Capabilities as AuthorizationPluginCapabilities} from "models/shared/plugin_infos/authorization_plugin_capabilities";
import {Capabilities as ElasticPluginCapabilities} from "models/shared/plugin_infos/elastic_plugin_capabilities";
import {Capabilities as AnalyticsPluginCapabilities} from "models/shared/plugin_infos/analytics_plugin_capabilities";
import {About} from "models/shared/plugin_infos/about";

export const PluginInfos = function (data) {
  Mixins.HasMany.call(this, {
    factory:    PluginInfos.PluginInfo.createByType,
    as:         'PluginInfo',
    collection: data,
    uniqueOn:   'id'
  });

  this.findById = function (id) {
    return this.findPluginInfo((pi) => pi.id() === id);
  };

  this.filterByType = function (type) {
    return new PluginInfos(this.filterPluginInfo((pi) => _.includes(pi.types(), type)));
  };

  this.pluggableTasksTypes = function () {
    const pluggableTasks = {};
    this.filterByType('task').eachPluginInfo((pluginInfo) => {
      pluggableTasks[pluginInfo.id()] = {description: pluginInfo.id()};
    });

    return pluggableTasks;
  };

};

PluginInfos.API_VERSION = 'v6';

CrudMixins.Index({
  type:     PluginInfos,
  indexUrl: SparkRoutes.apiPluginInfoPath({}),
  version:  PluginInfos.API_VERSION,
  dataPath: '_embedded.plugin_info'
});

PluginInfos.PluginInfo = function (types, {about, bundledPlugin, extensions, id, imageUrl, pluginFileLocation, pluginSettings, status}) {
  this.constructor.modelType = 'pluginInfo';
  this.parent                = Mixins.GetterSetter();
  Mixins.HasUUID.call(this);

  this.types = Stream(types);

  this.id       = Stream(id);
  this.about    = Stream(about);
  this.imageUrl = Stream(imageUrl);

  if (pluginSettings) {
    this.pluginSettings = Stream(pluginSettings);
  }

  this.status             = Stream(status);
  this.pluginFileLocation = Stream(pluginFileLocation);
  this.bundledPlugin      = Stream(bundledPlugin);
  this.extensions         = Stream(extensions);

  this.isActive = () => this.status().state() === 'active';

  this.isBundled = () => this.bundledPlugin() === true;

  this.hasErrors = () => this.status().state() === 'invalid';

  this.supportsPluginSettings = function () {
    return !!this.pluginSettings && this.pluginSettings().hasView() && this.pluginSettings().hasConfigurations();
  };
};

PluginInfos.PluginInfo.MultiPluginInfo = {};

PluginInfos.PluginInfo.MultiPluginInfo.fromJSON = (data = {}) => {
  const extensions = {};
  _.forEach(data.extensions, (extension) => {
    extensions[extension.type] = PluginInfos.PluginInfo.Extensions[extension.type](extension);
  });

  const extensionWithPluginSettings = _.find(data.extensions, 'plugin_settings');

  return new PluginInfos.PluginInfo(_.map(data.extensions, (extension) => extension.type), {
    about:              About.fromJSON(data.about),
    bundledPlugin:      data.bundled_plugin,
    extensions,
    id:                 data.id,
    imageUrl:           _.get(data, '_links.image.href'),
    pluginFileLocation: data.plugin_file_location,
    pluginSettings:     _.isUndefined(extensionWithPluginSettings) ? null : PluggableInstanceSettings.fromJSON(extensionWithPluginSettings.plugin_settings),
    status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),
  });
};

PluginInfos.PluginInfo.Bad = function (data) {
  PluginInfos.PluginInfo.call(this, null, data);
};

PluginInfos.PluginInfo.Bad.fromJSON = function (data) {
  return new PluginInfos.PluginInfo.Bad({
    about:              About.fromJSON(data.about),
    bundledPlugin:      data.bundled_plugin,
    extensions:         null,
    id:                 data.id,
    imageUrl:           null,
    pluginFileLocation: data.plugin_file_location,
    pluginSettings:     null,
    status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),
  });
};

PluginInfos.PluginInfo.Extensions = {};

PluginInfos.PluginInfo.Extensions['analytics'] = (extensionData = {}) => {
  return {
    capabilities: Stream(AnalyticsPluginCapabilities.fromJSON(extensionData.capabilities.supported_analytics)),
  };
};

PluginInfos.PluginInfo.Extensions['artifact'] = (extensionData = {}) => {
  return {
    artifactConfigSettings: Stream(PluggableInstanceSettings.fromJSON(extensionData.artifact_config_settings)),
    fetchArtifactSettings:  Stream(PluggableInstanceSettings.fromJSON(extensionData.fetch_artifact_settings)),
    storeConfigSettings:    Stream(PluggableInstanceSettings.fromJSON(extensionData.store_config_settings)),
  };
};

PluginInfos.PluginInfo.Extensions['authorization'] = (extensionData = {}) => {
  return {
    authConfigSettings: Stream(PluggableInstanceSettings.fromJSON(extensionData.auth_config_settings)),
    capabilities:       Stream(AuthorizationPluginCapabilities.fromJSON(extensionData.capabilities)),
    roleSettings:       Stream(PluggableInstanceSettings.fromJSON(extensionData.role_settings)),
  };
};

PluginInfos.PluginInfo.Extensions['configrepo'] = () => {
  return {};
};

PluginInfos.PluginInfo.Extensions['elastic-agent'] = (extensionData = {}) => {
  const supportsClusterProfiles = extensionData.supports_cluster_profiles || false;

  return {
    supportsClusterProfiles: Stream(supportsClusterProfiles),
    capabilities:            Stream(ElasticPluginCapabilities.fromJSON(extensionData.capabilities)),
    profileSettings:         Stream(PluggableInstanceSettings.fromJSON(extensionData.elastic_agent_profile_settings)),
    clusterProfileSettings:  Stream(supportsClusterProfiles ? PluggableInstanceSettings.fromJSON(extensionData.cluster_profile_settings) : undefined)
  };
};

PluginInfos.PluginInfo.Extensions['notification'] = () => {
  return {};
};

PluginInfos.PluginInfo.Extensions['package-repository'] = (extensionData = {}) => {
  return {
    packageSettings:    Stream(PluggableInstanceSettings.fromJSON(extensionData.package_settings)),
    repositorySettings: Stream(PluggableInstanceSettings.fromJSON(extensionData.repository_settings)),
  };
};

PluginInfos.PluginInfo.Extensions['scm'] = (extensionData = {}) => {
  return {
    scmSettings: Stream(PluggableInstanceSettings.fromJSON(extensionData.scm_settings)),
  };
};

PluginInfos.PluginInfo.Extensions['task'] = (extensionData = {}) => {
  return {
    taskSettings: Stream(PluggableInstanceSettings.fromJSON(extensionData.task_settings)),
  };
};

PluginInfos.PluginInfo.Extensions['secrets'] = (extensionData = {}) => {
  return {
    secretConfigSettings: Stream(PluggableInstanceSettings.fromJSON(extensionData.secret_config_settings))
  };
};


PluginInfos.PluginInfo.Status = function ({state, messages}) {
  this.state    = Stream(state);
  this.messages = Stream(messages);
};

PluginInfos.PluginInfo.Status.fromJSON = function (data) {
  return new PluginInfos.PluginInfo.Status(data);
};

PluginInfos.PluginInfo.createByType = ({type}) => {
  return new PluginInfos.MultiPluginInfo.fromJSON({
    extensions: [
      {
        type
      }
    ]
  });
};

PluginInfos.PluginInfo.fromJSON = (data = {}) => {
  return data.status && data.status.state === 'active' && typeof data.extensions !== 'undefined' && typeof data.extensions[0] !== 'undefined'
    ? PluginInfos.PluginInfo.MultiPluginInfo.fromJSON(data)
    : PluginInfos.PluginInfo.Bad.fromJSON(data);
};

Mixins.fromJSONCollection({
  parentType: PluginInfos,
  childType:  PluginInfos.PluginInfo,
  via:        'addPluginInfo'
});

