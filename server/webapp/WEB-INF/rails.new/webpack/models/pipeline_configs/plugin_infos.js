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

const $           = require('jquery');
const Stream      = require('mithril/stream');
const _           = require('lodash');
const s           = require('string-plus');
const mrequest    = require('helpers/mrequest');
const Image       = require('models/shared/image');
const Routes      = require('gen/js-routes');
const PluginInfos = Stream([]);

PluginInfos.init = (type) => PluginInfos.all(type).then(PluginInfos);

PluginInfos.all = (type) => $.Deferred(function () {
  const deferred = this;

  const jqXHR = $.ajax({
    method:      'GET',
    url:         Routes.apiv2AdminPluginInfoIndexPath({'type': type}),
    beforeSend:  mrequest.xhrConfig.forVersion('v2'),
    contentType: false
  });

  jqXHR.done(({_embedded}, _textStatus, _jqXHR) => {
    let pluginInfos = _.map(_embedded.plugin_info, (pluginInfo) => new PluginInfos.PluginInfo(pluginInfo));
    deferred.resolve(pluginInfos);
  });
}).promise();

PluginInfos.findById = (id) => _.find(PluginInfos(), (pluginInfo) => _.isEqual(pluginInfo.id(), id));

PluginInfos.filterByType = (type) => _.filter(PluginInfos(), (pluginInfo) => _.isEqual(pluginInfo.type(), type));

PluginInfos.PluginInfo = function (data) {
  const view = (settings) => settings ? settings.view : {};

  this.id             = Stream(data.id);
  this.name           = Stream(data.name);
  this.displayName    = Stream(s.defaultToIfBlank(data.display_name, data.name));
  this.version        = Stream(data.version);
  this.type           = Stream(data.type);
  this.configurations = data.pluggable_instance_settings ?
    Stream(s.defaultToIfBlank(data.pluggable_instance_settings.configurations, {})) :
    Stream({});
  this.viewTemplate   = Stream(s.defaultToIfBlank(view(data.pluggable_instance_settings).template, ''));

  if (data.image) {
    this.image = Stream(new Image(data.image.content_type, data.image.data));
  }
};

PluginInfos.PluginInfo.get = (id) => $.Deferred(function () {
  const deferred = this;

  const jqXHR = $.ajax({
    method:      'GET',
    url:         Routes.apiv2AdminPluginInfoPath({id}),
    beforeSend:  mrequest.xhrConfig.forVersion('v2'),
    contentType: false
  });

  jqXHR.done((data, _textStatus, _jqXHR) => {
    deferred.resolve(new PluginInfos.PluginInfo(data));
  });

  jqXHR.fail((jqXHR, _textStatus, _errorThrown) => {
    deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
  });
}).promise();

module.exports = PluginInfos;
