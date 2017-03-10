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

describe("Read Only Template Widget", function () {
  var $      = require("jquery");
  var m      = require("mithril");
  var Stream = require("mithril/stream");
  require('jasmine-jquery');
  require('jasmine-ajax');

  var TemplateWidget = require("views/pipeline_configs/read_only_template_widget");
  var Template       = require("models/pipeline_configs/template");

  var root, template;
  beforeEach(() => {
    [, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(function () {
    template = Template.fromJSON(rawTemplateJSON());
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest('/go/api/admin/templates/' + template.name(), null, 'GET').andReturn({
      responseText: JSON.stringify(rawTemplateJSON()),
      headers:      {
        'Content-Type': 'application/json'
      },
      status:       200
    });
    mount();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    unmount();
  });

  it('should render template name', function () {
    expect($('.accordion-content.show')).toContainText('name:');
    expect($('.accordion-content.show')).toContainText(template.name());
  });

  it('should render template stages', function () {
    expect($('.stages')).toBeInDOM();
  });

  var mount = function () {
    m.mount(root, {
      view: function () {
        return m(TemplateWidget, {templateName: template.name(), currentSelection: Stream()});
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
