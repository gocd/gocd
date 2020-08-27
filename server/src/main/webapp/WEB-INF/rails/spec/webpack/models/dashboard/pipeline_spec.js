/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {Pipeline} from "models/dashboard/pipeline";

describe("Dashboard", () => {

  let pipelineJson, defaultPauseInfo;
  beforeEach(() => {
    defaultPauseInfo = {
      "paused":       true,
      "paused_by":    "admin",
      "pause_reason": "under construction"
    };
    pipelineJson     = pipelineJsonFor(defaultPauseInfo);
  });
  describe('Pipeline Model', () => {

    it("should deserialize from json", () => {
      const pipeline = new Pipeline(pipelineJson);

      expect(pipeline.name).toBe(pipelineJson.name);

      expect(pipeline.canAdminister).toBe(true);
      expect(pipeline.settingsPath).toBe(`/go/admin/pipelines/${pipelineJson.name}/edit#!${pipelineJson.name}/general`);

      expect(pipeline.historyPath).toBe(`/go/pipeline/activity/${pipelineJson.name}`);
      expect(pipeline.instances.length).toEqual(pipelineJson._embedded.instances.length);

      expect(pipeline.isPaused).toBe(true);
      expect(pipeline.pausedBy).toBe("admin");
      expect(pipeline.pausedCause).toBe("under construction");
      expect(pipeline.canPause).toBe(true);

      expect(pipeline.canOperate).toBe(true);

      expect(pipeline.isLocked).toBe(false);
      expect(pipeline.canUnlock).toBe(true);

      expect(pipeline.isDefinedInConfigRepo()).toBe(false);

      expect(pipeline.trackingTool).toEqual({
        "regex": "#(\\d+)",
        "link":  "http://example.com/${ID}/"
      });

      expect(pipeline.isFirstStageInProgress()).toBe(false);
    });

    it("should disable trigger when user dont have permissions", () => {
      pipelineJson['can_operate'] = false;

      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.canOperate).toBe(false);
      expect(pipeline.triggerDisabled()).toBe(true);
    });

    it("should disable trigger when first stage of pipeline is in progress", () => {
      pipelineJson._embedded.instances[0]._embedded.stages[0].status = 'Building';

      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.isFirstStageInProgress()).toBe(true);
      expect(pipeline.triggerDisabled()).toBe(true);
    });

    it("should disable trigger when pipeline is locked", () => {
      pipelineJson.locked = true;

      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.isLocked).toBe(true);
      expect(pipeline.triggerDisabled()).toBe(true);
    });

    it("should disable trigger when pipeline is paused", () => {
      pipelineJson.pause_info.paused = true;

      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.isPaused).toBe(true);
      expect(pipeline.triggerDisabled()).toBe(true);
    });

    it("should return counters for pipeline instances", () => {
      const pipeline         = new Pipeline(pipelineJson);
      const instanceCounters = pipeline.getInstanceCounters();
      expect(instanceCounters.length).toEqual(1);
      expect(instanceCounters).toEqual([1]);
    });

    it("should answer whether pipeline is defined using template", () => {
      const pipeline         = new Pipeline(pipelineJson);
      expect(pipeline.isUsingTemplate).toEqual(true);
    });

    it("should return template name", () => {
      const pipeline         = new Pipeline(pipelineJson);
      expect(pipeline.templateName).toEqual("build-project");
    });

    it("should return the trigger tooltip text when first stage is building", () => {
      pipelineJson.pause_info.paused                                 = false;
      pipelineJson._embedded.instances[0]._embedded.stages[0].status = "Building";
      const pipeline                                                 = new Pipeline(pipelineJson);
      expect(pipeline.getDisabledTooltipText()).toEqual("Cannot trigger pipeline - First stage is still in progress.");
    });

    it("should return first stage building trigger tooltip text if first stage is building and pipeline is also locked", () => {
      pipelineJson.pause_info.paused                                 = false;
      pipelineJson._embedded.instances[0]._embedded.stages[0].status = "Building";
      pipelineJson.locked                                            = true;
      const pipeline                                                 = new Pipeline(pipelineJson);

      expect(pipeline.getDisabledTooltipText()).toEqual("Cannot trigger pipeline - First stage is still in progress.");
    });

    it("should return first stage building tooltip text if the first stage is building and pipeline is also paused", () => {
      pipelineJson.pause_info.paused                                 = false;
      pipelineJson._embedded.instances[0]._embedded.stages[0].status = "Building";

      const pipeline = new Pipeline(pipelineJson);

      expect(pipeline.getDisabledTooltipText()).toEqual("Cannot trigger pipeline - First stage is still in progress.");
    });

    it("should return empty tooltip trigger text if the trigger button isn't disabled", () => {
      pipelineJson.pause_info.paused = false;
      const pipeline                 = new Pipeline(pipelineJson);
      expect(pipeline.getDisabledTooltipText()).toEqual(undefined);
    });

    it("should return pipeline paused trigger tooltip text when pipeline is paused", () => {
      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.getDisabledTooltipText()).toEqual("Cannot trigger pipeline - Pipeline is currently paused.");
    });

    it("should return pipeline paused trigger tooltip text when pipeline is paused and also locked", () => {
      pipelineJson.locked = true;
      const pipeline      = new Pipeline(pipelineJson);
      expect(pipeline.getDisabledTooltipText()).toEqual("Cannot trigger pipeline - Pipeline is currently paused.");
    });

    it("should return pipeline locked trigger tooltip text when pipeline is locked", () => {
      pipelineJson.locked            = true;
      pipelineJson.pause_info.paused = false;
      const pipeline                 = new Pipeline(pipelineJson);
      expect(pipeline.getDisabledTooltipText()).toEqual("Cannot trigger pipeline - Pipeline is currently locked.");
    });

    it("should return no permissions trigger tooltip text when user doesn't have permission to operate the pipeline", () => {
      pipelineJson = pipelineJsonFor(defaultPauseInfo, false, false);

      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.getDisabledTooltipText()).toEqual("You do not have permission to trigger the pipeline");
    });

    it("should return no permission to pause pipeline text if users don't have permission to pause the pipeline", () => {
      pipelineJson   = pipelineJsonFor({}, false);
      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.getPauseDisabledTooltipText()).toEqual("You do not have permission to pause the pipeline.");
    });

    it("should return no permission to unpause pipeline text if users don't have permission to unpause the pipeline", () => {
      pipelineJson   = pipelineJsonFor(defaultPauseInfo, false);
      const pipeline = new Pipeline(pipelineJson);
      expect(pipeline.getPauseDisabledTooltipText()).toEqual("You do not have permission to unpause the pipeline.");
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

    describe("Trigger", () => {
      it('should Trigger a pipeline with appropriate headers', () => {
        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipelineJson.name}/schedule`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": `Request to schedule pipeline '${pipelineJson.name}' accepted successfully.`}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          const successCallback = jasmine.createSpy();

          const pipeline = new Pipeline(pipelineJson);
          pipeline.trigger().then(successCallback);

          expect(successCallback).toHaveBeenCalled();

          const request = jasmine.Ajax.requests.mostRecent();
          expect(request.method).toBe('POST');
          expect(request.url).toBe(`/go/api/pipelines/${pipelineJson.name}/schedule`);
          expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          expect(request.requestHeaders['X-GoCD-Confirm']).toContain('true');
        });
      });
    });
  });

  const pipelineJsonFor = (pauseInfo, canPause = true, canOperate = true, fromConfigRepo = false) => {
    return {
      "_links":                   {
        "self": {
          "href": "http://localhost:8153/go/api/pipelines/up42/history"
        },
        "doc":  {
          "href": "https://api.go.cd/current/#pipelines"
        }
      },
      "name":                     "up42",
      "last_updated_timestamp":   1510299695473,
      "locked":                   false,
      "can_unlock":               true,
      "pause_info":               pauseInfo,
      "template_info": {
        "is_using_template": true,
        "template_name":     "build-project"
      },
      "can_administer":           true,
      "can_operate":              canOperate,
      "can_pause":                canPause,
      "from_config_repo":         fromConfigRepo,
      "config_repo_id":           "sample_config_repo",
      "config_repo_material_url": "https://foo:1234/go",
      "tracking_tool":            {
        "regex": "#(\\d+)",
        "link":  "http://example.com/${ID}/"
      },
      "_embedded":                {
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
            "counter":      1,
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
    };
  };

});
