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

describe("Read Only Stage Settings Widget", function () {
  var $ = require("jquery");
  var m = require("mithril");
  require('jasmine-jquery');

  var StagesSettingsWidget = require("views/pipeline_configs/read_only/stage_settings_widget");
  var Template             = require("models/pipeline_configs/template");

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

  it('should render stage setting heading', function () {
    expect($('h5')).toContainText('Stage Settings:');
  });

  it('should render stage name', function () {
    expect($root).toContainText('Stage name:');
    expect($root).toContainText(rawTemplateJSON().stages[0].name);
  });

  it('should render stage type', function () {
    expect($root).toContainText('Stage type:');
    expect($root).toContainText(rawTemplateJSON().stages[0].approval.type);
  });

  it('should render fetch material checkbox icon', function () {
    var checklist     = $($('.columns.medium-4.large-5.end')).children();
    var fetchMaterial = checklist[0];
    expect($(fetchMaterial)).toContainText('Fetch materials');
    expect($(fetchMaterial)).toHaveClass('checkbox-selected-icon');
  });

  it('should render never cleanup artifacts checkbox icon', function () {
    var checklist             = $($('.columns.medium-4.large-5.end')).children();
    var neverCleanupArtifacts = checklist[1];
    expect($(neverCleanupArtifacts)).toContainText('Never cleanup artifacts');
    expect($(neverCleanupArtifacts)).toHaveClass('checkbox-unselected-icon');
  });

  it('should render delete working directory on every build checkbox icon', function () {
    var checklist             = $($('.columns.medium-4.large-5.end')).children();
    var neverCleanupArtifacts = checklist[2];
    expect($(neverCleanupArtifacts)).toContainText('Delete working directory on every build');
    expect($(neverCleanupArtifacts)).toHaveClass('checkbox-unselected-icon');
  });

  var mount = function () {
    m.mount(root, {
      view: function () {
        return m(StagesSettingsWidget, {stage: template.stages().firstStage});
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
        }
      ]
    };
  };
});
