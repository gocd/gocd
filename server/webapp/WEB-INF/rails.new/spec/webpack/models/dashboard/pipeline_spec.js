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

describe("Dashboard", () => {
  describe('Pipeline Model', () => {
    const Pipeline = require('models/dashboard/pipeline');

    it("should deserialize from json", () => {
      const pipeline = new Pipeline(pipelineJson);

      expect(pipeline.name()).toBe(pipelineJson.name);
      expect(pipeline.settingsPath()).toBe(pipelineJson._links.settings_path.href);
      expect(pipeline.historyPath()).toBe(pipelineJson._links.self.href);
      expect(pipeline.instances().length).toEqual(pipelineJson._embedded.instances.length);
      expect(pipeline.canAdminister).toEqual(pipelineJson.can_administer);
    });

    const pipelineJson = {
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
      "can_administer": true,
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
            "scheduled_at":  "2017-11-10T07:25:28.539Z",
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
  });
});
