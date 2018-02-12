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
describe("Dashboard Pipeline Instance Widget", () => {
  const m      = require("mithril");
  const moment = require("moment");
  require("moment-duration-format");
  const PipelineInstanceWidget = require("views/dashboard/pipeline_instance_widget");
  const PipelineInstance       = require('models/dashboard/pipeline_instance');
  const DashboardVM            = require("views/dashboard/models/dashboard_view_model");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

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
  };

  const pipelineName = 'up42';
  const instance     = new PipelineInstance(pipelineInstanceJson, pipelineName);

  beforeEach(() => {
    const pipelineName       = "dummy";
    const dashboardViewModel = new DashboardVM();
    dashboardViewModel.initialize([pipelineName]);

    m.mount(root, {
      view() {
        return m(PipelineInstanceWidget, {
          instance,
          dropdown: dashboardViewModel.dropdown,
          pipelineName
        });
      }
    });
    m.redraw(true);
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should render instance label", () => {
    expect($root.find('.pipeline_instance-label')).toContainText('Instance: 1');
  });

  it("should render triggered by information", () => {
    expect($root.find('.pipeline_instance-details div:nth-child(1)').text()).toEqual(`${ pipelineInstanceJson.triggered_by }`);
    const expectedTime = moment(new Date(pipelineInstanceJson.scheduled_at)).format('[on] DD MMM YYYY [at] HH:mm:ss [Local Time]');
    expect($root.find('.pipeline_instance-details div:nth-child(2)').text()).toEqual(expectedTime);
  });

  it("should show server triggered by information on hover", () => {
    const expectedTime = moment(new Date(pipelineInstanceJson.scheduled_at)).format("[Server Time:] DD MMM, YYYY [at] HH:mm:ss Z");
    expect($root.find('.pipeline_instance-details div:nth-child(2)').get(0).title).toEqual(expectedTime);
  });

  it("should render compare link", () => {
    const links       = $root.find('.info a');
    const compareLink = links.get(0);

    expect(compareLink).toContainText('Compare');
    expect(compareLink.href).toEqual(pipelineInstanceJson._links.compare_url.href);
  });

  it("should render changes link", () => {
    const links       = $root.find('.info a');
    const changesLink = links.get(1);

    expect(changesLink).toContainText('Changes');
  });

  it("should show changes once changes link is clicked", () => {
    expect($root.find('.material_changes')).not.toBeInDOM();

    $root.find('.info a').get(1).click();

    expect($root.find('.material_changes')).toBeInDOM();
  });

  it("should render vsm link", () => {
    const links       = $root.find('.info a');
    const compareLink = links.get(2);

    expect(compareLink).toContainText('VSM');
    expect(compareLink.href).toEqual(pipelineInstanceJson._links.vsm_url.href);
  });

  it("should render stages instance", () => {
    expect($root.find('.pipeline_stages')).toBeInDOM();
    expect($root.find('.pipeline_stage')).toBeInDOM();
  });
});
