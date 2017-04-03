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
const _                    = require('lodash');
const $                    = require('jquery');
const s                    = require('string-plus');
const mrequest             = require('helpers/mrequest');
const Errors               = require('models/mixins/errors');
const Validatable          = require('models/mixins/validatable_mixin');
const PluginConfigurations = require('models/shared/plugin_configurations');
const Routes               = require('gen/js-routes');
const SCMs                 = Stream([]);
SCMs.scmIdToEtag         = {};

SCMs.SCM = function (data) {
  Validatable.call(this, data);
  this.validatePresenceOf('name');

  this.init = function (data) {
    this.id             = Stream(s.defaultToIfBlank(data.id));
    this.name           = Stream(s.defaultToIfBlank(data.name, ''));
    this.autoUpdate     = Stream(s.defaultToIfBlank(data.auto_update, true));
    this.pluginMetadata = Stream(new SCMs.SCM.PluginMetadata(data.plugin_metadata || {}));
    this.configuration  = s.collectionToJSON(Stream(SCMs.SCM.Configurations.fromJSON(data.configuration || {})));
    this.errors         = Stream(new Errors(data.errors));
  };

  this.init(data);

  this.reInitialize = function (data) {
    this.init(data);
  };

  this.clone = function () {
    return new SCMs.SCM(JSON.parse(JSON.stringify(this)));
  };

  this.toJSON = function () {
    /* eslint-disable camelcase */
    return {
      id:              this.id(),
      name:            this.name(),
      auto_update:     this.autoUpdate(),
      plugin_metadata: this.pluginMetadata().toJSON(),
      configuration:   this.configuration
    };
    /* eslint-enable camelcase */
  };

  this.update = function () {
    const entity = this;

    const config = (xhr) => {
      xhr.setRequestHeader("Content-Type", "application/json");
      xhr.setRequestHeader("Accept", "application/vnd.go.cd.v1+json");
      xhr.setRequestHeader("If-Match", SCMs.scmIdToEtag[entity.id()]);
    };

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'PATCH',
        url:         Routes.apiv1AdminScmPath({material_name: entity.name()}), //eslint-disable-line camelcase
        background:  false,
        beforeSend:  config,
        data:        JSON.stringify(entity),
        contentType: 'application/json'
      });

      const callback = (data, _textStatus, jqXHR) => {
        if (jqXHR.status === 200) {
          SCMs.scmIdToEtag[data.id] = jqXHR.getResponseHeader('ETag');
        }
        deferred.resolve(new SCMs.SCM(data));
      };

      const errback = ({responseJSON}) => {
        deferred.reject(responseJSON);
      };

      jqXHR.then(callback, errback);

    }).promise();


  };

  this.create = function () {
    const entity = this;

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'POST',
        url:         Routes.apiv1AdminScmsPath(),
        background:  false,
        beforeSend:  mrequest.xhrConfig.forVersion('v1'),
        data:        JSON.stringify(entity),
        contentType: 'application/json'
      });

      const resolve = (data, _textStatus, jqXHR) => {
        if (jqXHR.status === 200) {
          SCMs.scmIdToEtag[data.id] = jqXHR.getResponseHeader('ETag');
        }
        deferred.resolve(new SCMs.SCM(data));
      };

      const errback = ({responseJSON}) => {
        deferred.reject(responseJSON);
      };

      jqXHR.then(resolve, errback);

    }).promise();
  };
};

SCMs.SCM.PluginMetadata = function({id, version}) {
  this.id      = Stream(s.defaultToIfBlank(id, ''));
  this.version = Stream(s.defaultToIfBlank(version, ''));

  this.toJSON = function () {
    return {
      id:      this.id(),
      version: this.version()
    };
  };
};

SCMs.SCM.Configurations = PluginConfigurations;

SCMs.init = () => $.Deferred(function () {
  const deferred = this;

  const jqXHR = $.ajax({
    method:      'GET',
    url:         Routes.apiv1AdminScmsPath(),
    timeout:     mrequest.timeout,
    beforeSend:  mrequest.xhrConfig.forVersion('v1'),
    contentType: false
  });

  jqXHR.then(({_embedded}, _textStatus, _jqXHR) => {
    SCMs(_.map(_embedded.scms, (scm) => new SCMs.SCM(scm)));
    deferred.resolve();
  });

}).promise();

SCMs.filterByPluginId = (pluginId) => _.filter(SCMs(), (scm) => scm.pluginMetadata().id() === pluginId);

SCMs.findById = (id) => {
  const scm = _.find(SCMs(), (scm) => scm.id() === id);

  if (!scm) {
    return null;
  }

  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv1AdminScmPath({material_name: scm.name()}),  //eslint-disable-line camelcase
      background:  false,
      beforeSend:  mrequest.xhrConfig.forVersion('v1'),
      contentType: false
    });

    jqXHR.then((data, _textStatus, jqXHR) => {
      SCMs.scmIdToEtag[scm.id()] = jqXHR.getResponseHeader('ETag');
      deferred.resolve(new SCMs.SCM(data));
    });

  }).promise();

};

module.exports = SCMs;
