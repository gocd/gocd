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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/pipeline_configs/environment_variables', 'models/pipeline_configs/parameters',
  'models/pipeline_configs/materials', 'models/pipeline_configs/tracking_tool', 'models/pipeline_configs/stages', 'helpers/mrequest', 'models/validatable_mixin', 'js-routes'
], function (m, _, s, Mixins, EnvironmentVariables, Parameters, Materials, TrackingTool, Stages, mrequest, Validatable, Routes) {
  var Pipeline = function (data) {
    this.constructor.modelType = 'pipeline';
    Mixins.HasUUID.call(this);
    Validatable.call(this, data);

    this.name                  = m.prop(data.name);
    this.enablePipelineLocking = m.prop(data.enablePipelineLocking);
    this.templateName          = m.prop(s.defaultToIfBlank(data.templateName, ''));
    this.labelTemplate         = m.prop(s.defaultToIfBlank(data.labelTemplate, ''));
    this.template              = m.prop(data.template);
    this.timer                 = m.prop(s.defaultToIfBlank(data.timer, new Pipeline.Timer({})));
    this.timer.toJSON          = function () {
      var timer = this();

      if (timer && timer.isBlank()) {
        return null;
      }

      return timer;
    };
    this.environmentVariables  = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.environmentVariables, new EnvironmentVariables())));
    this.parameters            = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.parameters, new Parameters())));
    this.materials             = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.materials, new Materials())));
    this.trackingTool          = m.prop(data.trackingTool);
    this.trackingTool.toJSON   = function () {
      var value = this();
      if (value) {
        return value.toJSON();
      } else {
        return null;
      }
    };
    this.stages                = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.stages, new Stages())));

    this.validatePresenceOf('labelTemplate');
    this.validateFormatOf('labelTemplate', {format: /(([a-zA-Z0-9_\-.!~*'()#:])*[$#]\{[a-zA-Z0-9_\-.!~*'()#:]+(\[:(\d+)])?}([a-zA-Z0-9_\-.!~*'()#:])*)+/,
                                            message: "Label should be composed of alphanumeric text, it may contain the build number as ${COUNT}, it may contain a material revision as ${<material-name>} or ${<material-name>[:<length>]}, or use params as #{<param-name>}"});
    this.validateAssociated('materials');
    this.validateAssociated('environmentVariables');
    this.validateAssociated('parameters');
    this.validateAssociated('stages');
    this.validateAssociated('trackingTool');

    this.update = function (etag, extract) {
      var self = this;

      var config =  function (xhr) {
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("Accept", "application/vnd.go.cd.v3+json");
        xhr.setRequestHeader("If-Match", etag);
      };

      return m.request({
        method: 'PATCH',
        url:     Routes.apiv1AdminPipelinePath({pipeline_name: self.name()}), //eslint-disable-line camelcase
        config:  config,
        extract: extract,
        data:    JSON.parse(JSON.stringify(this, s.snakeCaser))
      });
    };
  };

  Pipeline.fromJSON = function (data) {
    return new Pipeline({
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
  };

  Pipeline.Timer = function (data) {
    this.constructor.modelType = 'pipelineTimer';
    Mixins.HasUUID.call(this);

    this.spec          = m.prop(s.defaultToIfBlank(data.spec, ''));
    this.onlyOnChanges = m.prop(data.onlyOnChanges);

    this.isBlank = function () {
      return s.isBlank(this.spec()) && !this.onlyOnChanges();
    };
  };

  Pipeline.Timer.fromJSON = function (data) {
    if (!_.isEmpty(data)) {
      return new Pipeline.Timer({
        spec:          data.spec,
        onlyOnChanges: data.only_on_changes
      });
    }
  };

  Pipeline.find = function (url, extract) {
    return m.request({
      method:     'GET',
      url:        url,
      background: false,
      config:     mrequest.xhrConfig.v3,
      extract:    extract
    });
  };

  Pipeline.vm = function () {
    this.saveState = m.prop('');
    var errors    = [];

    this.updating = function () {
      this.saveState('in-progress disabled');
    };

    this.saveFailed = function (data) {
      errors.push(data.message);

      if(data.data) {
        if(data.data.errors) {
          errors = _.concat(errors, _.flattenDeep(_.values(data.data.errors)));
        }
      }

      this.saveState('alert');
    };

    this.saveSuccess = function () {
      this.saveState('success');
    };

    this.clearErrors = function () {
      errors = [];
    };

    this.errors = function () {
      return errors;
    };

    this.hasErrors = function () {
      return !_.isEmpty(errors);
    };

    this.markClientSideErrors = function () {
      errors.push('There are errors on the page, fix them and save');
    };
  };

  return Pipeline;
});
