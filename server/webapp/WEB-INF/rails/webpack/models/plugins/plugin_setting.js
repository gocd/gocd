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

const PluginSetting = function (data) {
  this.pluginId   = Stream(s.defaultToIfBlank(data.pluginId, ''));
  this.configuration = s.collectionToJSON(Stream(s.defaultToIfBlank(data.configuration, new PluginConfigurations())));
  this.etag       = Mixins.GetterSetter();

  Validatable.call(this, data);
  this.validatePresenceOf('pluginId');

  CrudMixins.AllOperations.call(this, ['refresh', 'update', 'create'],
    {
      type:     PluginSetting,
      indexUrl: Routes.apiv1AdminPluginSettingsPath(),
      version:  PluginSetting.API_VERSION,
      resourceUrl(pluginSettings) {
        return Routes.apiv1AdminPluginSettingPath(pluginSettings.pluginId());
      }
    }
  );
};

PluginSetting.get = (pluginId) => new PluginSetting({pluginId}).refresh();

PluginSetting.create = (data) => new PluginSetting(data);

PluginSetting.fromJSON = ({plugin_id, errors, configuration}) => new PluginSetting({ //eslint-disable-line camelcase
  pluginId:   plugin_id, //eslint-disable-line camelcase
  errors,
  configuration: PluginConfigurations.fromJSON(configuration) //eslint-disable-line camelcase
});


PluginSetting.API_VERSION = 'v1';

module.exports = PluginSetting;
