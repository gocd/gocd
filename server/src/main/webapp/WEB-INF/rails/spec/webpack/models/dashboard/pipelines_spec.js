/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {Pipelines} from "models/dashboard/pipelines";
import {Pipeline} from "models/dashboard/pipeline";

describe("Dashboard", () => {
  describe('Pipelines Model', () => {


    it("should deserialize from json", () => {
      const pipelines = Pipelines.fromJSON(pipelinesData);

      expect(pipelines.size).toBe(1);
    });

    it("should find the pipeline", () => {
      const pipelines = Pipelines.fromJSON(pipelinesData);

      const pipelineName     = "up42";
      const actualPipeline   = pipelines.find(pipelineName);
      const expectedPipeline = new Pipeline(pipelinesData[0]);
      expect(actualPipeline.name).toEqual(expectedPipeline.name);
    });

    const pipelinesData = [
      {
        "_links":                 {
          "self":                 {
            "href": "http://localhost:8153/go/api/pipelines/up42/history"
          },
          "doc":                  {
            "href": "https://api.go.cd/current/#pipelines"
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
        "template_info": {
          "is_using_template": false,
          "template_name":     null
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
    ];
  });
});
