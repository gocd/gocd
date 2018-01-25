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

const _                               = require('lodash');
const Stream                          = require('mithril/stream');
const Mixins                          = require('models/mixins/model_mixins');
const Routes                          = require('gen/js-routes');
const CrudMixins                      = require('models/mixins/crud_mixins');
const PluggableInstanceSettings       = require('models/shared/plugin_infos/pluggable_instance_settings');
const AuthorizationPluginCapabilities = require('models/shared/plugin_infos/authorization_plugin_capabilities');
const ElasticPluginCapabilities       = require('models/shared/plugin_infos/elastic_plugin_capabilities');
const AnalyticsPluginCapabilities     = require('models/shared/plugin_infos/analytics_plugin_capabilities');
const About                           = require('models/shared/plugin_infos/about');

const PluginInfos = function (data) {
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
    return new PluginInfos(this.filterPluginInfo((pi) => pi.type() === type));
  };

  this.pluggableTasksTypes = function () {
    const pluggableTasks = {};
    this.filterByType('task').eachPluginInfo((pluginInfo) => {
      pluggableTasks[pluginInfo.id()] = {description: pluginInfo.id()};
    });

    return pluggableTasks;
  };

};

PluginInfos.API_VERSION = 'v4';

CrudMixins.Index({
  type:     PluginInfos,
  indexUrl: Routes.apiv4AdminPluginInfoIndexPath(),
  version:  PluginInfos.API_VERSION,
  dataPath: '_embedded.plugin_info'
});

PluginInfos.PluginInfo = function (type, {about, bundledPlugin, extensions, id, imageUrl, pluginFileLocation, pluginSettings, status}) {
  this.constructor.modelType = 'pluginInfo';
  this.parent                = Mixins.GetterSetter();
  Mixins.HasUUID.call(this);

  this.type = Stream(type);

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

PluginInfos.PluginInfo.ConfigRepo = function (data) {
  PluginInfos.PluginInfo.call(this, "configrepo", data);
};

PluginInfos.PluginInfo.ConfigRepo.fromJSON = (data = {}) => new PluginInfos.PluginInfo.ConfigRepo({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  pluginSettings:     PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].plugin_settings),
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),
});

PluginInfos.PluginInfo.Notification = function (data) {
  PluginInfos.PluginInfo.call(this, "notification", data);
};

PluginInfos.PluginInfo.Notification.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Notification({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  pluginSettings:     PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].plugin_settings),
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),
});

PluginInfos.PluginInfo.PackageRepository = function (data) {
  PluginInfos.PluginInfo.call(this, "package-repository", data);

  this.packageSettings    = Stream(data.packageSettings);
  this.repositorySettings = Stream(data.repositorySettings);
};

PluginInfos.PluginInfo.PackageRepository.fromJSON = (data = {}) => new PluginInfos.PluginInfo.PackageRepository({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  pluginSettings:     PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].plugin_settings),
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),
  packageSettings:    PluggableInstanceSettings.fromJSON(_.get(data, "extensions[0].package_settings")),
  repositorySettings: PluggableInstanceSettings.fromJSON(_.get(data, "extensions[0].repository_settings")),
});

PluginInfos.PluginInfo.Task = function (data) {
  PluginInfos.PluginInfo.call(this, "task", data);

  this.taskSettings = Stream(data.taskSettings);
};

PluginInfos.PluginInfo.Task.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Task({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),

  taskSettings:       PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].task_settings),
});

PluginInfos.PluginInfo.SCM = function (data) {
  PluginInfos.PluginInfo.call(this, "scm", data);

  this.scmSettings = Stream(data.scmSettings);
};

PluginInfos.PluginInfo.SCM.fromJSON = (data = {}) => new PluginInfos.PluginInfo.SCM({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  pluginSettings:     PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].plugin_settings),
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),
  scmSettings:        PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].scm_settings),
});

PluginInfos.PluginInfo.Authorization = function (data) {
  PluginInfos.PluginInfo.call(this, "authorization", data);

  this.authConfigSettings = Stream(data.authConfigSettings);
  this.capabilities       = Stream(data.capabilities);
  this.roleSettings       = Stream(data.roleSettings);
};

PluginInfos.PluginInfo.Authorization.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Authorization({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),

  authConfigSettings: PluggableInstanceSettings.fromJSON(_.get(data, "extensions[0].auth_config_settings")),
  capabilities:       AuthorizationPluginCapabilities.fromJSON(_.get(data, "extensions[0].capabilities")),
  roleSettings:       PluggableInstanceSettings.fromJSON(_.get(data, "extensions[0].role_settings")),
});

PluginInfos.PluginInfo.ElasticAgent = function (data) {
  PluginInfos.PluginInfo.call(this, "elastic-agent", data);

  this.capabilities    = Stream(data.capabilities);
  this.profileSettings = Stream(data.profileSettings);
};

PluginInfos.PluginInfo.ElasticAgent.fromJSON = (data = {}) => new PluginInfos.PluginInfo.ElasticAgent({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  pluginSettings:     PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].plugin_settings),
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),

  capabilities:       ElasticPluginCapabilities.fromJSON(_.get(data, "extensions[0].capabilities")),
  profileSettings:    PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].profile_settings),
});

PluginInfos.PluginInfo.Artifact = function (data) {
  PluginInfos.PluginInfo.call(this, "artifact", data);

  this.artifactConfigSettings = Stream(data.artifactConfigSettings);
  this.fetchArtifactSettings  = Stream(data.fetchArtifactSettings);
  this.storeConfigSettings    = Stream(data.storeConfigSettings);
};

PluginInfos.PluginInfo.Artifact.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Artifact({
  about:                  About.fromJSON(data.about),
  bundledPlugin:          data.bundled_plugin,
  extensions:             data.extensions,
  id:                     data.id,
  imageUrl:               _.get(data, '_links.image.href'),
  pluginFileLocation:     data.plugin_file_location,
  status:                 PluginInfos.PluginInfo.Status.fromJSON(data.status),

  artifactConfigSettings: PluggableInstanceSettings.fromJSON(_.get(data, "extensions[0].artifact_config_settings")),
  fetchArtifactSettings:  PluggableInstanceSettings.fromJSON(_.get(data, "extensions[0].fetch_artifact_settings")),
  storeConfigSettings:    PluggableInstanceSettings.fromJSON(_.get(data, "extensions[0].store_config_settings")),
});

PluginInfos.PluginInfo.Analytics = function (data) {
  PluginInfos.PluginInfo.call(this, "analytics", data);

  this.capabilities    = Stream(data.capabilities);
};

PluginInfos.PluginInfo.Analytics.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Analytics({
  about:              About.fromJSON(data.about),
  bundledPlugin:      data.bundled_plugin,
  extensions:         data.extensions,
  id:                 data.id,
  imageUrl:           _.get(data, '_links.image.href'),
  pluginFileLocation: data.plugin_file_location,
  pluginSettings:     PluggableInstanceSettings.fromJSON(data.extensions[0] && data.extensions[0].plugin_settings),
  status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),

  capabilities:       AnalyticsPluginCapabilities.fromJSON(_.get(data, "extensions[0].capabilities")),
});

PluginInfos.PluginInfo.Bad = function (data) {
  PluginInfos.PluginInfo.call(this, null, data);
};

PluginInfos.PluginInfo.Bad.fromJSON = function (data) {
  return new PluginInfos.PluginInfo.Bad({
    about:              About.fromJSON(data.about),
    bundledPlugin:      data.bundled_plugin,
    extensions:         data.extensions,
    id:                 data.id,
    pluginFileLocation: data.plugin_file_location,
    status:             PluginInfos.PluginInfo.Status.fromJSON(data.status),
  });
};

PluginInfos.PluginInfo.Status = function ({state, messages}) {
  this.state    = Stream(state);
  this.messages = Stream(messages);
};

PluginInfos.PluginInfo.Status.fromJSON = function (data) {
  return new PluginInfos.PluginInfo.Status(data);
};

PluginInfos.PluginInfo.createByType = ({type}) => new PluginInfos.Types[type]({});

PluginInfos.PluginInfo.fromJSON = (data = {}) => (data.status && data.status.state === 'active' && typeof data.extensions !== 'undefined' && typeof data.extensions[0] !== 'undefined') ? PluginInfos.Types[data.extensions[0].type].fromJSON(data) : PluginInfos.PluginInfo.Bad.fromJSON(data);

PluginInfos.Types = {
  'artifact':           PluginInfos.PluginInfo.Artifact,
  'authorization':      PluginInfos.PluginInfo.Authorization,
  'notification':       PluginInfos.PluginInfo.Notification,
  'elastic-agent':      PluginInfos.PluginInfo.ElasticAgent,
  'package-repository': PluginInfos.PluginInfo.PackageRepository,
  'task':               PluginInfos.PluginInfo.Task,
  'scm':                PluginInfos.PluginInfo.SCM,
  'configrepo':         PluginInfos.PluginInfo.ConfigRepo,
  'analytics' :         PluginInfos.PluginInfo.Analytics,
};

Mixins.fromJSONCollection({
  parentType: PluginInfos,
  childType:  PluginInfos.PluginInfo,
  via:        'addPluginInfo'
});

module.exports = PluginInfos;
