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

var $           = require('jquery');
var Stream      = require('mithril/stream');
var _           = require('lodash');
var s           = require('string-plus');
var mrequest    = require('helpers/mrequest');
var Image       = require('models/shared/image');
var Routes      = require('gen/js-routes');
var PluginInfos = Stream([]);

PluginInfos.init = function (type) {
  return PluginInfos.all(type).then(PluginInfos);
};

PluginInfos.all = function (type) {
  return $.Deferred(function () {
    var deferred = this;

    var jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv2AdminPluginInfoIndexPath({'type': type}),
      beforeSend:  mrequest.xhrConfig.forVersion('v2'),
      contentType: false
    });

    jqXHR.done(function (data, _textStatus, _jqXHR) {
      let pluginInfos = _.map(data._embedded.plugin_info, function (pluginInfo) {
        return new PluginInfos.PluginInfo(pluginInfo);
      });
      deferred.resolve(pluginInfos);
    });
  }).promise();
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

PluginInfos.PluginInfo.get = function (id) {
  return $.Deferred(function () {
    var deferred = this;

    var jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv2AdminPluginInfoPath({id: id}),
      beforeSend:  mrequest.xhrConfig.forVersion('v2'),
      contentType: false
    });

    jqXHR.done(function (data, _textStatus, _jqXHR) {
      deferred.resolve(new PluginInfos.PluginInfo(data));
    });

    jqXHR.fail(function (jqXHR, _textStatus, _errorThrown) {
      deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
    });
  }).promise();
};

module.exports = PluginInfos;