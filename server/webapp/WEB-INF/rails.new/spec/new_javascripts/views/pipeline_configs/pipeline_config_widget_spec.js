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

define([
  "jquery", "mithril", 'lodash', 'string-plus', "models/pipeline_configs/pipeline", "views/pipeline_configs/pipeline_config_widget"
], function ($, m, _, s, Pipeline, PipelineConfigWidget) {

  describe("PipelineConfigWidget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);
    var pipeline;

    beforeAll(function (done) {
      spyOn(Pipeline, 'find').and.callFake(function () {
        return {
          then: function (callback) {
            return callback(samplePipelineJSON());
          }
        };
      });

      // needed because the widget needs to fetch data via ajax, and complete rendering
      var reallyDone = _.after(2, function () {
        m.redraw(true);
        $root.find('.pipeline-settings>.accordion-item>a')[0].click();
        m.redraw(true);
        done();
      });

      var component = PipelineConfigWidget('/pipeline.json', function (controller) {
        pipeline = controller.pipeline();
        reallyDone();
      });

      m.mount(root, component);

      reallyDone();
    });

    afterAll(function () {
      m.mount(root, null);
      m.redraw(true);
    });

    function inputFieldFor(propName, modelType) {
      modelType = s.defaultToIfBlank(modelType, 'pipeline');
      return $root.find('.pipeline input[data-model-type=' + modelType + '][data-prop-name=' + propName + ']');
    }

    it("should render the pipeline name", function () {
      expect($root.find('.pipeline .heading h1')).toHaveText('Pipeline configuation for pipeline yourproject');
    });

    it("should render enablePipelineLocking checkbox", function () {
      expect(inputFieldFor('enablePipelineLocking')).toBeChecked();
      expect(pipeline.enablePipelineLocking()).toBe(true);
    });

    it("should render the pipeline scheduling type in the pipeline settings view", function() {
      expect($root.find('.pipeline-schedule')).toContainText('Automatically triggered');
    });

    it("should show tooltip message for automatic pipeline scheduling", function() {
      expect($root.find('.pipeline-schedule')).toContainText("This pipeline is automatically triggered as the first stage of this pipeline is set to 'success'.");
    });

    it("should toggle pipeline enablePipelineLocking attribute", function () {
      var lockedCheckBox = inputFieldFor('enablePipelineLocking').get(0);
      lockedCheckBox.click();
      expect(pipeline.enablePipelineLocking()).toBe(false);
    });

    it("should render value of timer", function () {
      expect(inputFieldFor('spec', 'pipelineTimer')).toHaveValue("0 0 22 ? * MON-FRI");
    });

    it("should set the value of labelTemplate", function () {
      var labelTextElem = inputFieldFor('labelTemplate');
      var value         = "some-label-text";
      labelTextElem.val(value);

      expect(labelTextElem).toHaveValue(value);
    });

    it("should render the params (when clicked)", function () {
      expect('.parameters .parameter').not.toBeInDOM();

      var accordion = $root.find('.parameters.accordion .accordion-item > a').get(0);

      var evObj = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);

      expect($root.find('.parameters .parameter')).toHaveLength(3);

      expect($root.find('.parameter').map(function () {
        return $(this).attr('data-parameter-name');
      })).toEqual(['COMMAND', 'WORKING_DIR']);

    });

    it("should render the environment variables", function () {
      expect('.environment-variables .environment-variable').not.toBeInDOM();

      var accordion = $root.find('.environment-variables.accordion .accordion-item > a').get(0);

      var evObj = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);

      expect($root.find('.environment-variables .environment-variable[data-variable-type=plain]')).toHaveLength(2);
      expect($root.find('.environment-variables .environment-variable[data-variable-type=secure]')).toHaveLength(2);

      expect($root.find('.environment-variable').map(function () {
        return $(this).attr('data-variable-name');
      })).toEqual(['USERNAME', 'PASSWORD']);
    });

    it("should not render the template name if pipeline is not built from template", function () {
      expect('input[name=template_name]').not.toBeInDOM();
    });
  });

  function samplePipelineJSON() {
    /* eslint-disable camelcase */
    return {
      name:                    "yourproject",
      label_template:          "foo-1.0.${COUNT}-${svn}",
      enable_pipeline_locking: true,
      template_name:           null,
      timer:                   {
        spec:            "0 0 22 ? * MON-FRI",
        only_on_changes: true
      },
      parameters:              [
        {
          name:  "COMMAND",
          value: "echo"
        },
        {
          name:  "WORKING_DIR",
          value: "/repo/branch"
        }
      ],
      environment_variables:   [
        {
          name:   "USERNAME",
          value:  "bob",
          secure: false
        },
        {
          name:           "PASSWORD",
          encryptedValue: "c!ph3rt3xt",
          secure:         true
        }
      ],
      stages:                  [
        {
          name: 'BuildLinux'
        }
      ]
    };
    /* eslint-enable camelcase */
  }
});
