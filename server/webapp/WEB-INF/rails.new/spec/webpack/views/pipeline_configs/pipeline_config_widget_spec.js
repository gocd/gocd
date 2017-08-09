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

describe("PipelineConfigWidget", () => {
  const $             = require("jquery");
  const m             = require('mithril');
  const Stream        = require('mithril/stream');
  const _             = require('lodash');
  const s             = require('string-plus');
  const simulateEvent = require('simulate-event');
  const Modal         = require('views/shared/new_modal');

  require('jasmine-jquery');

  const Pipeline             = require("models/pipeline_configs/pipeline");
  const PipelineConfigWidget = require("views/pipeline_configs/pipeline_config_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  let pipeline;

  Pipeline.find = (_url, extract) => {
    extract(samplePipelineJSON(), "success", {
      status:            200,
      getResponseHeader: Stream('etag')
    });
  };


  beforeEach((done) => {
    // needed because the widget needs to fetch data via ajax, and complete rendering
    const reallyDone = _.after(2, () => {
      $root.find('.pipeline-settings>.accordion-item>a')[0].click();
      m.redraw();
      done();
    });

    const component = PipelineConfigWidget({
      url:       Stream('/pipeline.json'), callback(controller) {
        pipeline = controller.pipeline();
        reallyDone();
      },
      pipelines: Stream([])
    });

    m.mount(root, {
      view() {
        return m(component);
      }
    });

    reallyDone();
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
    Modal.destroyAll();
  });

  function inputFieldFor(propName, modelType) {
    modelType = s.defaultToIfBlank(modelType, 'pipeline');
    return $root.find(`.pipeline input[data-model-type=${modelType}][data-prop-name=${propName}]`);
  }

  it("should render normal edit button", () => {
    expect($root.find("a.toggle-old-view")).toContainText("Normal Edit");
  });

  it("should show the proceed confirm modal when clicked on normal edit link", () => {
    simulateEvent.simulate($root.find("a.toggle-old-view").get(0), 'click');
    m.redraw();

    expect($(".modal-title")).toContainText("Unsaved Changes");
    expect($(".modal-content > p")).toContainText("'Proceed' will discard any unsaved data.");

    expect($(".actions > a.secondary")).toContainText("Cancel");
    expect($(".actions > a.primary")).toContainText("Proceed");
  });

  it("should contain the appropriate href link on proceed button", () => {
    simulateEvent.simulate($root.find("a.toggle-old-view").get(0), 'click');
    m.redraw();

    const proceedButton = $(".actions > a.primary");
    expect(proceedButton.attr('href')).toBe('/go/admin/pipelines/yourproject/general');
  });

  it("should render the pipeline name", () => {
    expect($root.find('.pipeline .heading h1')).toHaveText('Pipeline configuration for pipeline yourproject');
  });

  it('should disable button and page edits while pipeline config save is in progress', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest('/go/api/admin/pipelines/yourproject', undefined, 'PUT');

      const saveButton   = $root.find('button.save-pipeline');
      const pipelineBody = $root.find('.pipeline-body');

      expect($(saveButton)).not.toHaveClass('in-progress');
      expect($(pipelineBody)).not.toHaveClass('page-save-in-progress');

      $(saveButton).trigger('click');
      m.redraw(true);

      expect($(saveButton)).toHaveClass('in-progress');
      expect($(saveButton)).toHaveClass('disabled');
      expect($(pipelineBody)).toHaveClass('page-save-in-progress');
    });
  });

  it("should render enablePipelineLocking checkbox", () => {
    expect(inputFieldFor('enablePipelineLocking')).toBeChecked();
    expect(pipeline.enablePipelineLocking()).toBe(true);
  });

  it("should render the pipeline scheduling type in the pipeline settings view", () => {
    expect($root.find('.pipeline-schedule')).toContainText('Automatically triggered');
  });

  it("should show tooltip message for automatic pipeline scheduling", () => {
    expect($root.find('.pipeline-schedule')).toContainText("This pipeline is automatically triggered as the first stage of this pipeline is set to 'success'.");
  });

  it("should toggle pipeline enablePipelineLocking attribute", () => {
    const lockedCheckBox = inputFieldFor('enablePipelineLocking').get(0);
    lockedCheckBox.click();
    expect(pipeline.enablePipelineLocking()).toBe(false);
  });

  it("should render value of timer", () => {
    expect(inputFieldFor('spec', 'pipelineTimer')).toHaveValue("0 0 22 ? * MON-FRI");
  });

  it("should set the value of labelTemplate", () => {
    const labelTextElem = inputFieldFor('labelTemplate');
    const value         = "some-label-text";
    labelTextElem.val(value);

    expect(labelTextElem).toHaveValue(value);
  });

  it("should render the params (when clicked)", () => {
    expect('.parameters .parameter').not.toBeInDOM();
    simulateEvent.simulate($root.find('.parameters.accordion .accordion-item > a').get(0), 'click');
    m.redraw();

    expect($root.find('.parameters .parameter')).toHaveLength(3);

    expect($root.find('.parameter').map(function () {
      return $(this).attr('data-parameter-name');
    })).toEqual(['COMMAND', 'WORKING_DIR']);

  });

  it("should render the environment variables", () => {
    expect('.environment-variables .environment-variable').not.toBeInDOM();

    simulateEvent.simulate($root.find('.environment-variables.accordion .accordion-item > a').get(0), 'click');

    m.redraw();

    expect($root.find('.environment-variables .environment-variable[data-variable-type=plain]')).toHaveLength(2);
    expect($root.find('.environment-variables .environment-variable[data-variable-type=secure]')).toHaveLength(2);

    expect($root.find('.environment-variable').map(function () {
      return $(this).attr('data-variable-name');
    })).toEqual(['USERNAME', 'PASSWORD']);
  });

  it("should not render the template name if pipeline is not built from template", () => {
    expect('input[name=template_name]').not.toBeInDOM();
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
