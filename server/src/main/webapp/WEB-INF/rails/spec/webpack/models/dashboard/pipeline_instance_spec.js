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
import {SparkRoutes} from "helpers/spark_routes";
import {PipelineInstance} from "models/dashboard/pipeline_instance";

describe("Dashboard", () => {
  describe('Pipeline Instance Model', () => {
    const pipelineName     = "up42";

    it("should deserialize from json", () => {
      const pipelineInstance = new PipelineInstance(pipelineInstanceJson, pipelineName);

      expect(pipelineInstance.label).toBe(pipelineInstanceJson.label);
      expect(pipelineInstance.counter).toBe(pipelineInstanceJson.counter);

      expect(pipelineInstance.scheduledAt).toEqual(pipelineInstanceJson.scheduled_at);
      expect(pipelineInstance.triggeredBy).toEqual(pipelineInstanceJson.triggered_by);

      expect(pipelineInstance.stages.length).toEqual(pipelineInstanceJson._embedded.stages.length);

      const stage = pipelineInstanceJson._embedded.stages[0];
      expect(pipelineInstance.stages[0].name).toEqual(stage.name);
      expect(pipelineInstance.stages[0].counter).toEqual(stage.counter);
      expect(pipelineInstance.stages[0].status).toEqual(stage.status);
      const path = `/go/pipelines/${pipelineName}/${pipelineInstanceJson.counter}/${stage.name}/${stage.counter}`;
      expect(pipelineInstance.stages[0].stageDetailTabPath).toEqual(path);
      expect(pipelineInstance.stages[0].isBuilding()).toEqual(false);

      expect(pipelineInstance.stages[0].isBuildingOrCompleted()).toEqual(true);
      expect(pipelineInstance.stages[1].isBuildingOrCompleted()).toEqual(false);

      expect(pipelineInstance.stages[0].triggerOnCompletionOfPreviousStage()).toEqual(true);
      expect(pipelineInstance.stages[0].isManual()).toEqual(false);

      expect(pipelineInstance.stages[1].triggerOnCompletionOfPreviousStage()).toEqual(false);
      expect(pipelineInstance.stages[1].isManual()).toEqual(true);

      expect(pipelineInstance.stages[0].approvedBy).toEqual("changes");
      expect(pipelineInstance.stages[1].approvedBy).toEqual("admin");

      expect(pipelineInstance.isFirstStageInProgress()).toEqual(false);
    });

    it("should provide a link to vsm", () => {
      const pipelineInstance = new PipelineInstance(pipelineInstanceJson, pipelineName);
      expect(pipelineInstance.vsmPath).toEqual("/go/pipelines/value_stream_map/up42/1");
    });

    it("should provide a link to compare pipeline instances", () => {
      const pipelineInstance = new PipelineInstance(pipelineInstanceJson, pipelineName);
      expect(pipelineInstance.comparePath).toEqual("/go/compare/up42/0/with/1");
    });

    it("should fetch build cause", () => {
      const pipelineInstance = new PipelineInstance(pipelineInstanceJson, pipelineName);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath(pipelineName, pipelineInstance.counter), undefined, 'GET').andReturn({
          responseText:    JSON.stringify(buildCauseJson),
          responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((modifications) => {
          expect(modifications).toHaveLength(1);
        });

        pipelineInstance.getBuildCause().then(successCallback);
        expect(successCallback).toHaveBeenCalled();

        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`/go/api/internal/build_cause/up42/1`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    const pipelineInstanceJson = {
      "_links":       {
        "self":            {
          "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
        },
        "doc":             {
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
            "approval_type": "success",
            "scheduled_at": "2017-11-10T07:25:28.539Z"
          },
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
            "status":       "Unknown",
            "approved_by":  "admin",
            "approval_type": "manual",
            "scheduled_at": "2017-11-10T07:25:28.539Z"
          }
        ]
      }
    };

    const buildCauseJson = {
      'material_revisions': [{
        "material_type": "Pipeline",
        "material_name": "up42",
        "changed":       true,
        "modifications": [{
          "_links":         {
            "vsm":               {
              "href": "http://localhost:8153/go/pipelines/value_stream_map/up42/2"
            },
            "stage_details_url": {
              "href": "http://localhost:8153/go/pipelines/up42/2/up42_stage/1"
            }
          },
          "revision":       "up42/2/up42_stage/1",
          "modified_time":  "2017-12-26T09:01:03.503Z",
          "pipeline_label": "2"
        }]
      }]
    };

  });
});
