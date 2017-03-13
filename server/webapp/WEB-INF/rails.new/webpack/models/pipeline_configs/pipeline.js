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

var m                    = require('mithril');
var Stream               = require('mithril/stream');
var _                    = require('lodash');
var s                    = require('string-plus');
var Mixins               = require('models/mixins/model_mixins');
var EnvironmentVariables = require('models/pipeline_configs/environment_variables');
var Parameters           = require('models/pipeline_configs/parameters');
var Materials            = require('models/pipeline_configs/materials');
var TrackingTool         = require('models/pipeline_configs/tracking_tool');
var Stages               = require('models/pipeline_configs/stages');
var mrequest             = require('helpers/mrequest');
var Validatable          = require('models/mixins/validatable_mixin');
var Routes               = require('gen/js-routes');
var $                    = require('jquery');

var Pipeline = function (data) {
  this.constructor.modelType = 'pipeline';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.name                  = Stream(data.name);
  this.enablePipelineLocking = Stream(data.enablePipelineLocking);
  this.templateName          = Stream(s.defaultToIfBlank(data.templateName, ''));
  this.labelTemplate         = Stream(s.defaultToIfBlank(data.labelTemplate, ''));
  this.template              = Stream(data.template);
  this.timer                 = Stream(s.defaultToIfBlank(data.timer, new Pipeline.Timer({})));
  this.timer.toJSON          = function () {
    var timer = this();

    if (timer && timer.isBlank()) {
      return null;
    }

    return timer;
  };
  this.environmentVariables  = s.collectionToJSON(Stream(s.defaultToIfBlank(data.environmentVariables, new EnvironmentVariables())));
  this.parameters            = s.collectionToJSON(Stream(s.defaultToIfBlank(data.parameters, new Parameters())));
  this.materials             = s.collectionToJSON(Stream(s.defaultToIfBlank(data.materials, new Materials())));
  this.trackingTool          = Stream(data.trackingTool);
  this.trackingTool.toJSON   = function () {
    var value = this();
    if (value) {
      return value.toJSON();
    } else {
      return null;
    }
  };
  this.stages                = s.collectionToJSON(Stream(s.defaultToIfBlank(data.stages, new Stages())));

  this.validatePresenceOf('labelTemplate');
  this.validateFormatOf('labelTemplate', {
    format:  /(([a-zA-Z0-9_\-.!~*'()#:])*[$#]\{[a-zA-Z0-9_\-.!~*'()#:]+(\[:(\d+)])?}([a-zA-Z0-9_\-.!~*'()#:])*)+/,
    message: "Label should be composed of alphanumeric text, it may contain the build number as ${COUNT}, it may contain a material revision as ${<material-name>} or ${<material-name>[:<length>]}, or use params as #{<param-name>}"
  });
  this.validateAssociated('materials');
  this.validateAssociated('environmentVariables');
  this.validateAssociated('parameters');
  this.validateAssociated('stages');
  this.validateAssociated('trackingTool');

  this.update = function (etag, extract) {
    var config = xhr => {
      xhr.setRequestHeader("Content-Type", "application/json");
      xhr.setRequestHeader("Accept", "application/vnd.go.cd.v3+json");
      xhr.setRequestHeader("If-Match", etag);
    };

    var entity = this;

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'PUT',
        url:         Routes.apiv3AdminPipelinePath({pipeline_name: entity.name()}), //eslint-disable-line camelcase
        timeout:     mrequest.timeout,
        beforeSend:  config,
        data:        JSON.stringify(entity, s.snakeCaser),
        contentType: false
      });

      jqXHR.then((_data, _textStatus, jqXHR) => {
        deferred.resolve(extract(jqXHR));
      });

      jqXHR.fail((response, _textStatus, _error) => {
        deferred.reject(response.responseJSON);
      });

      jqXHR.always(m.redraw);

    }).promise();


  };

  this.isFirstStageAutoTriggered = function () {
    return this.stages().countStage() === 0 ? true : this.stages().firstStage().approval().isSuccess();
  };
};

Pipeline.fromJSON = data => new Pipeline({
  name:                  data.name,
  enablePipelineLocking: data.enable_pipeline_locking,
  templateName:          data.template_name,
  labelTemplate:         data.label_template,
  template:              data.template,
  timer:                 Pipeline.Timer.fromJSON(data.timer),
  trackingTool:          TrackingTool.fromJSON(data.tracking_tool),
  environmentVariables:  EnvironmentVariables.fromJSON(data.environment_variables),
  parameters:            Parameters.fromJSON(data.parameters),
  materials:             Materials.fromJSON(data.materials),
  stages:                Stages.fromJSON(data.stages)
});

Pipeline.Timer = function (data) {
  this.constructor.modelType = 'pipelineTimer';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.spec          = Stream(s.defaultToIfBlank(data.spec, ''));
  this.onlyOnChanges = Stream(data.onlyOnChanges);

  this.isBlank = function () {
    return s.isBlank(this.spec()) && !this.onlyOnChanges();
  };
};

Pipeline.Timer.fromJSON = data => {
  if (!_.isEmpty(data)) {
    return new Pipeline.Timer({
      spec:          data.spec,
      onlyOnChanges: data.only_on_changes,
      errors:        data.errors
    });
  }
};

Pipeline.find = (url, extract) => $.Deferred(() => {

  var jqXHR = $.ajax({
    method:      'GET',
    url:         url,
    beforeSend:  mrequest.xhrConfig.forVersion('v3'),
    contentType: false
  });

  jqXHR.done(extract);

  jqXHR.always(() => {
    m.redraw();
  });

}).promise();

Pipeline.vm = function () {
  this.saveState = Stream('');
  var errors     = [];

  this.updating = function () {
    this.saveState('in-progress disabled');
  };

  this.saveFailed = function (data) {
    errors.push(data.message);

    if (data.data) {
      if (data.data.errors) {
        errors = _.concat(errors, _.flattenDeep(_.values(data.data.errors)));
      }
    }

    this.saveState('alert');
  };

  this.saveSuccess = function () {
    this.saveState('success');
  };

  this.defaultState = function () {
    this.saveState('');
  };

  this.clearErrors = () => {
    errors = [];
  };

  this.errors = () => errors;

  this.hasErrors = () => !_.isEmpty(errors);

  this.markClientSideErrors = () => {
    errors.push('There are errors on the page, fix them and save');
  };
};

module.exports = Pipeline;
