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

define(['mithril', 'lodash', 'string-plus', 'helpers/mrequest', 'js-routes'], function (m, _, s, mrequest, Routes) {
  var PluginInfos = m.prop([]);

  PluginInfos.init = function () {
    var unwrap = function (response) {
      return response._embedded.plugin_info;
    };

    return m.request({
      method:        'GET',
      url:           Routes.apiv1AdminPluginInfoIndexPath(),
      background:    true,
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: unwrap,
      type:          PluginInfos.PluginInfo
    }).then(PluginInfos);
  };

  PluginInfos.findById = function (id) {
    return _.find(PluginInfos(), function (pluginInfo) {
      return _.isEqual(pluginInfo.id(), id);
    });
  };

  PluginInfos.filterByType = function (type) {
    return _.filter(PluginInfos(), function (pluginInfo) {
      return _.isEqual(pluginInfo.type(), type);
    });
  };

  PluginInfos.PluginInfo = function (data) {
    var view = function (settings) {
      return settings ? settings.view : {};
    };

    this.id             = m.prop(data.id);
    this.name           = m.prop(data.name);
    this.displayName    = m.prop(s.defaultToIfBlank(data.display_name, data.name));
    this.version        = m.prop(data.version);
    this.type           = m.prop(data.type);
    this.configurations = data.pluggable_instance_settings ?
                            m.prop(s.defaultToIfBlank(data.pluggable_instance_settings.configurations, {})) :
                            m.prop({});
    this.viewTemplate   = m.prop(s.defaultToIfBlank(view(data.pluggable_instance_settings).template, ''));
  };

  PluginInfos.PluginInfo.byId = function (id) {
    return m.request({
      method: 'GET',
      url:    Routes.apiv1AdminPluginInfoPath({id: id}),
      config: mrequest.xhrConfig.v1,
      type:   PluginInfos.PluginInfo
    });
  };

  return PluginInfos;
});