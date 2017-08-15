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

const _                         = require('lodash');
const Stream                    = require('mithril/stream');
const Mixins                    = require('models/mixins/model_mixins');
const Routes                    = require('gen/js-routes');
const CrudMixins                = require('models/mixins/crud_mixins');
const PluggableInstanceSettings = require('models/shared/plugin_infos/pluggable_instance_settings');
const Capabilities              = require('models/shared/plugin_infos/capabilities');
const About                     = require('models/shared/plugin_infos/about');

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

PluginInfos.API_VERSION = 'v3';

CrudMixins.Index({
  type:     PluginInfos,
  indexUrl: Routes.apiv3AdminPluginInfoIndexPath(),
  version:  PluginInfos.API_VERSION,
  dataPath: '_embedded.plugin_info'
});

PluginInfos.PluginInfo = function (type, {id, version, about, imageUrl}) {
  this.constructor.modelType = 'pluginInfo';
  this.parent                = Mixins.GetterSetter();
  Mixins.HasUUID.call(this);

  this.type = Stream(type);

  this.id       = Stream(id);
  this.version  = Stream(version);
  this.about    = Stream(about);
  this.imageUrl = Stream(imageUrl);
};

PluginInfos.PluginInfo.Authentication = function (data) {
  PluginInfos.PluginInfo.call(this, "authentication", data);
};

PluginInfos.PluginInfo.Authentication.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Authentication({
  id:       data.id,
  version:  data.version,
  about:    About.fromJSON(data.about),
  imageUrl: _.get(data, '_links.image.href')
});

PluginInfos.PluginInfo.ConfigRepo = function (data) {
  PluginInfos.PluginInfo.call(this, "configrepo", data);
};

PluginInfos.PluginInfo.ConfigRepo.fromJSON = (data = {}) => new PluginInfos.PluginInfo.ConfigRepo({
  id:       data.id,
  version:  data.version,
  about:    About.fromJSON(data.about),
  imageUrl: _.get(data, '_links.image.href')
});

PluginInfos.PluginInfo.Notification = function (data) {
  PluginInfos.PluginInfo.call(this, "notification", data);
};

PluginInfos.PluginInfo.Notification.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Notification({
  id:       data.id,
  version:  data.version,
  about:    About.fromJSON(data.about),
  imageUrl: _.get(data, '_links.image.href')
});

PluginInfos.PluginInfo.PackageRepository = function (data) {
  PluginInfos.PluginInfo.call(this, "package-repository", data);

  this.packageSettings    = Stream(data.packageSettings);
  this.repositorySettings = Stream(data.repositorySettings);
};

PluginInfos.PluginInfo.PackageRepository.fromJSON = (data = {}) => new PluginInfos.PluginInfo.PackageRepository({
  id:                 data.id,
  version:            data.version,
  about:              About.fromJSON(data.about),
  packageSettings:    PluggableInstanceSettings.fromJSON(data.extension_info && data.extension_info.package_settings),
  repositorySettings: PluggableInstanceSettings.fromJSON(data.extension_info && data.extension_info.repository_settings),
  imageUrl:           _.get(data, '_links.image.href')
});

PluginInfos.PluginInfo.Task = function (data) {
  PluginInfos.PluginInfo.call(this, "task", data);

  this.taskSettings = Stream(data.taskSettings);
};

PluginInfos.PluginInfo.Task.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Task({
  id:           data.id,
  version:      data.version,
  about:        About.fromJSON(data.about),
  taskSettings: PluggableInstanceSettings.fromJSON(data.extension_info && data.extension_info.task_settings),
  imageUrl:     _.get(data, '_links.image.href'),
});

PluginInfos.PluginInfo.SCM = function (data) {
  PluginInfos.PluginInfo.call(this, "scm", data);

  this.scmSettings = Stream(data.scmSettings);
};

PluginInfos.PluginInfo.SCM.fromJSON = (data = {}) => new PluginInfos.PluginInfo.SCM({
  id:          data.id,
  version:     data.version,
  about:       About.fromJSON(data.about),
  scmSettings: PluggableInstanceSettings.fromJSON(data.extension_info && data.extension_info.scm_settings),
  imageUrl:    _.get(data, '_links.image.href'),
});

PluginInfos.PluginInfo.Authorization = function (data) {
  PluginInfos.PluginInfo.call(this, "authorization", data);

  this.authConfigSettings = Stream(data.authConfigSettings);
  this.roleSettings       = Stream(data.roleSettings);
  this.capabilities       = Stream(data.capabilities);

};

PluginInfos.PluginInfo.Authorization.fromJSON = (data = {}) => new PluginInfos.PluginInfo.Authorization({
  id:                 data.id,
  version:            data.version,
  about:              About.fromJSON(data.about),
  authConfigSettings: PluggableInstanceSettings.fromJSON(data.extension_info && data.extension_info.auth_config_settings),
  roleSettings:       PluggableInstanceSettings.fromJSON(data.extension_info && data.extension_info.role_settings),
  capabilities:       Capabilities.fromJSON(data.capabilities),
  imageUrl:           _.get(data, '_links.image.href'),
});

PluginInfos.PluginInfo.ElasticAgent = function (data) {
  PluginInfos.PluginInfo.call(this, "elastic-agent", data);
  this.profileSettings = Stream(data.profileSettings);
};

PluginInfos.PluginInfo.ElasticAgent.fromJSON = (data = {}) => new PluginInfos.PluginInfo.ElasticAgent({
  id:              data.id,
  version:         data.version,
  about:           About.fromJSON(data.about),
  profileSettings: PluggableInstanceSettings.fromJSON(data.extension_info && data.extension_info.profile_settings),
  imageUrl:        _.get(data, '_links.image.href'),
});

PluginInfos.PluginInfo.createByType = ({type}) => new PluginInfos.Types[type]({});

PluginInfos.PluginInfo.fromJSON = (data = {}) => {
  if (PluginInfos.Types[data.type]) {
    return PluginInfos.Types[data.type].fromJSON(data);
  } else {
    throw `Could not find plugin type ${data.type}`;
  }
};

PluginInfos.Types = {
  'authentication':     PluginInfos.PluginInfo.Authentication,
  'authorization':      PluginInfos.PluginInfo.Authorization,
  'notification':       PluginInfos.PluginInfo.Notification,
  'elastic-agent':      PluginInfos.PluginInfo.ElasticAgent,
  'package-repository': PluginInfos.PluginInfo.PackageRepository,
  'task':               PluginInfos.PluginInfo.Task,
  'scm':                PluginInfos.PluginInfo.SCM,
  'configrepo':         PluginInfos.PluginInfo.ConfigRepo,
};

Mixins.fromJSONCollection({
  parentType: PluginInfos,
  childType:  PluginInfos.PluginInfo,
  via:        'addPluginInfo'
});

module.exports = PluginInfos;
