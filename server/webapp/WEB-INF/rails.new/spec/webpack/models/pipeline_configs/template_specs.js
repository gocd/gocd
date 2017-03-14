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

const Template = require("models/pipeline_configs/template");

describe("Template Model", () => {
  let template;
  beforeEach(() => {
    template = Template.fromJSON(sampleTemplateJSON());
  });

  it("should initialize template model with name", () => {
    expect(template.name()).toBe("scratch");
  });

  it("should initialize template model with stages", () => {
    expect(template.stages().countStage()).toBe(1);
    expect(template.stages().firstStage().name()).toBe('up42_stage');
  });

  describe("De-serialization from JSON", () => {
    it("should de-serialize from JSON", () => {
      const template = Template.fromJSON(sampleTemplateJSON());
      expect(template.name()).toBe("scratch");
      expect(template.stages().countStage()).toBe(1);
      expect(template.stages().firstStage().name()).toBe('up42_stage');
    });
  });

  describe('find', () => {
    it('should fetch template using template name', () => {
      jasmine.Ajax.withMock(() => {
        const url = `/go/api/admin/templates/${  template.name()}`;

        jasmine.Ajax.stubRequest(url, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(sampleTemplateJSON()),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v3+json'
          }
        });

        const successCallback = jasmine.createSpy().and.callFake((template) => {
          expect(template.name()).toBe(sampleTemplateJSON().name);
        });

        Template.find(template.name()).then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });
  });

  function sampleTemplateJSON() {
    /* eslint-disable camelcase */
    return {
      "name":   "scratch",
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
    /* eslint-enable camelcase */
  }
});
