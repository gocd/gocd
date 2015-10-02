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

define(["jquery", "mithril", 'lodash', 'string-plus', "pipeline_configs/models/pipeline", "pipeline_configs/views/pipeline_config_widget"], function ($, m, _, s, Pipeline, PipelineConfigWidget) {
  describe("PipelineConfigWidget", function () {
    var root, $root;
    var pipeline;

    beforeEach(function (done) {
      root  = document.createElement("div");
      document.body.appendChild(root);
      $root = $(root);

      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest('/pipeline.json').andReturn({
        contentType:  'application/json',
        responseText: JSON.stringify(samplePipelineJSON())
      });

      // needed because the widget needs to fetch data via ajax, and complete rendering
      var reallyDone = _.after(2, function(){
        m.redraw(true);
        done();
      });

      var component = PipelineConfigWidget('/pipeline.json', function (controller) {
        pipeline = controller.pipeline;
        reallyDone();
      });

      m.mount($root.get(0), component);
      reallyDone();
    });

    afterEach(function () {
      root.parentNode.removeChild(root);
    });

    afterEach(function () {
      jasmine.Ajax.uninstall();
    });

    function inputFieldFor(propName, modelType) {
      modelType = s.defaultToIfBlank(modelType, 'pipeline');
      return $root.find('.pipeline input[data-model-type=' + modelType + '][data-prop-name=' + propName + ']');
    }

    it("should render the pipeline name", function () {
      expect($root.find('.pipeline .heading')).toHaveText('Pipeline Details for yourproject');
    });


    it("should render enablePipelineLocking checkbox", function () {
      expect(inputFieldFor('enablePipelineLocking')).toBeChecked();
      expect(pipeline.enablePipelineLocking()).toBe(true);
    });

    it("should toggle pipeline enablePipelineLocking attribute", function () {
      var lockedCheckBox = inputFieldFor('enablePipelineLocking').get(0);
      lockedCheckBox.click();
      expect(pipeline.enablePipelineLocking()).toBe(false);
    });

    it("should render value of timer", function () {
      expect(inputFieldFor('spec', 'pipelineTimer').val()).toEqual("0 0 22 ? * MON-FRI");
    });

    it("should set the value of labelTemplate", function () {
      var labelTextElem = inputFieldFor('labelTemplate');
      var value         = "some-label-text";
      labelTextElem.val(value);

      expect(labelTextElem.val()).toEqual(value);
    });

    it("should render the params (when clicked)", function () {
      expect($root.find('.parameters .parameter').length).toBe(0);

      var accordion = $root.find('.parameters.accordion .accordion-navigation > a').get(0);

      var evObj     = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);

      expect($root.find('.parameters .parameter').length).toBe(3);

      expect($root.find('.parameter').map(function () {
        return $(this).attr('data-parameter-name');
      })).toEqual(['COMMAND', 'WORKING_DIR']);

    });

    it("should render the environment variables", function () {
      expect($root.find('.environment-variables .environment-variable').length).toBe(0);

      var accordion = $root.find('.environment-variables.accordion .accordion-navigation > a').get(0);

      var evObj = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);

      expect($root.find('.environment-variables .environment-variable').length).toBe(3);

      expect($root.find('.environment-variable').map(function () {
        return $(this).attr('data-variable-name');
      })).toEqual(['MULTIPLE_LINES', 'COMPLEX']);
    });

    it("should not render the template name if pipeline is not built from template", function () {
      expect($root.find('input[name=template_name]').length).toBe(0);
    });
  });

  function samplePipelineJSON() {
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
          name:   "MULTIPLE_LINES",
          value:  "multiplelines",
          secure: true
        },
        {
          name:   "COMPLEX",
          value:  "This has very <complex> data",
          secure: false
        }
      ],
      stages:                  [
        {
          name: 'BuildLinux'
        }
      ]
    };
  }
});
