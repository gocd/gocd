/*
 * Copyright 2018 ThoughtWorks, Inc.
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


describe("StagesConfigWidget", () => {
  const m      = require('mithril');
  const Stream = require('mithril/stream');
  const $             = require('jquery');

  require('jasmine-jquery');

  const Pipeline           = require("models/pipeline_configs/pipeline");
  const StagesConfigWidget = require("views/pipeline_configs/stages_config_widget");

  let $root, root;
  let pipeline;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(() => {
    pipeline         = Stream(Pipeline.fromJSON(samplePipelineJSON()));
    mount(pipeline);
  });

  function mount(pipeline) {
    const currentSelection = Stream(pipeline().stages().firstStage());
    const pluginInfos      = jasmine.createSpy();
    const elasticProfiles  = jasmine.createSpy();
    const vm               = jasmine.createSpy();

    m.mount(root, {
      view() {
        return m(StagesConfigWidget, {
          pipeline,
          pluginInfos,
          elasticProfiles,
          currentSelection,
          vm
        });
      }
    });
    m.redraw();
  }

  afterEach(() => {
    unmount();
  });

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  it("should show delete button as enabled if more than one stages are present", () => {
    expect(pipeline().stages().countStage()).toBe(2);
    const removeStageButton = $root.find('.remove-stage');
    expect(removeStageButton).toHaveClass("remove");
  });

  it("should show delete button as disabled if only single stage is present", () => {
    unmount();
    pipeline().stages().removeStage(pipeline().stages().firstStage());
    mount(pipeline);
    expect(pipeline().stages().countStage()).toBe(1);
    const removeStageButton = $root.find('.remove-stage');
    expect(removeStageButton).toHaveClass("remove-disabled");
    const tooltipId = $(removeStageButton).attr('data-tooltip-id');
    expect($(`#${tooltipId}`)).toHaveText("Cannot delete the only stage in a pipeline");
  });

  function samplePipelineJSON() {
    /* eslint-disable camelcase */
    return {
      name:                  "yourproject",
      label_template:        "foo-1.0.${COUNT}-${svn}",
      lock_behavior:         "none",
      template_name:         null,
      timer:                 {
        spec:            "0 0 22 ? * MON-FRI",
        only_on_changes: true
      },
      parameters:            [
        {
          name:  "COMMAND",
          value: "echo"
        },
        {
          name:  "WORKING_DIR",
          value: "/repo/branch"
        }
      ],
      environment_variables: [
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
      stages:                [
        {
          name: 'BuildLinux_1'
        },
        {
          name: 'BuildLinux_2'
        }
      ]
    };
    /* eslint-enable camelcase */
  }
});
