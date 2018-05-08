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
    const _ = require('lodash');

    const Dashboard        = require('models/dashboard/dashboard');
    const PipelineGroups   = require('models/dashboard/pipeline_groups');
    const Pipelines        = require('models/dashboard/pipelines');
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
      const expectedPipelineGroups = new PipelineGroups(dashboardData._embedded.pipeline_groups).groups;
      const actualPipelineGroups   = dashboard.getPipelineGroups();

      expect(actualPipelineGroups.length).toEqual(expectedPipelineGroups.length);
    });

    it("it should filter dashboard provided pipeline name as filter text", () => {
      const pipelineName     = "up42";
      expect(dashboard.getPipelineGroups()[0].pipelines[0].name).toEqual(pipelineName);
      dashboard.searchText("up");
      expect(dashboard.getPipelineGroups()[0].pipelines[0].name).toEqual(pipelineName);
      dashboard.searchText("42");
      expect(dashboard.getPipelineGroups()[0].pipelines[0].name).toEqual(pipelineName);
      dashboard.searchText("up42");
      expect(dashboard.getPipelineGroups()[0].pipelines[0].name).toEqual(pipelineName);
      dashboard.searchText("up42-some-more");
      expect(dashboard.getPipelineGroups()).toEqual([]);
    });

    it("it should filter dashboard provided pipeline group name as filter text", () => {
      const pipelineGroupName     = "first";
      expect(dashboard.getPipelineGroups()[0].name).toEqual(pipelineGroupName);
      dashboard.searchText("fi");
      expect(dashboard.getPipelineGroups()[0].name).toEqual(pipelineGroupName);
      dashboard.searchText("fir");
      expect(dashboard.getPipelineGroups()[0].name).toEqual(pipelineGroupName);
      dashboard.searchText("first");
      expect(dashboard.getPipelineGroups()[0].name).toEqual(pipelineGroupName);
      dashboard.searchText("first-some-more");
      expect(dashboard.getPipelineGroups()).toEqual([]);
    });

    it("it should filter dashboard provided pipeline status as filter text", () => {
      const pipelineStatus     = "Failed";
      expect(dashboard.getPipelineGroups()[0].pipelines[0].instances[0].stages[0].status).toEqual(pipelineStatus);
      dashboard.searchText("fai");
      expect(dashboard.getPipelineGroups()[0].pipelines[0].instances[0].stages[0].status).toEqual(pipelineStatus);
      dashboard.searchText("failed");
      expect(dashboard.getPipelineGroups()[0].pipelines[0].instances[0].stages[0].status).toEqual(pipelineStatus);
      dashboard.searchText("building");
      expect(dashboard.getPipelineGroups().length).toEqual(0);
    });

    it("should peform routing when filter text is updated", () => {
      const performSpy = spyOn(dashboard, '_performRouting');

      expect(performSpy).not.toHaveBeenCalled();

      dashboard.searchText("up");

      expect(performSpy).toHaveBeenCalled();
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
          const expected = new Dashboard();
          expected.initialize(dashboard);
          expect(expected.getPipelineGroups().length).toBe(1);
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
