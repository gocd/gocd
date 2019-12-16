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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineRunInfo, Stage, StageConfig, StageConfigs, Stages} from "models/pipeline_activity/pipeline_activity";
import {
  building,
  cancelled,
  failed,
  passed,
  PipelineActivityData,
  unknown
} from "models/pipeline_activity/spec/test_data";
import {TestHelper} from "../../spec/test_helper";
import styles from "../index.scss";
import {PipelineRunWidget} from "../pipeline_run_info_widget";

describe("PipelineRunInfoWidget", () => {
  const helper              = new TestHelper();
  const showBuildCauseFor   = Stream<string>();
  const showCommentFor      = Stream<string>();
  const cancelStageInstance = jasmine.createSpy("cancelStageInstance");
  const runStage            = jasmine.createSpy("runStage");
  const addOrUpdateComment  = jasmine.createSpy("addOrUpdateComment");

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

  describe("Stage gate icons", () => {

    describe("Gate title", () => {
      it("should be Auto approved when pipeline is triggered by changes", () => {
        const integrationStage      = building("integration", 2);
        integrationStage.approvedBy = "changes";
        const pipelineRunInfo       = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
          passed("unit", 1),
          integrationStage
        ));
        const stageConfigs          = toStageConfigs(pipelineRunInfo.stages());
        mount(pipelineRunInfo, stageConfigs);

        expect(helper.byTestId("auto-gate-icon-integration-2")).toBeInDOM();
        expect(helper.byTestId("auto-gate-icon-integration-2")).toHaveAttr("title", "Automatically approved");
      });

      it("should be awaiting for approval when stage is not run yet", () => {
        const integrationStage      = building("integration", 2);
        integrationStage.approvedBy = "";
        const pipelineRunInfo       = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
          passed("unit", 1),
          integrationStage
        ));
        const stageConfigs          = toStageConfigs(pipelineRunInfo.stages());
        mount(pipelineRunInfo, stageConfigs);

        expect(helper.byTestId("auto-gate-icon-integration-2")).toBeInDOM();
        expect(helper.byTestId("auto-gate-icon-integration-2")).toHaveAttr("title", "Awaiting approval");
      });

      it("should be approved by user when stage manually approved by some user", () => {
        const integrationStage      = building("integration", 2);
        integrationStage.approvedBy = "Bob";
        const pipelineRunInfo       = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
          passed("unit", 1),
          integrationStage
        ));
        const stageConfigs          = toStageConfigs(pipelineRunInfo.stages());
        mount(pipelineRunInfo, stageConfigs);

        expect(helper.byTestId("auto-gate-icon-integration-2")).toBeInDOM();
        expect(helper.byTestId("auto-gate-icon-integration-2")).toHaveAttr("title", "Approved by Bob");
      });

      it("should be error message if stage has any", () => {
        const integrationStage        = building("integration", 2);
        integrationStage.approvedBy   = "changes";
        integrationStage.errorMessage = "Can not schedule stage as previous stage is failed";
        const pipelineRunInfo         = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
          passed("unit", 1),
          integrationStage
        ));
        const stageConfigs            = toStageConfigs(pipelineRunInfo.stages());
        mount(pipelineRunInfo, stageConfigs);

        expect(helper.byTestId("auto-gate-icon-integration-2")).toBeInDOM();
        expect(helper.byTestId("auto-gate-icon-integration-2")).toHaveAttr("title", "Can not schedule stage as previous stage is failed");
      });
    });

    it("should render gate icon before the stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        passed("unit", 1),
        building("integration", 2),
        unknown("release", 3))
      );
      const stageConfigs    = toStageConfigs(pipelineRunInfo.stages());
      makeStageManual(stageConfigs, "release");
      mount(pipelineRunInfo, stageConfigs);

      expect(helper.byTestId("auto-gate-icon-integration-2")).toBeInDOM();

      expect(helper.byTestId("manual-gate-icon-release-3")).toBeInDOM();
    });

    it("should render disabled gate icon when can not run stage", () => {
      const releaseStage     = unknown("release", 3);
      releaseStage.getCanRun = false;
      const pipelineRunInfo  = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        passed("unit", 1),
        building("integration", 2),
        releaseStage
      ));
      const stageConfigs     = toStageConfigs(pipelineRunInfo.stages());
      makeStageManual(stageConfigs, "release");
      mount(pipelineRunInfo, stageConfigs);

      expect(helper.byTestId("auto-gate-icon-integration-2")).toBeInDOM();
      expect(helper.byTestId("auto-gate-icon-integration-2")).not.toBeDisabled();

      expect(helper.byTestId("manual-gate-icon-release-3")).toBeInDOM();
      expect(helper.byTestId("manual-gate-icon-release-3")).toBeDisabled();
    });

    it("should render disabled gate icon when can stage is already scheduled", () => {
      const releaseStage     = unknown("release", 3);
      releaseStage.getCanRun = true;
      releaseStage.scheduled = true;
      const pipelineRunInfo  = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        passed("unit", 1),
        building("integration", 2),
        releaseStage
      ));
      const stageConfigs     = toStageConfigs(pipelineRunInfo.stages());
      makeStageManual(stageConfigs, "release");
      mount(pipelineRunInfo, stageConfigs);

      expect(helper.byTestId("auto-gate-icon-integration-2")).toBeInDOM();
      expect(helper.byTestId("auto-gate-icon-integration-2")).not.toBeDisabled();
      expect(helper.byTestId("stage-status-wrapper-42-integration")).toBeInDOM();
      expect(helper.byTestId("stage-status-wrapper-42-integration")).not.toHaveClass(styles.disabledIcon);

      expect(helper.byTestId("manual-gate-icon-release-3")).toBeInDOM();
      expect(helper.byTestId("manual-gate-icon-release-3")).toBeDisabled();
      expect(helper.byTestId("stage-status-wrapper-42-release")).toBeInDOM();
      expect(helper.byTestId("stage-status-wrapper-42-release")).toHaveClass(styles.disabledIcon);
    });

    it("should run stage on click of manual gate", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        passed("unit", 1),
        unknown("release", 2))
      );
      const stageConfigs    = toStageConfigs(pipelineRunInfo.stages());
      makeStageManual(stageConfigs, "release");
      mount(pipelineRunInfo, stageConfigs);

      helper.clickByTestId("manual-gate-icon-release-2");
      expect(runStage).toHaveBeenCalled();
    });

    it("should run stage on click of auto approval gate when previous stage is failed", () => {
      const nextStage       = unknown("integration", 2);
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        failed("unit", 1),
        nextStage
      ));
      mount(pipelineRunInfo);

      helper.clickByTestId("auto-gate-icon-integration-2");

      expect(runStage).toHaveBeenCalled();
    });
  });

  describe("Stage actions", () => {
    it("should not have any action when it is not scheduled", () => {
      const unitTestStage     = passed("unit", 2);
      unitTestStage.scheduled = false;
      const pipelineRunInfo   = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));
      mount(pipelineRunInfo);

      expect(helper.byTestId("info-stage-action-icon-unit-2")).not.toBeInDOM();
      expect(helper.byTestId("rerun-stage-action-icon-unit-2")).not.toBeInDOM();
      expect(helper.byTestId("cancel-stage-action-icon-unit-2")).not.toBeInDOM();
    });

    it("should have only info action when it is scheduled but can not perform rerun or cancel", () => {
      const unitTestStage        = passed("unit", 2);
      unitTestStage.scheduled    = true;
      unitTestStage.getCanRun    = false;
      unitTestStage.getCanCancel = false;
      const pipelineRunInfo      = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));

      mount(pipelineRunInfo);

      const infoActionIcon = helper.byTestId("info-stage-action-icon-unit-2");
      expect(infoActionIcon).toBeInDOM();
      expect(infoActionIcon).toBeHidden();
      expect(infoActionIcon.parentElement).toHaveAttr("target", "_blank");
      expect(infoActionIcon.parentElement).toHaveAttr("href", `/go/pipelines/${unitTestStage.stageLocator}`);

      expect(helper.byTestId("rerun-stage-action-icon-unit-2")).not.toBeInDOM();
      expect(helper.byTestId("cancel-stage-action-icon-unit-2")).not.toBeInDOM();
    });

    it("should have only info and cancel action when it is scheduled and user can cancel", () => {
      const unitTestStage        = passed("unit", 2);
      unitTestStage.scheduled    = true;
      unitTestStage.getCanRun    = false;
      unitTestStage.getCanCancel = true;
      const pipelineRunInfo      = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));
      mount(pipelineRunInfo);

      expect(helper.byTestId("info-stage-action-icon-unit-2")).toBeInDOM();
      expect(helper.byTestId("info-stage-action-icon-unit-2")).toBeHidden();

      expect(helper.byTestId("cancel-stage-action-icon-unit-2")).toBeInDOM();
      expect(helper.byTestId("cancel-stage-action-icon-unit-2")).toBeHidden();

      expect(helper.byTestId("rerun-stage-action-icon-unit-2")).not.toBeInDOM();
    });

    it("should have only info and rerun action when it is scheduled and user can rerun ", () => {
      const unitTestStage        = passed("unit", 2);
      unitTestStage.scheduled    = true;
      unitTestStage.getCanRun    = true;
      unitTestStage.getCanCancel = false;
      const pipelineRunInfo      = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));
      mount(pipelineRunInfo);

      expect(helper.byTestId("info-stage-action-icon-unit-2")).toBeInDOM();
      expect(helper.byTestId("info-stage-action-icon-unit-2")).toBeHidden();

      expect(helper.byTestId("rerun-stage-action-icon-unit-2")).toBeInDOM();
      expect(helper.byTestId("rerun-stage-action-icon-unit-2")).toBeHidden();

      expect(helper.byTestId("cancel-stage-action-icon-unit-2")).not.toBeInDOM();
    });
  });

  function mount(pipelineRunInfo: PipelineRunInfo, stageConfigs?: StageConfigs) {
    helper.mount(() => <PipelineRunWidget pipelineRunInfo={pipelineRunInfo}
                                          pipelineName={"up42"}
                                          showBuildCaseFor={showBuildCauseFor}
                                          showCommentFor={showCommentFor}
                                          stageConfigs={stageConfigs ? stageConfigs : toStageConfigs(pipelineRunInfo.stages())}
                                          cancelStageInstance={cancelStageInstance}
                                          addOrUpdateComment={addOrUpdateComment}
                                          canOperatePipeline={false}
                                          runStage={runStage}/>);
  }

  function toStageConfigs(stages: Stages) {
    return new StageConfigs(...(stages.map((stage: Stage) => {
      return new StageConfig(stage.stageName(), true);
    })));
  }

  function makeStageManual(stageConfigs: StageConfigs, name: string) {
    const stage = stageConfigs.find((stage: StageConfig) => stage.name() === name);
    if (!stage) {
      throw new Error("Stage with name " + name + " not found!!");
    }

    stage.isAutoApproved(false);
  }
});
