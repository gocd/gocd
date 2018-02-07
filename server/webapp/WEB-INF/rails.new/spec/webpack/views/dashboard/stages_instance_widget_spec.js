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
describe("Dashboard Stages Instance Widget", () => {
  const m = require("mithril");

  const StagesInstanceWidget = require("views/dashboard/stages_instance_widget");
  const PipelineInstance     = require('models/dashboard/pipeline_instance');

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
        },
        {
          "_links":       {
            "self": {
              "href": "http://localhost:8153/go/api/stages/up42/1/up42_stage2/1"
            },
            "doc":  {
              "href": "https://api.go.cd/current/#get-stage-instance"
            }
          },
          "name":         "up42_stage2",
          "counter":      "1",
          "status":       "Unknown",
          "approved_by":  "changes",
          "scheduled_at": "2017-11-10T07:25:28.539Z"
        }
      ]
    }
  };

  const pipelineName   = 'up42';
  const stagesInstance = new PipelineInstance(pipelineInstanceJson, pipelineName).stages;

  beforeEach(() => {
    m.mount(root, {
      view() {
        return m(StagesInstanceWidget, {
          stages: stagesInstance
        });
      }
    });
    m.redraw(true);
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should render each stage instance", () => {
    const stagesInstance = $root.find('.pipeline_stage');

    expect(stagesInstance.get(0)).toHaveClass('failed');
    expect(stagesInstance.get(1)).toHaveClass('unknown');
  });

  it("should link to stage details page", () => {
    const stagesInstance = $root.find('.pipeline_stage');

    expect(stagesInstance.get(0).href.indexOf(`/go/pipelines/up42/1/up42_stage/1`)).not.toEqual(-1);
  });

  it("should not link to stage details page for stages with no run", () => {
    expect($root.find('span.pipeline_stage')).toBeInDOM();
    expect($root.find('span.pipeline_stage')).toHaveClass('unknown');
  });

  it("should show stage status on hover", () => {
    const stages       = pipelineInstanceJson._embedded.stages;
    const stage1Status = `${stages[0].name} (${stages[0].status})`;
    const stage2Status = `${stages[1].name} (${stages[1].status})`;

    expect($root.find('.pipeline_stage').get(0).title).toEqual(stage1Status);
    expect($root.find('.pipeline_stage').get(1).title).toEqual(stage2Status);
  });
});
