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

describe("Dashboard", () => {
  describe('Dashboard Model', () => {
    const Dashboard      = require('models/dashboard/dashboard');
    const PipelineGroups = require('models/dashboard/pipeline_groups');
    const Pipelines      = require('models/dashboard/pipelines');

    it("should get pipeline groups", () => {
      const dashboard = Dashboard.fromJSON(dashboardData);

      const expectedPipelineGroups = new PipelineGroups(dashboardData._embedded.pipeline_groups).groups;
      const actualPipelineGroups   = dashboard.getPipelineGroups();

      expect(actualPipelineGroups.length).toEqual(expectedPipelineGroups.length);
    });

    it("should get pipelines", () => {
      const dashboard = Dashboard.fromJSON(dashboardData);

      const expectedPipelines = new Pipelines(dashboardData._embedded.pipelines);
      const actualPipelines   = dashboard.getPipelines();

      expect(actualPipelines.length).toEqual(expectedPipelines.length);
    });

    it("should find the pipeline by pipeline name", () => {
      const dashboard = Dashboard.fromJSON(dashboardData);

      const pipelineName     = "up42";
      const expectedPipeline = Pipelines.fromJSON(dashboardData._embedded.pipelines).find(pipelineName);
      const actualPipeline   = dashboard.findPipeline(pipelineName);

      expect(actualPipeline.name).toEqual(expectedPipeline.name);
    });

    it("it should filter dashboard provided filter text", () => {
      const dashboard = Dashboard.fromJSON(dashboardData);

      expect(dashboard.allPipelineNames()).toEqual(['up42']);

      expect(dashboard.filterBy("up").allPipelineNames()).toEqual(['up42']);
      expect(dashboard.filterBy("42").allPipelineNames()).toEqual(['up42']);
      expect(dashboard.filterBy("up42").allPipelineNames()).toEqual(['up42']);
    });

    it('should get new dashboard json', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/dashboard', undefined, 'GET').andReturn({
          responseText:    JSON.stringify(dashboardData),
          responseHeaders: {
            ETag:           'etag',
            'Content-Type': 'application/vnd.go.cd.v2+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((dashboard) => {
          expect(Dashboard.fromJSON(dashboard).getPipelineGroups().length).toBe(1);
        });

        Dashboard.get().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe('/go/api/dashboard');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v2+json');
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
              "self":                 {
                "href": "http://localhost:8153/go/api/pipelines/up42/history"
              },
              "doc":                  {
                "href": "https://api.go.cd/current/#pipelines"
              },
              "settings_path":        {
                "href": "http://localhost:8153/go/admin/pipelines/up42/general"
              },
              "trigger":              {
                "href": "http://localhost:8153/go/api/pipelines/up42/schedule"
              },
              "trigger_with_options": {
                "href": "http://localhost:8153/go/api/pipelines/up42/schedule"
              },
              "pause":                {
                "href": "http://localhost:8153/go/api/pipelines/up42/pause"
              },
              "unpause":              {
                "href": "http://localhost:8153/go/api/pipelines/up42/unpause"
              }
            },
            "name":                   "up42",
            "last_updated_timestamp": 1510299695473,
            "locked":                 false,
            "pause_info":             {
              "paused":       false,
              "paused_by":    null,
              "pause_reason": null
            },
            "_embedded":              {
              "instances": [
                {
                  "_links":       {
                    "self":            {
                      "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
                    },
                    "doc":             {
                      "href": "https://api.go.cd/current/#get-pipeline-instance"
                    },
                    "history_url":     {
                      "href": "http://localhost:8153/go/api/pipelines/up42/history"
                    },
                    "vsm_url":         {
                      "href": "http://localhost:8153/go/pipelines/value_stream_map/up42/1"
                    },
                    "compare_url":     {
                      "href": "http://localhost:8153/go/compare/up42/0/with/1"
                    },
                    "build_cause_url": {
                      "href": "http://localhost:8153/go/pipelines/up42/1/build_cause"
                    }
                  },
                  "label":        "1",
                  "counter":      "1",
                  "scheduled_at": "2017-11-10T07:25:28.539Z",
                  "triggered_by": "changes",
                  "build_cause":  {
                    "approver":           "",
                    "is_forced":          false,
                    "trigger_message":    "modified by GoCD Test User <devnull@example.com>",
                    "material_revisions": [
                      {
                        "material_type": "Git",
                        "material_name": "test-repo",
                        "changed":       true,
                        "modifications": [
                          {
                            "_links":        {
                              "vsm": {
                                "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
                              }
                            },
                            "user_name":     "GoCD Test User <devnull@example.com>",
                            "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
                            "modified_time": "2017-12-19T05:30:32.000Z",
                            "comment":       "Initial commit"
                          }
                        ]
                      }
                    ]
                  },
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
