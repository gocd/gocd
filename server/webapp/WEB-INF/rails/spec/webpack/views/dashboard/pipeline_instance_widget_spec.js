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
import SparkRoutes from "helpers/spark_routes";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Dashboard Pipeline Instance Widget", () => {

  const m             = require("mithril");
  const _             = require("lodash");
  const TimeFormatter = require('helpers/time_formatter');

  const PipelineInstanceWidget = require("views/dashboard/pipeline_instance_widget");
  const PipelineInstance       = require('models/dashboard/pipeline_instance');
  const Dashboard              = require('models/dashboard/dashboard');
  const DashboardVM            = require("views/dashboard/models/dashboard_view_model");


  const helper = new TestHelper();

  const pipelineInstanceJson = {
    "_links":       {
      "self": {
        "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
      },
      "doc":  {
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
  };

  const buildCauseJson = {
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
            "comment":       "Initial commit #1234"
          }
        ]
      }
    ]
  };

  const pipelineName = 'up42';
  const instance     = new PipelineInstance(pipelineInstanceJson, pipelineName);

  beforeEach(() => {
    const dashboardViewModel = new DashboardVM();
    const dashboard          = new Dashboard();
    dashboard.initialize(dashboardJsonForPipelines([pipelineName]));

    helper.mount(() => m(PipelineInstanceWidget, {
      instance,
      buildCause:   dashboardViewModel.buildCause,
      dropdown:     dashboardViewModel.dropdown,
      trackingTool: {link: "http://example.com/${ID}", regex: "#(\\d+)"},
      pipelineName
    }));
  });

  afterEach(helper.unmount.bind(helper));

  it("should render instance label", () => {
    expect(helper.find('.pipeline_instance-label')).toContainText('Instance: 1');
  });

  it("should render triggered by information", () => {
    expect(helper.find('.pipeline_instance-details div:nth-child(1)').text()).toEqual(`${pipelineInstanceJson.triggered_by}`);
    const expectedTime = `on ${TimeFormatter.format(pipelineInstanceJson.scheduled_at)}`;
    expect(helper.find('.pipeline_instance-details div:nth-child(2)').text()).toEqual(expectedTime);
  });

  it("should show server triggered by information on hover", () => {
    const expectedTime = TimeFormatter.formatInServerTime(pipelineInstanceJson.scheduled_at);
    expect(helper.find('.pipeline_instance-details div:nth-child(2)').get(0).title).toEqual(expectedTime);
  });

  it("should render compare link", () => {
    const links       = helper.find('.info a');
    const compareLink = links.get(0);

    expect(compareLink).toContainText('Compare');

    const hasCompareLink = compareLink.href.indexOf("/go/compare/up42/0/with/1") >= 0;
    expect(hasCompareLink).toBe(true);
  });

  it("should render changes link", () => {
    const links       = helper.find('.info a');
    const changesLink = links.get(1);

    expect(changesLink).toContainText('Changes');
  });

  it("should show changes once changes link is clicked", () => {
    expect(helper.find('.material_changes')).not.toBeInDOM();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath(pipelineName, instance.counter), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });
      helper.find('.info a').get(1).click();
    });

    expect(helper.find('.material_changes')).toBeInDOM();
    expect(helper.find('.comment')).toHaveHtml('<p>Initial commit <a target="story_tracker" href="http://example.com/1234">#1234</a></p>');
  });

  it("should render vsm link", () => {
    const links   = helper.find('.info a');
    const vsmLink = links.get(2);

    expect(vsmLink).toContainText('VSM');

    const hasVSMLink = vsmLink.href.indexOf("/go/pipelines/value_stream_map/up42/1") >= 0;
    expect(hasVSMLink).toBe(true);
  });

  it("should render stages instance", () => {
    expect(helper.find('.pipeline_stages')).toBeInDOM();
    expect(helper.find('.pipeline_stage')).toBeInDOM();
  });

  const dashboardJsonForPipelines = (pipelines) => {
    return {
      "_embedded": {
        "pipeline_groups": [
          {
            "_links":         {
              "self": {
                "href": "http://localhost:8153/go/api/config/pipeline_groups/first"
              },
              "doc":  {
                "href": "https://api.go.cd/current/#pipeline-groups"
              }
            },
            "name":           "first",
            pipelines,
            "can_administer": true
          }
        ],
        "pipelines":       pipelinesJsonForPipelines(pipelines)
      }
    };
  };

  const pipelinesJsonForPipelines = (pipelineNames) => {
    const pipelines = [];
    _.each((pipelineNames), (pipelineName) => {
      pipelines.push({
        "_links":                 {
          "self": {
            "href": "http://localhost:8153/go/api/pipelines/up42/history"
          },
          "doc":  {
            "href": "https://api.go.cd/current/#pipelines"
          }
        },
        "name":                   pipelineName,
        "last_updated_timestamp": 1510299695473,
        "locked":                 false,
        "can_pause":              true,
        "pause_info":             {
          "paused":       false,
          "paused_by":    null,
          "pause_reason": null
        },
        "_embedded":              {
          "instances": [pipelineInstanceJson]
        }
      });
    });

    return pipelines;
  };
});
