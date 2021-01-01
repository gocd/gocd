/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {DashboardGroups} from "models/dashboard/dashboard_groups";
import {Dashboard} from "models/dashboard/dashboard";
import {Pipelines} from "models/dashboard/pipelines";
import _ from "lodash";

describe("Dashboard", () => {

  describe('Dashboard Model', () => {
    const originalDebounce = _.debounce;

    let dashboard;
    beforeEach(() => {
      dashboard = new Dashboard();
      dashboard.initialize(dashboardData);
      dashboard._performRouting = _.noop;
    });

    beforeAll(() => {
      spyOn(_, 'debounce').and.callFake((func) => {
        return function () {
          func.apply(this, arguments);
        };
      });
    });

    afterAll(() => {
      _.debounce = originalDebounce;
    });

    it("should get pipeline groups", () => {
      const expectedPipelineGroups = new DashboardGroups(dashboardData._embedded.pipeline_groups).groups;
      const actualPipelineGroups   = dashboard.getPipelineGroups().groups;

      expect(actualPipelineGroups.length).toEqual(expectedPipelineGroups.length);
    });

    it("should get pipelines", () => {
      const expectedPipelines = new Pipelines(dashboardData._embedded.pipelines);
      const actualPipelines   = dashboard.getPipelines();

      expect(actualPipelines.length).toEqual(expectedPipelines.length);
    });

    it("should find the pipeline by pipeline name", () => {
      const pipelineName     = "up42";
      const expectedPipeline = Pipelines.fromJSON(dashboardData._embedded.pipelines).find(pipelineName);
      const actualPipeline   = dashboard.findPipeline(pipelineName);

      expect(actualPipeline.name).toEqual(expectedPipeline.name);
    });

    it('should get new dashboard json', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/dashboard?allowEmpty=false', undefined, 'GET').andReturn({
          responseText:    JSON.stringify(dashboardData),
          responseHeaders: {
            ETag:           'etag',
            'Content-Type': 'application/vnd.go.cd.v4+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((dashboard) => {
          const expected = new Dashboard();
          expected.initialize(dashboard);
          expect(expected.getPipelineGroups().groups.length).toBe(1);
        });

        Dashboard.get().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toMatch(/^\/go\/api\/dashboard(?:\?|&|$)/);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v4+json');
      });
    });

    const dashboardData = {
      "_embedded": {
        "pipeline_groups": [
          {
            "_links":    {
              "self": {
                "href": "http://localhost:8153/go/api/config/pipeline_groups/first"
              },
              "doc":  {
                "href": "https://api.go.cd/current/#pipeline-groups"
              }
            },
            "name":      "first",
            "pipelines": ["up42"]
          }
        ],
        "pipelines":       [
          {
            "_links":                 {
              "self": {
                "href": "http://localhost:8153/go/api/pipelines/up42/history"
              },
              "doc":  {
                "href": "https://api.go.cd/current/#pipelines"
              }
            },
            "name":                   "up42",
            "last_updated_timestamp": 1510299695473,
            "locked":                 false,
            "template_info": {
              "is_using_template": false,
              "template_name":     null
            },
            "pause_info":             {
              "paused":       false,
              "paused_by":    null,
              "pause_reason": null
            },
            "_embedded":              {
              "instances": [
                {
                  "_links":       {
                    "self": {
                      "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
                    },
                    "doc":  {
                      "href": "https://api.go.cd/current/#get-pipeline-instance"
                    }
                  },
                  "label":        "1",
                  "counter":      "1",
                  "scheduled_at": "2017-11-10T07:25:28.539Z",
                  "triggered_by": "changes",
                  "_embedded":    {
                    "stages": [
                      {
                        "_links":       {
                          "self": {
                            "href": "http://localhost:8153/go/api/stages/up42/1/up42_stage/1"
                          },
                          "doc":  {
                            "href": "https://api.go.cd/current/#get-stage-instance"
                          }
                        },
                        "name":         "up42_stage",
                        "counter":      "1",
                        "status":       "Failed",
                        "approved_by":  "changes",
                        "scheduled_at": "2017-11-10T07:25:28.539Z"
                      }
                    ]
                  }
                }
              ]
            }
          }
        ]
      }
    };
  });
});
