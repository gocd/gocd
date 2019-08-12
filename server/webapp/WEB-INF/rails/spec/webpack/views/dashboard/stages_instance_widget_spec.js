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
import {TestHelper} from "views/pages/spec/test_helper";
import {PipelineInstance} from "models/dashboard/pipeline_instance";
import {StagesInstanceWidget} from "views/dashboard/stages_instance_widget";
import m from "mithril";

describe("Dashboard Stages Instance Widget", () => {

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

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
        },
        {
          "_links":       {
            "self": {
              "href": "http://localhost:8153/go/api/stages/up42/1/cancelled_stage/1"
            },
            "doc":  {
              "href": "https://api.go.cd/current/#get-stage-instance"
            }
          },
          "name":         "cancelled_stage",
          "counter":      "1",
          "status":       "Cancelled",
          "cancelled_by": "someone",
          "approved_by":  "changes",
          "scheduled_at": "2017-11-10T07:25:28.539Z"
        }
      ]
    }
  };

  const pipelineName   = 'up42';
  const stagesInstance = new PipelineInstance(pipelineInstanceJson, pipelineName).stages;

  beforeEach(() => {
    helper.mount(() => m(StagesInstanceWidget, {
      stages: stagesInstance
    }));
  });

  it("should render each stage instance", () => {
    const stagesInstance = helper.find('.pipeline_stage');

    expect(stagesInstance.get(0)).toHaveClass('failed');
    expect(stagesInstance.get(1)).toHaveClass('unknown');
  });

  it("should link to stage details page", () => {
    const stagesInstance = helper.find('.pipeline_stage');

    expect(stagesInstance.get(0).href.indexOf(`/go/pipelines/up42/1/up42_stage/1`)).not.toEqual(-1);
  });

  it("should not link to stage details page for stages with no run", () => {
    expect(helper.find('span.pipeline_stage')).toBeInDOM();
    expect(helper.find('span.pipeline_stage')).toHaveClass('unknown');
  });

  it("should show stage status on hover", () => {
    const stages       = pipelineInstanceJson._embedded.stages;
    const stage1Status = `${stages[0].name} (${stages[0].status})`;
    const stage2Status = `${stages[1].name} (${stages[1].status})`;
    const stage3Status = `${stages[2].name} (cancelled by: ${stages[2].cancelled_by})`;

    expect(helper.find('.pipeline_stage').get(0).title).toEqual(stage1Status);
    expect(helper.find('.pipeline_stage').get(1).title).toEqual(stage2Status);
    expect(helper.find('.pipeline_stage').get(2).title).toEqual(stage3Status);
  });
});
