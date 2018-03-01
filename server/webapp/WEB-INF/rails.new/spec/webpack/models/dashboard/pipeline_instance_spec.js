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
  describe('Pipeline Instance Model', () => {
    const pipelineName     = "up42";
    const PipelineInstance = require('models/dashboard/pipeline_instance');

    it("should deserialize from json", () => {
      const pipelineInstance = new PipelineInstance(pipelineInstanceJson, pipelineName);

      expect(pipelineInstance.label).toBe(pipelineInstanceJson.label);
      expect(pipelineInstance.counter).toBe(pipelineInstanceJson.counter);

      expect(pipelineInstance.scheduledAt).toEqual(pipelineInstanceJson.scheduled_at);
      expect(pipelineInstance.triggeredBy).toEqual(pipelineInstanceJson.triggered_by);

      expect(pipelineInstance.vsmPath).toEqual(pipelineInstanceJson._links.vsm_url.href);
      expect(pipelineInstance.comparePath).toEqual(pipelineInstanceJson._links.compare_url.href);

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

      expect(pipelineInstance.isFirstStageInProgress()).toEqual(false);
    });

    const pipelineInstanceJson = {
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
            "approved_by":  "changes",
            "scheduled_at": "2017-11-10T07:25:28.539Z"
          }
        ]
      }
    };
  });
});
