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

define(['mithril', 'lodash', 'string-plus', '../helpers/mrequest'], function (m, _, s, mrequest) {
  var Plugins = m.prop([]);

  Plugins.init = function () {
    var unwrap = function (response) {
      return response._embedded.plugins;
    };

    return m.request({
      method:        'GET',
      url:           Routes.apiv1AdminPluginsPath(),
      background:    true,
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: unwrap,
      type:          Plugins.Plugin
    }).then(function(data) {
      Plugins(data);
    });
  };

  Plugins.findById = function (id) {
    return _.find(Plugins(), function (plugin) {
      return _.isEqual(plugin.id(), id);
    });
  };

  Plugins.filterByType = function (type) {
    return _.filter(Plugins(), function (plugin) {
      return _.isEqual(plugin.type(), type);
    });
  };

  Plugins.Plugin = function (data) {
    this.id             = m.prop(data.id);
    this.name           = m.prop(data.name);
    this.version        = m.prop(data.version);
    this.type           = m.prop(data.type);
    this.viewTemplate   = m.prop(s.defaultToIfBlank(data.viewTemplate, ''));
    this.configurations = m.prop(s.defaultToIfBlank(data.configurations, {}));
  };

  Plugins.Plugin.byId = function (id) {
    return m.request({
      method: 'GET',
      url:    Routes.apiv1AdminPluginPath({id: id}),
      config: mrequest.xhrConfig.v1,
      type:   Plugins.Plugin
    });
  };

  return Plugins;
});