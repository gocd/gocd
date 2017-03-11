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

var Stream               = require('mithril/stream');
var _                    = require('lodash');
var $                    = require('jquery');
var s                    = require('string-plus');
var mrequest             = require('helpers/mrequest');
var Errors               = require('models/mixins/errors');
var Validatable          = require('models/mixins/validatable_mixin');
var PluginConfigurations = require('models/shared/plugin_configurations');
var Routes               = require('gen/js-routes');
var SCMs                 = Stream([]);
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
    var entity = this;

    var config = function (xhr) {
      xhr.setRequestHeader("Content-Type", "application/json");
      xhr.setRequestHeader("Accept", "application/vnd.go.cd.v1+json");
      xhr.setRequestHeader("If-Match", SCMs.scmIdToEtag[entity.id()]);
    };

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'PATCH',
        url:         Routes.apiv1AdminScmPath({material_name: entity.name()}), //eslint-disable-line camelcase
        background:  false,
        beforeSend:  config,
        data:        JSON.stringify(entity),
        contentType: 'application/json'
      });

      var callback = function (data, _textStatus, jqXHR) {
        if (jqXHR.status === 200) {
          SCMs.scmIdToEtag[data.id] = jqXHR.getResponseHeader('ETag');
        }
        deferred.resolve(new SCMs.SCM(data));
      };

      var errback = function (jqXHR) {
        deferred.reject(jqXHR.responseJSON);
      };

      jqXHR.then(callback, errback);

    }).promise();


  };

  this.create = function () {
    var entity = this;

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'POST',
        url:         Routes.apiv1AdminScmsPath(),
        background:  false,
        beforeSend:  mrequest.xhrConfig.forVersion('v1'),
        data:        JSON.stringify(entity),
        contentType: 'application/json'
      });

      var resolve = function (data, _textStatus, jqXHR) {
        if (jqXHR.status === 200) {
          SCMs.scmIdToEtag[data.id] = jqXHR.getResponseHeader('ETag');
        }
        deferred.resolve(new SCMs.SCM(data));
      };

      var errback = function (jqXHR) {
        deferred.reject(jqXHR.responseJSON);
      };

      jqXHR.then(resolve, errback);

    }).promise();
  };
};

SCMs.SCM.PluginMetadata = function (data) {
  this.id      = Stream(s.defaultToIfBlank(data.id, ''));
  this.version = Stream(s.defaultToIfBlank(data.version, ''));

  this.toJSON = function () {
    return {
      id:      this.id(),
      version: this.version()
    };
  };
};

SCMs.SCM.Configurations = PluginConfigurations;

SCMs.init = function () {
  return $.Deferred(function () {
    var deferred = this;

    var jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv1AdminScmsPath(),
      timeout:     mrequest.timeout,
      beforeSend:  mrequest.xhrConfig.forVersion('v1'),
      contentType: false
    });

    jqXHR.then(function (data, _textStatus, _jqXHR) {
      SCMs(_.map(data._embedded.scms, function (scm) {
        return new SCMs.SCM(scm);
      }));
      deferred.resolve();
    });
  }).promise();
};

SCMs.filterByPluginId = function (pluginId) {
  return _.filter(SCMs(), (scm) => scm.pluginMetadata().id() === pluginId);
};

SCMs.findById = function (id) {
  var scm = _.find(SCMs(), (scm) => scm.id() === id);

  if (!scm) {
    return null;
  }

  return $.Deferred(function () {
    var deferred = this;

    var jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv1AdminScmPath({material_name: scm.name()}),  //eslint-disable-line camelcase
      background:  false,
      beforeSend:  mrequest.xhrConfig.forVersion('v1'),
      contentType: false
    });

    jqXHR.then(function (data, _textStatus, jqXHR) {
      SCMs.scmIdToEtag[scm.id()] = jqXHR.getResponseHeader('ETag');
      deferred.resolve(new SCMs.SCM(data));
    });

  }).promise();

};

//SCMs.vm = function () {
//  this.saveState = Stream('');
//  var errors     = [];
//
//  this.startUpdating = function () {
//    errors = [];
//    this.saveState('in-progress disabled');
//  };
//
//  this.saveFailed = function (data) {
//    errors.push(data.message);
//
//    if (data.data) {
//      if (data.data.configuration) {
//        errors = _.concat(errors, _.flattenDeep(_.map(data.data.configuration, function (conf) {
//          return _.values(conf.errors);
//        })));
//      }
//    }
//
//    this.saveState('alert');
//  };
//
//  this.saveSuccess = function () {
//    this.saveState('success');
//  };
//
//  this.clearErrors = function () {
//    errors = [];
//  };
//
//  this.reset = function () {
//    errors = [];
//    this.saveState('');
//  };
//
//  this.errors = function () {
//    return errors;
//  };
//
//  this.hasErrors = function () {
//    return !_.isEmpty(errors);
//  };
//
//  this.markClientSideErrors = function () {
//    errors.push('There are errors on the page, fix them and save');
//  };
//};

module.exports = SCMs;
