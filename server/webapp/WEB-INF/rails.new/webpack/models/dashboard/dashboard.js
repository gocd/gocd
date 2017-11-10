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

const PipelineGroups = require('models/dashboard/pipeline_groups');
const Pipelines      = require('models/dashboard/pipelines');


const Dashboard = function (json) {
  const pipelineGroups = new PipelineGroups(json.pipeline_groups);
  const pipelines      = new Pipelines(json.pipelines);

  this.getPipelineGroups = () => {
    return pipelineGroups.groups;
  };

  this.getPipelines = () => {
    return pipelines.pipelines;
  };

  this.findPipeline = (pipelineName) => {
    return pipelines.find(pipelineName);
  };
};


Dashboard.get = () => {
  const json = {
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
        "pipelines": ["up42", "up43"]
      },
      {
        "_links":    {
          "self": {
            "href": "http://localhost:8153/go/api/config/pipeline_groups/pipelinegroup2"
          },
          "doc":  {
            "href": "https://api.go.cd/current/#pipeline-groups"
          }
        },
        "name":      "pipelinegroup2",
        "pipelines": ["up44"]
      }
    ],
    "pipelines":       [
      {
        "_links":                 {
          "self":                 {
            "href": "http://localhost:8153/go/tab/pipeline/history/up42"
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
                  "href": "http://localhost:8153/go/tab/pipeline/history/up42"
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
              "schedule_at":  "2017-11-10T07:25:28.539Z",
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
                  },
                  {
                    "_links":       {
                      "self": {
                        "href": "http://localhost:8153/go/api/stages/up42/1/up42_stag2e/1"
                      },
                      "doc":  {
                        "href": "https://api.go.cd/current/#get-stage-instance"
                      }
                    },
                    "name":         "up42_stage2",
                    "status":       "Failed",
                    "approved_by":  "changes",
                    "scheduled_at": "2017-11-10T07:25:28.539Z"
                  }
                ]
              }
            }
          ]
        }
      },
      {
        "_links":                 {
          "self":                 {
            "href": "http://localhost:8153/go/tab/pipeline/history/up43"
          },
          "doc":                  {
            "href": "https://api.go.cd/current/#pipelines"
          },
          "settings_path":        {
            "href": "http://localhost:8153/go/admin/pipelines/up43/general"
          },
          "trigger":              {
            "href": "http://localhost:8153/go/api/pipelines/up43/schedule"
          },
          "trigger_with_options": {
            "href": "http://localhost:8153/go/api/pipelines/up43/schedule"
          },
          "pause":                {
            "href": "http://localhost:8153/go/api/pipelines/up43/pause"
          },
          "unpause":              {
            "href": "http://localhost:8153/go/api/pipelines/up43/unpause"
          }
        },
        "name":                   "up43",
        "last_updated_timestamp": 1510299695474,
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
                  "href": "http://localhost:8153/go/api/pipelines/up43/instance/1"
                },
                "doc":             {
                  "href": "https://api.go.cd/current/#get-pipeline-instance"
                },
                "history_url":     {
                  "href": "http://localhost:8153/go/tab/pipeline/history/up43"
                },
                "vsm_url":         {
                  "href": "http://localhost:8153/go/pipelines/value_stream_map/up43/1"
                },
                "compare_url":     {
                  "href": "http://localhost:8153/go/compare/up43/0/with/1"
                },
                "build_cause_url": {
                  "href": "http://localhost:8153/go/pipelines/up43/1/build_cause"
                }
              },
              "label":        "1",
              "schedule_at":  "2017-11-10T07:28:21.088Z",
              "triggered_by": "admin",
              "_embedded":    {
                "stages": [
                  {
                    "_links":       {
                      "self": {
                        "href": "http://localhost:8153/go/api/stages/up43/1/up42_stage/1"
                      },
                      "doc":  {
                        "href": "https://api.go.cd/current/#get-stage-instance"
                      }
                    },
                    "name":         "up42_stage",
                    "status":       "Passed",
                    "approved_by":  "admin",
                    "scheduled_at": "2017-11-10T07:28:21.088Z"
                  }
                ]
              }
            }
          ]
        }
      },
      {
        "_links":                 {
          "self":                 {
            "href": "http://localhost:8153/go/tab/pipeline/history/up44"
          },
          "doc":                  {
            "href": "https://api.go.cd/current/#pipelines"
          },
          "settings_path":        {
            "href": "http://localhost:8153/go/admin/pipelines/up44/general"
          },
          "trigger":              {
            "href": "http://localhost:8153/go/api/pipelines/up44/schedule"
          },
          "trigger_with_options": {
            "href": "http://localhost:8153/go/api/pipelines/up44/schedule"
          },
          "pause":                {
            "href": "http://localhost:8153/go/api/pipelines/up44/pause"
          },
          "unpause":              {
            "href": "http://localhost:8153/go/api/pipelines/up44/unpause"
          }
        },
        "name":                   "up44",
        "last_updated_timestamp": 1510299695477,
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
                  "href": "http://localhost:8153/go/api/pipelines/up44/instance/2"
                },
                "doc":             {
                  "href": "https://api.go.cd/current/#get-pipeline-instance"
                },
                "history_url":     {
                  "href": "http://localhost:8153/go/tab/pipeline/history/up44"
                },
                "vsm_url":         {
                  "href": "http://localhost:8153/go/pipelines/value_stream_map/up44/2"
                },
                "compare_url":     {
                  "href": "http://localhost:8153/go/compare/up44/1/with/2"
                },
                "build_cause_url": {
                  "href": "http://localhost:8153/go/pipelines/up44/2/build_cause"
                }
              },
              "label":        "2",
              "schedule_at":  "2017-11-10T07:43:51.055Z",
              "triggered_by": "admin",
              "_embedded":    {
                "stages": [
                  {
                    "_links":       {
                      "self": {
                        "href": "http://localhost:8153/go/api/stages/up44/2/up42_stage/1"
                      },
                      "doc":  {
                        "href": "https://api.go.cd/current/#get-stage-instance"
                      }
                    },
                    "name":         "up42_stage",
                    "status":       "Building",
                    "approved_by":  "admin",
                    "scheduled_at": "2017-11-10T07:43:51.055Z"
                  }
                ]
              }
            }
          ]
        }
      }
    ]
  };

  return new Promise((fulfil) => {
    fulfil(new Dashboard(json));
  });
};

module.exports = Dashboard;
