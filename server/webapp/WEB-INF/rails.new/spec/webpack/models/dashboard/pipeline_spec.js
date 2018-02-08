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
  const Pipeline = require('models/dashboard/pipeline');
  require('jasmine-jquery');

  let pipelineJson;
  beforeEach(() => {
    const defaultPipelineJson = {
      "paused":       true,
      "paused_by":    "admin",
      "pause_reason": "under construction"
    };
    pipelineJson              = pipelineJsonFor(defaultPipelineJson);
  });
  describe('Pipeline Model', () => {

    it("should deserialize from json", () => {
      const pipeline = new Pipeline(pipelineJson);

      expect(pipeline.name).toBe(pipelineJson.name);

      expect(pipeline.canAdminister).toBe(true);
      expect(pipeline.settingsPath).toBe(`/go/admin/pipelines/${pipelineJson.name}/general`);
      expect(pipeline.quickEditPath).toBe(`/go/admin/pipelines/${pipelineJson.name}/edit`);

      expect(pipeline.historyPath).toBe(`/go/tab/pipeline/history/${pipelineJson.name}`);
      expect(pipeline.instances.length).toEqual(pipelineJson._embedded.instances.length);

      expect(pipeline.isPaused).toBe(true);
      expect(pipeline.pausedBy).toBe("admin");
      expect(pipeline.pausedCause).toBe("under construction");
      expect(pipeline.canPause).toBe(true);

      expect(pipeline.canOperate).toBe(true);

      expect(pipeline.isLocked).toBe(false);
      expect(pipeline.canUnlock).toBe(true);
    });
  });

  describe("Pipeline Operations", () => {
    describe("Unpause", () => {
      it('should unpause a pipeline with appropriate headers', () => {
        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipelineJson.name}/unpause`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": `Pipeline '${pipelineJson.name}' paused successfully.`}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          const successCallback = jasmine.createSpy();

          const pipeline = new Pipeline(pipelineJson);
          pipeline.unpause().then(successCallback);

          expect(successCallback).toHaveBeenCalled();

          const request = jasmine.Ajax.requests.mostRecent();
          expect(request.method).toBe('POST');
          expect(request.url).toBe(`/go/api/pipelines/${pipelineJson.name}/unpause`);
          expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          expect(request.requestHeaders['X-GoCD-Confirm']).toContain('true');
        });
      });
    });

    describe("Pause", () => {
      beforeEach(() => {
        const pauseInfo = {
          "paused":       false,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        };
        pipelineJson    = pipelineJsonFor(pauseInfo);
      });

      it('should pause a pipeline with appropriate headers', () => {
        jasmine.Ajax.withMock(() => {
          const postData = {"pauseCause": "test"};
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipelineJson.name}/pause`, JSON.stringify(postData), 'POST').andReturn({
            responseText:    JSON.stringify({}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          const successCallback = jasmine.createSpy();

          const pipeline = new Pipeline(pipelineJson);
          pipeline.pause(postData).then(successCallback);

          expect(successCallback).toHaveBeenCalled();
          const request = jasmine.Ajax.requests.mostRecent();
          expect(request.method).toBe('POST');
          expect(request.url).toBe(`/go/api/pipelines/${pipelineJson.name}/pause`);
          expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          expect(request.requestHeaders['X-GoCD-Confirm']).toContain('true');
        });
      });
    });

    describe("Unlock", () => {
      it('should unlock a pipeline with appropriate headers', () => {
        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipelineJson.name}/unlock`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": `Pipeline '${pipelineJson.name}' unlocked successfully.`}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          const successCallback = jasmine.createSpy();

          const pipeline = new Pipeline(pipelineJson);
          pipeline.unlock().then(successCallback);

          expect(successCallback).toHaveBeenCalled();

          const request = jasmine.Ajax.requests.mostRecent();
          expect(request.method).toBe('POST');
          expect(request.url).toBe(`/go/api/pipelines/${pipelineJson.name}/unlock`);
          expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          expect(request.requestHeaders['X-GoCD-Confirm']).toContain('true');
        });
      });
    });

    describe("Trigger With Options View", () => {
      it('should fetch information for triggering a pipeline with options', () => {
        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest(`/go/api/internal/trigger_with_options_view/${pipelineJson.name}`, undefined, 'GET').andReturn({
            responseText:    JSON.stringify(triggerWithOptionsViewJson),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          const successCallback = jasmine.createSpy().and.callFake((info) => {
            expect(info.materials.length).toBe(triggerWithOptionsViewJson.materials.length);
            expect(info.plainTextVariables.length).toBe(triggerWithOptionsViewJson.environment_variables.length);
            expect(info.secureVariables.length).toBe(triggerWithOptionsViewJson.secure_environment_variables.length);
          });

          const pipeline = new Pipeline(pipelineJson);
          pipeline.viewInformationForTriggerWithOptions().then(successCallback);

          expect(successCallback).toHaveBeenCalled();

          const request = jasmine.Ajax.requests.mostRecent();
          expect(request.method).toBe('GET');
          expect(request.url).toBe(`/go/api/internal/trigger_with_options_view/${pipelineJson.name}`);
          expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          expect(request.requestHeaders['X-GoCD-Confirm']).toContain('true');
        });
      });
    });
  });

  const pipelineJsonFor = (pauseInfo) => {
    return {
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
      "can_unlock":             true,
      "pause_info":             pauseInfo,
      "can_administer":         true,
      "can_operate":            true,
      "can_pause":              true,
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
    };
  };

  const triggerWithOptionsViewJson = {
    "environment_variables":        [
      {
        "name":  "version",
        "value": "asdf"
      },
      {
        "name":  "foobar",
        "value": "asdf"
      }
    ],
    "secure_environment_variables": [
      {
        "name":  "secure1",
        "value": "****"
      },
      {
        "name":  "highly secure",
        "value": "****"
      }
    ],

    "materials": [
      {
        "type":        "Git",
        "name":        "https://github.com/ganeshspatil/gocd",
        "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
        "revision":    {
          "date":              "2018-02-08T04:32:11Z",
          "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
          "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
          "last_run_revision": "a2d23c5505ac571d9512bdf08d6287e47dcb52d5"
        }
      }
    ]
  };
});
