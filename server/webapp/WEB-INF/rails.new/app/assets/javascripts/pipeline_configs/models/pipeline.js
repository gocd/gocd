/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'string-plus', './model_mixins', './environment_variables', './parameters', './materials', './tracking_tool', './stages'], function (m, _, s, Mixins, EnvironmentVariables, Parameters, Materials, TrackingTool, Stages) {
  var Pipeline = function (data) {
    this.constructor.modelType = 'pipeline';
    Mixins.HasUUID.call(this);

    this.name                  = m.prop(data.name);
    this.enablePipelineLocking = m.prop(data.enablePipelineLocking);
    this.templateName          = m.prop(s.defaultToIfBlank(data.templateName, ''));
    this.labelTemplate         = m.prop(s.defaultToIfBlank(data.labelTemplate, ''));
    this.timer                 = m.prop(s.defaultToIfBlank(data.timer, new Pipeline.Timer({})));
    this.environmentVariables  = s.overrideToJSON(m.prop(s.defaultToIfBlank(data.environmentVariables, new EnvironmentVariables())));
    this.parameters            = s.overrideToJSON(m.prop(s.defaultToIfBlank(data.parameters, new Parameters())));
    this.materials             = s.overrideToJSON(m.prop(s.defaultToIfBlank(data.materials, new Materials())));
    this.trackingTool          = s.overrideToJSON(m.prop(data.trackingTool));
    this.stages                = s.overrideToJSON(m.prop(s.defaultToIfBlank(data.stages, new Stages())));

    this.validate = function () {
      var errors = new Mixins.Errors();

      if (s.isBlank(this.labelTemplate())) {
        errors.add('labelTemplate', Mixins.ErrorMessages.mustBePresent('labelTemplate'));
      } else if (!this.labelTemplate().match(/(([a-zA-Z0-9_\-.!~*'()#:])*[$#]\{[a-zA-Z0-9_\-.!~*'()#:]+(\[:(\d+)])?}([a-zA-Z0-9_\-.!~*'()#:])*)+/)) {
        errors.add('labelTemplate', "Label should be composed of alphanumeric text, it may contain the build number as ${COUNT}, it may contain a material revision as ${<material-name>} or ${<material-name>[:<length>]}, or use params as #{<param-name>}");
      }

      return errors;
    };
  };

  Pipeline.get = function (url) {
    return m.request({method: 'GET', url: url});
  };

  Pipeline.fromJSON = function (data) {
    return new Pipeline({
      name:                  data.name,
      enablePipelineLocking: data.enable_pipeline_locking,
      templateName:          data.template_name,
      labelTemplate:         data.label_template,
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
  };

  Pipeline.Timer.fromJSON = function (data) {
    if (!_.isEmpty(data)) {
      return new Pipeline.Timer({
        spec:          data.spec,
        onlyOnChanges: data.only_on_changes
      });
    }

  };

  return Pipeline;
});
