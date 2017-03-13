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

const _      = require('lodash');
const Stream = require('mithril/stream');
const Mixins = require('models/mixins/model_mixins');

const PluggableInstanceSettings = function({viewTemplate, configurations}) {
  this.viewTemplate   = Stream(viewTemplate);
  this.configurations = Stream(configurations);
};

PluggableInstanceSettings.fromJSON = (data = {}) => new PluggableInstanceSettings({
  configurations: PluggableInstanceSettings.Configurations.fromJSON(data.configurations),
  viewTemplate:   _.get(data, 'view.template'),
});

PluggableInstanceSettings.Configurations = function (data) {
  Mixins.HasMany.call(this, {
    factory:    PluggableInstanceSettings.Configurations.Configuration.create,
    as:         'Configuration',
    collection: data,
    uniqueOn:   'key'
  });

};

PluggableInstanceSettings.Configurations.Configuration = function({key, metadata}) {
  this.parent   = Mixins.GetterSetter();
  this.key      = Stream(key);
  this.metadata = Stream(metadata);
};

PluggableInstanceSettings.Configurations.Configuration.create = (data) => new PluggableInstanceSettings.Configurations.Configuration(data);

PluggableInstanceSettings.Configurations.Configuration.fromJSON = (data = {}) => new PluggableInstanceSettings.Configurations.Configuration.create({
  key:      data.key,
  metadata: data.metadata
});

Mixins.fromJSONCollection({
  parentType: PluggableInstanceSettings.Configurations,
  childType:  PluggableInstanceSettings.Configurations.Configuration,
  via:        'addConfiguration'
});


module.exports = PluggableInstanceSettings;
