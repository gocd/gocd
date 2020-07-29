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
import {DashboardViewModel} from "../../../../webpack/views/dashboard/models/dashboard_view_model";

describe("Dashboard Stages Instance Widget", () => {

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

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
          "can_operate": true,
          "allow_only_on_success_of_previous_stage": false,
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
          "approval_type": "manual",
          "approved_by":  "changes",
          "can_operate": true,
          "allow_only_on_success_of_previous_stage": false,
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
          "can_operate": true,
          "allow_only_on_success_of_previous_stage": false,
          "scheduled_at": "2017-11-10T07:25:28.539Z"
        }
      ]
    }
  };

  let pipelineName;
  let pipelineInstance;
  let stagesInstance;

  beforeEach(() => {
    pipelineName   = 'up42';
    pipelineInstance = new PipelineInstance(pipelineInstanceJson, pipelineName);
    stagesInstance = pipelineInstance.stages;
    mount();
  });

  function mount() {
    helper.mount(() => m(StagesInstanceWidget, {
      stages:        stagesInstance,
      stageOverview: new DashboardViewModel().stageOverview
    }));
  }

  it("should render each stage instance", () => {
    const stagesInstance = helper.qa('.pipeline_stage');

    expect(stagesInstance[0]).toHaveClass('failed');
    expect(stagesInstance[1]).toHaveClass('unknown');
  });

  it("should not link to stage details page for stages with no run", () => {
    expect(helper.q('span.pipeline_stage')).toBeInDOM();
    expect(helper.q('span.pipeline_stage')).toHaveClass('unknown');
  });

  it("should show stage status on hover", () => {
    const stages = pipelineInstanceJson._embedded.stages;
    const stage1Status = `${stages[0].name} (${stages[0].status})`;
    const stage2Status = `${stages[1].name} (${stages[1].status})`;
    const stage3Status = `${stages[2].name} (cancelled by: ${stages[2].cancelled_by})`;

    expect(helper.qa('.pipeline_stage')[0].title).toEqual(stage1Status);
    expect(helper.qa('.pipeline_stage')[1].title).toEqual(stage2Status);
    expect(helper.qa('.pipeline_stage')[2].title).toEqual(stage3Status);
  });

  it("should render manual gate icon before the manual stage", () => {
    expect(helper.qa('.manual_gate')).toBeInDOM();
  });

  it("should provide help text for the manual gate", () => {
    expect(helper.q('.manual_gate').title).toEqual('Awaiting approval. Waiting for users with the operate permission to schedule \'up42_stage2\' stage');
  });

  it("should render enabled manual gate", () => {
    expect(helper.q('.manual_gate')).not.toHaveClass('disabled');
  });

  it("should render disabled manual gate when previous stage is in progress", () => {
    stagesInstance[0].isCompleted = () => false;
    m.redraw.sync();

    expect(helper.q('.manual_gate')).toHaveClass('disabled');
    expect(helper.q('.tooltip')).toContainText('Can not schedule next stage - Either the previous stage hasn\'t run or is in progress.');
  });

  it("should render disabled manual gate when user does not have permission to operate on the pipeline", () => {
    stagesInstance[1].canOperate = false;
    m.redraw.sync();

    expect(helper.q('.manual_gate')).toHaveClass('disabled');
    expect(helper.q('.tooltip')).toContainText('Can not schedule next stage - You don\'t have permissions to schedule the next stage.');
  });

  it("should render disabled manual gate when the stage has already been scheduled", () => {
    stagesInstance[1].isBuildingOrCompleted = () => true;
    stagesInstance[1].approvedBy = 'admin';
    m.redraw.sync();

    expect(helper.q('.manual_gate')).toHaveClass('disabled');
    expect(helper.q('.tooltip')).toContainText('Approved by \'admin\'.');
  });

  it("should render disabled manual gate when previous stage is failed and allow_only_on_success_of_previous_stage is set to true", () => {
    stagesInstance[0].status = 'Failed';
    stagesInstance[1].triggerOnlyOnSuccessOfPreviousStage = () => true;
    m.redraw.sync();

    expect(helper.q('.manual_gate')).toHaveClass('disabled');
    expect(helper.q('.tooltip')).toContainText('Can not schedule next stage - stage \'up42_stage2\' is set to run only on success of previous stage, whereas, the previous stage \'up42_stage\' has Failed.');
  });

  it("should render disabled manual gate when previous stage is cancelled and allow_only_on_success_of_previous_stage is set to true", () => {
    stagesInstance[0].status = 'Cancelled';
    stagesInstance[1].triggerOnlyOnSuccessOfPreviousStage = () => true;
    m.redraw.sync();

    expect(helper.q('.manual_gate')).toHaveClass('disabled');
    expect(helper.q('.tooltip')).toContainText('Can not schedule next stage - stage \'up42_stage2\' is set to run only on success of previous stage, whereas, the previous stage \'up42_stage\' has Cancelled.');
  });
});
