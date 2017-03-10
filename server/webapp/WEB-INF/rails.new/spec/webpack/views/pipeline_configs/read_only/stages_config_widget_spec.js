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

describe("Read Only Stages Widget", function () {
  var $             = require("jquery");
  var m             = require("mithril");
  var simulateEvent = require('simulate-event');
  require('jasmine-jquery');
  require('jasmine-ajax');

  var StagesConfigWidget = require("views/pipeline_configs/read_only/stages_config_widget");
  var Template           = require("models/pipeline_configs/template");

  var $root, root, template;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(function () {
    template = Template.fromJSON(rawTemplateJSON());
    mount();
  });

  afterEach(() => {
    unmount();
  });

  it('should render stage boxes for all the stages', function () {
    var stageBoxes = $('.stages-wrapper>.stage');
    expect(stageBoxes.length).toBe(2);
    expect($(stageBoxes[0]).text()).toBe(rawTemplateJSON().stages[0].name);
    expect($(stageBoxes[1]).text()).toBe(rawTemplateJSON().stages[1].name);
  });

  it('should render the stage details for selected stage box', function () {
    var selectedStageName = $('.stages-wrapper>.stage.active').text();
    var renderedStageName = $('.stage-definition').attr('data-stage-name');

    expect(selectedStageName).toBe(renderedStageName);
  });

  it('should select the first stage by default', function () {
    var selectedStageName = $('.stages-wrapper>.stage.active').text();

    expect(selectedStageName).toBe(rawTemplateJSON().stages[0].name);
  });

  it('should render the related stage details when another stage is selected', function () {
    var selectedStage = () => $('.stages-wrapper>.stage.active');
    var renderedStage = () => $('.stage-definition').attr('data-stage-name');

    expect(selectedStage().text()).toBe(rawTemplateJSON().stages[0].name);
    expect(renderedStage()).toBe(rawTemplateJSON().stages[0].name);

    simulateEvent.simulate($root.find('.stages-wrapper>.stage').get(1), 'click');

    expect(selectedStage().text()).toBe(rawTemplateJSON().stages[1].name);
    expect(renderedStage()).toBe(rawTemplateJSON().stages[1].name);
  });

  var mount = function () {
    m.mount(root, {
      view: function () {
        return m(StagesConfigWidget, {stages: template.stages});
      }
    });
    m.redraw();
  };

  var unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  var rawTemplateJSON = function () {
    return {
      "name":   "template.name",
      "stages": [
        {
          "name":                    "up42_stage",
          "fetch_materials":         true,
          "clean_working_directory": false,
          "never_cleanup_artifacts": false,
          "approval":                {
            "type":          "success",
            "authorization": {
              "roles": [],
              "users": []
            }
          },
          "environment_variables":   [],
          "jobs":                    [
            {
              "name":                  "up42_job",
              "run_instance_count":    null,
              "timeout":               "never",
              "elastic_profile_id":    "docker",
              "environment_variables": [],
              "resources":             [],
              "tasks":                 [
                {
                  "type":       "exec",
                  "attributes": {
                    "run_if":            [],
                    "on_cancel":         null,
                    "command":           "ls",
                    "working_directory": null
                  }
                }
              ],
              "tabs":                  [],
              "artifacts":             [],
              "properties":            null
            }
          ]
        },
        {
          "name":                    "up42_stage_2",
          "fetch_materials":         true,
          "clean_working_directory": false,
          "never_cleanup_artifacts": false,
          "approval":                {
            "type":          "success",
            "authorization": {
              "roles": [],
              "users": []
            }
          },
          "environment_variables":   [],
          "jobs":                    [
            {
              "name":                  "up42_job",
              "run_instance_count":    null,
              "timeout":               "never",
              "elastic_profile_id":    "docker",
              "environment_variables": [],
              "resources":             [],
              "tasks":                 [
                {
                  "type":       "exec",
                  "attributes": {
                    "run_if":            [],
                    "on_cancel":         null,
                    "command":           "ls",
                    "working_directory": null
                  }
                }
              ],
              "tabs":                  [],
              "artifacts":             [],
              "properties":            null
            }
          ]
        }
      ]
    };
  };
});
