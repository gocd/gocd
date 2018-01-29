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

const $        = require('jquery');
const m        = require('mithril');
const mrequest = require('helpers/mrequest');
const Routes   = require('gen/js-routes');

const PipelineGroups = require('models/dashboard/pipeline_groups');
const Pipelines      = require('models/dashboard/pipelines');


const Dashboard = function (json) {
  const pipelineGroups = new PipelineGroups(json._embedded.pipeline_groups);
  const pipelines      = new Pipelines(json._embedded.pipelines);

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

Dashboard.API_VERSION = 'v2';

const dummyPipelineGroup = {
  "_links":         {
    "self": {
      "href": "https://build.gocd.org/go/api/config/pipeline_groups"
    },
    "doc":  {
      "href": "https://api.go.cd/current/#pipeline-groups"
    }
  },
  "name":           "non-existing-group",
  "pipelines":      [
    "build-linux"
  ],
  "can_administer": true
};

const dummyPipeline = {
  "_links":                 {
    "self":                 {
      "href": "https://build.gocd.org/go/api/pipelines/build-linux/history"
    },
    "doc":                  {
      "href": "https://api.go.cd/current/#pipelines"
    },
    "settings_path":        {
      "href": "https://build.gocd.org/go/admin/pipelines/build-linux/general"
    },
    "trigger":              {
      "href": "https://build.gocd.org/go/api/pipelines/build-linux/schedule"
    },
    "trigger_with_options": {
      "href": "https://build.gocd.org/go/api/pipelines/build-linux/schedule"
    },
    "pause":                {
      "href": "https://build.gocd.org/go/api/pipelines/build-linux/pause"
    },
    "unpause":              {
      "href": "https://build.gocd.org/go/api/pipelines/build-linux/unpause"
    }
  },
  "name":                   "build-linux",
  "last_updated_timestamp": 1516754093481,
  "locked":                 false,
  "pause_info":             {
    "paused":       false,
    "paused_by":    null,
    "pause_reason": null
  },
  "can_operate":            true,
  "can_administer":         true,
  "can_unlock":             true,
  "can_pause":              true,
  "_embedded":              {
    "instances": [
      {
        "_links":       {
          "self":        {
            "href": "https://build.gocd.org/go/api/pipelines/build-linux/instance/2121"
          },
          "history_url": {
            "href": "https://build.gocd.org/go/api/pipelines/build-linux/history"
          },
          "vsm_url":     {
            "href": "https://build.gocd.org/go/pipelines/value_stream_map/build-linux/2121"
          },
          "compare_url": {
            "href": "https://build.gocd.org/go/compare/build-linux/2120/with/2121"
          }
        },
        "label":        "2121",
        "scheduled_at": "2018-01-23T15:47:15.009Z",
        "triggered_by": "Triggered by changes",
        "build_cause":  {
          "approver":           "",
          "is_forced":          false,
          "trigger_message":    "modified by Jyoti Singh <jyotisingh7@gmail.com>",
          "material_revisions": [
            {
              "material_type": "Git",
              "material_name": "https://mirrors.gocd.org/git/gocd/gocd",
              "changed":       true,
              "modifications": [
                {
                  "_links":        {
                    "vsm": {
                      "href": "https://build.gocd.org/go/materials/value_stream_map/21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b/8a847b96ee8d38173f80178ed1285f0e53a970e0"
                    }
                  },
                  "user_name":     "Jyoti Singh <jyotisingh7@gmail.com>",
                  "revision":      "8a847b96ee8d38173f80178ed1285f0e53a970e0",
                  "modified_time": "2018-01-23T15:46:44.000Z",
                  "comment":       "Merge pull request #4213 from varshavaradarajan/revert-db-migrations\n\nComment out the db migrations for deleting old build related data."
                },
                {
                  "_links":        {
                    "vsm": {
                      "href": "https://build.gocd.org/go/materials/value_stream_map/21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b/97d3c2abd50a8447ee4ab122cc662a128ba2cfc2"
                    }
                  },
                  "user_name":     "Varsha Varadarajan <varshasvaradarajan@gmail.com>",
                  "revision":      "97d3c2abd50a8447ee4ab122cc662a128ba2cfc2",
                  "modified_time": "2018-01-23T12:32:07.000Z",
                  "comment":       "Comment out the db migrations for deleting old build related data."
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
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-non-server/1"
                }
              },
              "name":         "build-non-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:47:15.009Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            }
          ]
        }
      },
      {
        "_links":       {
          "self":        {
            "href": "https://build.gocd.org/go/api/pipelines/build-linux/instance/2121"
          },
          "history_url": {
            "href": "https://build.gocd.org/go/api/pipelines/build-linux/history"
          },
          "vsm_url":     {
            "href": "https://build.gocd.org/go/pipelines/value_stream_map/build-linux/2121"
          },
          "compare_url": {
            "href": "https://build.gocd.org/go/compare/build-linux/2120/with/2121"
          }
        },
        "label":        "2121",
        "scheduled_at": "2018-01-23T15:47:15.009Z",
        "triggered_by": "Triggered by changes",
        "build_cause":  {
          "approver":           "",
          "is_forced":          false,
          "trigger_message":    "modified by Jyoti Singh <jyotisingh7@gmail.com>",
          "material_revisions": [
            {
              "material_type": "Git",
              "material_name": "https://mirrors.gocd.org/git/gocd/gocd",
              "changed":       true,
              "modifications": [
                {
                  "_links":        {
                    "vsm": {
                      "href": "https://build.gocd.org/go/materials/value_stream_map/21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b/8a847b96ee8d38173f80178ed1285f0e53a970e0"
                    }
                  },
                  "user_name":     "Jyoti Singh <jyotisingh7@gmail.com>",
                  "revision":      "8a847b96ee8d38173f80178ed1285f0e53a970e0",
                  "modified_time": "2018-01-23T15:46:44.000Z",
                  "comment":       "Merge pull request #4213 from varshavaradarajan/revert-db-migrations\n\nComment out the db migrations for deleting old build related data."
                },
                {
                  "_links":        {
                    "vsm": {
                      "href": "https://build.gocd.org/go/materials/value_stream_map/21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b/97d3c2abd50a8447ee4ab122cc662a128ba2cfc2"
                    }
                  },
                  "user_name":     "Varsha Varadarajan <varshasvaradarajan@gmail.com>",
                  "revision":      "97d3c2abd50a8447ee4ab122cc662a128ba2cfc2",
                  "modified_time": "2018-01-23T12:32:07.000Z",
                  "comment":       "Comment out the db migrations for deleting old build related data."
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
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-non-server/1"
                }
              },
              "name":         "build-non-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:47:15.009Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Failing",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Unknown",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Passed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Failed",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Building",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            },
            {
              "_links":       {
                "self": {
                  "href": "https://build.gocd.org/go/api/stages/build-linux/2121/build-server/1"
                }
              },
              "name":         "build-server",
              "status":       "Cancelled",
              "approved_by":  "changes",
              "scheduled_at": "2018-01-23T15:54:16.861Z"
            }
          ]
        }
      }
    ]
  }
};

Dashboard.get = () => {
  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv2ShowDashboardPath(), //eslint-disable-line camelcase
      beforeSend:  mrequest.xhrConfig.forVersion(Dashboard.API_VERSION),
      contentType: false
    });

    jqXHR.then((data) => {
      data._embedded.pipeline_groups.push(dummyPipelineGroup);
      data._embedded.pipelines.push(dummyPipeline);

      deferred.resolve(new Dashboard(data));
    });

    jqXHR.always(m.redraw);

  }).promise();
};

module.exports = Dashboard;
