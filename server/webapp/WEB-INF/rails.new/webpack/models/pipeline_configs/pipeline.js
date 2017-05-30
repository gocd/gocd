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

const m                    = require('mithril');
const Stream               = require('mithril/stream');
const _                    = require('lodash');
const s                    = require('string-plus');
const Mixins               = require('models/mixins/model_mixins');
const EnvironmentVariables = require('models/pipeline_configs/environment_variables');
const Parameters           = require('models/pipeline_configs/parameters');
const Materials            = require('models/pipeline_configs/materials');
const TrackingTool         = require('models/pipeline_configs/tracking_tool');
const Stages               = require('models/pipeline_configs/stages');
const mrequest             = require('helpers/mrequest');
const Validatable          = require('models/mixins/validatable_mixin');
const Routes               = require('gen/js-routes');
const $                    = require('jquery');

const Pipeline = function (data) {
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
    const timer = this();

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
    const value = this();
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
    const config = (xhr) => {
      xhr.setRequestHeader("Content-Type", "application/json");
      xhr.setRequestHeader("Accept", "application/vnd.go.cd.v3+json");
      xhr.setRequestHeader("If-Match", etag);
    };

    const entity = this;

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
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

      jqXHR.fail(({responseJSON}, _textStatus, _error) => {
        deferred.reject(responseJSON);
      });

      jqXHR.always(m.redraw);

    }).promise();


  };

  this.isFirstStageAutoTriggered = function () {
    return this.stages().countStage() === 0 ? true : this.stages().firstStage().approval().isSuccess();
  };
};

Pipeline.fromJSON = (data) => new Pipeline({
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

Pipeline.Timer.fromJSON = (data) => {
  if (!_.isEmpty(data)) {
    return new Pipeline.Timer({
      spec:          data.spec,
      onlyOnChanges: data.only_on_changes,
      errors:        data.errors
    });
  }
};

Pipeline.API_VERSION = 'v3';

Pipeline.find = (url, extract) => $.Deferred(() => {

  const jqXHR = $.ajax({
    method:      'GET',
    url,
    beforeSend:  mrequest.xhrConfig.forVersion(Pipeline.API_VERSION),
    contentType: false
  });

  jqXHR.done(extract);

  jqXHR.always(() => {
    m.redraw();
  });

}).promise();

Pipeline.vm = function () {
  this.saveState = Stream('');
  this.pageSaveSpinner = Stream('');
  this.pageSaveState = Stream('');
  let errors     = [];

  this.updating = function () {
    this.saveState('in-progress disabled');
    this.pageSaveSpinner('page-spinner');
    this.pageSaveState('page-save-in-progress');
  };

  this.saveFailed = function (data) {
    if (_.has(data, 'message')) {
      errors.push(data.message);
    }

    if(_.has(data, 'data.errors')) {
      errors = _.concat(errors, _.flattenDeep(_.values(data.data.errors)));
    }

    this.saveState('alert');
    this.pageSaveSpinner('');
    this.pageSaveState('');
  };

  this.saveSuccess = function () {
    this.saveState('success');
    this.pageSaveSpinner('');
    this.pageSaveState('');
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
