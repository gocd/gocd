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

import m from "mithril"
import {TestHelper} from "../../spec/test_helper";
import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {PipelineRunInfo, Stage, StageConfig, StageConfigs, Stages} from "models/pipeline_activity/pipeline_activity";
import {PipelineRunWidget} from "../pipeline_run_info_widget";
import Stream from "mithril/stream";
import {
  building,
  cancelled,
  failed,
  passed,
  PipelineActivityData,
  unknown
} from "models/pipeline_activity/spec/test_data";
import styles from "../index.scss";

describe("PipelineRunInfoWidget", () => {

  const helper           = new TestHelper();
  const showBuildCaseFor = Stream<string>();
  const service          = new PipelineActivityService();

  afterEach(helper.unmount.bind(helper));

  describe("Pipeline has no run", () => {
    let pipelineRunInfo: PipelineRunInfo;
    beforeEach(() => {
      pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.underConstruction().groups[0].history[0]);
    });

    it("should render counter as unknown", () => {
      mount(pipelineRunInfo);

      expect(helper.byTestId(`counter-for${pipelineRunInfo.pipelineId()}`)).toBeInDOM();
      expect(helper.byTestId(`counter-for${pipelineRunInfo.pipelineId()}`)).toHaveText("unknown");
    });

    it("should render vsm as text instead of link", () => {
      mount(pipelineRunInfo);

      expect(helper.byTestId(`vsm-for${pipelineRunInfo.pipelineId()}`)).toBeInDOM();
      expect(helper.byTestId(`vsm-for${pipelineRunInfo.pipelineId()}`)).toHaveText("VSM");
      expect(helper.byTestId(`vsm-for${pipelineRunInfo.pipelineId()}`)).not.toHaveAttr("href");
    });

    it("should render time as N/A when timestamp on modification is null", () => {
      const pipelineActivityJSON              = PipelineActivityData.underConstruction();
      const pipelineRunInfoJSON               = pipelineActivityJSON.groups[0].history[0];
      pipelineRunInfoJSON.scheduled_timestamp = null;
      pipelineRunInfo                         = PipelineRunInfo.fromJSON(pipelineRunInfoJSON);

      mount(pipelineRunInfo);

      expect(helper.byTestId(`time-for${pipelineRunInfo.pipelineId()}`)).toBeInDOM();
      expect(helper.byTestId(`time-for${pipelineRunInfo.pipelineId()}`)).toHaveText("N/A");
    });
  });


  describe("Stage status", () => {
    it("should render passed stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(passed("Test")));
      mount(pipelineRunInfo);

      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toHaveClass(styles.passed);
    });

    it("should render failed stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(failed("Test")));
      mount(pipelineRunInfo);

      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toHaveClass(styles.failed);
    });

    it("should render unknown stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unknown("Test")));
      mount(pipelineRunInfo);

      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toHaveClass(styles.unknown);
    });

    it("should render cancelled stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(cancelled("Test")));
      mount(pipelineRunInfo);

      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toHaveClass(styles.cancelled);
    });

    it("should render building stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(building("Test")));
      mount(pipelineRunInfo);

      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-test`)).toHaveClass(styles.building);
    });
  });

  it("should render multiple stages", () => {
    const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(passed("unit"), building("integration")));
    mount(pipelineRunInfo);

    expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-unit`)).toBeInDOM();
    expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-unit`)).toHaveClass(styles.passed);

    expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-integration`)).toBeInDOM();
    expect(helper.byTestId(`stage-status-${pipelineRunInfo.pipelineId()}-integration`)).toHaveClass(styles.building);
  });

  it("should turncate stage counter when it has more than 17 chars", () => {
    const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(passed("unit")));
    pipelineRunInfo.label("This is more then 17 letters as pipeline label");
    mount(pipelineRunInfo);

    expect(helper.byTestId(`counter-for-${pipelineRunInfo.pipelineId()}`)).toBeInDOM();
    expect(helper.byTestId(`counter-for-${pipelineRunInfo.pipelineId()}`)).toHaveText("This is more then");
  });

  describe("Stage approval icon", () => {
    it("should render icon before the stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        passed("unit", 1),
        building("integration", 2),
        unknown("release", 3))
      );
      const stageConfigs    = toStageConfigs(pipelineRunInfo.stages());
      makeStageManual(stageConfigs, "release");
      mount(pipelineRunInfo, stageConfigs);
      
      expect(helper.byTestId("approval-icon-integration-2")).toBeInDOM();
      expect(helper.byTestId("approval-icon-integration-2")).toHaveAttr("title", "Forward");

      expect(helper.byTestId("approval-icon-release-3")).toBeInDOM();
      expect(helper.byTestId("approval-icon-release-3")).toHaveAttr("title", "Step Forward");
    });

    it("should render disabled icon when can not run stage", () => {
      const releaseStage     = unknown("release", 3);
      releaseStage.getCanRun = false;
      const pipelineRunInfo  = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        passed("unit", 1),
        building("integration", 2),
        releaseStage
      ));
      mount(pipelineRunInfo);


      expect(helper.byTestId("approval-icon-integration-2")).toBeInDOM();
      expect(helper.byTestId("approval-icon-integration-2")).not.toBeDisabled();

      expect(helper.byTestId("approval-icon-release-3")).toBeInDOM();
      expect(helper.byTestId("approval-icon-release-3")).toBeDisabled();
    });
  });

  function mount(pipelineRunInfo: PipelineRunInfo, stageConfigs?: StageConfigs) {
    helper.mount(() => <PipelineRunWidget pipelineRunInfo={pipelineRunInfo}
                                          pipelineName={"up42"}
                                          showBuildCaseFor={showBuildCaseFor}
                                          stageConfigs={stageConfigs ? stageConfigs : toStageConfigs(pipelineRunInfo.stages())}
                                          service={service}/>)
  }

  function toStageConfigs(stages: Stages) {
    return new StageConfigs(...(stages.map((stage: Stage) => {
      return new StageConfig(stage.stageName(), true);
    })));
  }

  function makeStageManual(stageConfigs: StageConfigs, name: string) {
    const stage = stageConfigs.find((stage: StageConfig) => stage.name() === name);
    if (!stage) {
      throw new Error("Stage with name " + name + " not found!!")
    }

    stage.isAutoApproved(false);
  }
});
