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

describe("Read Only Stage Settings Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");
  require('jasmine-jquery');

  const StagesSettingsWidget = require("views/pipeline_configs/read_only/stage_settings_widget");
  const Template             = require("models/pipeline_configs/template");

  let $root, root, template;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(() => {
    template = Template.fromJSON(rawTemplateJSON());
    mount();
  });

  afterEach(() => {
    unmount();
  });

  it('should render stage setting heading', () => {
    expect($('h5')).toContainText('Stage Settings');
  });

  it('should render stage name', () => {
    expect($root).toContainText('Stage name');
    expect($root).toContainText(rawTemplateJSON().stages[0].name);
  });

  it('should render stage type', () => {
    expect($root).toContainText('Stage type');
    expect($root).toContainText(rawTemplateJSON().stages[0].approval.type);
  });

  it('should render fetch material checkbox icon', () => {
    expect($('.checkbox-selected-icon')).toContainText('Fetch materials');
  });

  it('should render never cleanup artifacts checkbox icon', () => {
    expect($($('.checkbox-unselected-icon')[0])).toContainText('Never cleanup artifacts');
  });

  it('should render delete working directory on every build checkbox icon', () => {
    expect($($('.checkbox-unselected-icon')[1])).toContainText('Delete working directory on every build');
  });

  const mount = function () {
    m.mount(root, {
      view () {
        return m(StagesSettingsWidget, {stage: template.stages().firstStage});
      }
    });
    m.redraw();
  };

  const unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  const rawTemplateJSON = function () {
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
