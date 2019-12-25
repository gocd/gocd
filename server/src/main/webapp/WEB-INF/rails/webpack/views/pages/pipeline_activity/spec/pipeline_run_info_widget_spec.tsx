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
  failing,
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

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("counter", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("counter", pipelineRunContainer)).toHaveText("unknown");
    });

    it("should render vsm as text instead of link", () => {
      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("vsm", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("vsm", pipelineRunContainer)).toHaveText("VSM");
      expect(helper.byTestId("vsm", pipelineRunContainer)).not.toHaveAttr("href");
    });

    it("should render time as N/A when timestamp on modification is null", () => {
      const pipelineActivityJSON              = PipelineActivityData.underConstruction();
      const pipelineRunInfoJSON               = pipelineActivityJSON.groups[0].history[0];
      pipelineRunInfoJSON.scheduled_timestamp = null;
      pipelineRunInfo                         = PipelineRunInfo.fromJSON(pipelineRunInfoJSON);

      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("time", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("time", pipelineRunContainer)).toHaveText("N/A");
    });
  });

  describe("Stage status", () => {
    it("should render passed stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(passed("Test")));
      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toHaveClass(styles.passed);
    });

    it("should render failed stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(failed("Test")));
      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toHaveClass(styles.failed);
    });

    it("should render unknown stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unknown("Test")));
      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toHaveClass(styles.unknown);
    });

    it("should render cancelled stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(cancelled("Test")));
      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toHaveClass(styles.cancelled);
    });

    it("should render building stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(building("Test")));
      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toHaveClass(styles.building);
    });

    it("should render failing stage", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(failing("Test")));
      mount(pipelineRunInfo);

      const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toBeInDOM();
      expect(helper.byTestId("stage-status-test", pipelineRunContainer)).toHaveClass(styles.failing);
    });
  });

  it("should render multiple stages", () => {
    const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(passed("unit"), building("integration")));
    mount(pipelineRunInfo);

    const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
    expect(helper.byTestId("stage-status-unit", pipelineRunContainer)).toBeInDOM();
    expect(helper.byTestId("stage-status-unit", pipelineRunContainer)).toHaveClass(styles.passed);

    expect(helper.byTestId("stage-status-integration", pipelineRunContainer)).toBeInDOM();
    expect(helper.byTestId("stage-status-integration", pipelineRunContainer)).toHaveClass(styles.building);
  });

  it("should turncate stage counter when it has more than 17 chars", () => {
    const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(passed("unit")));
    pipelineRunInfo.label("This is more then 17 letters as pipeline label");
    mount(pipelineRunInfo);

    const pipelineRunContainer = helper.byTestId(`pipeline-instance-${pipelineRunInfo.label()}`);
    expect(helper.byTestId("counter", pipelineRunContainer)).toBeInDOM();
    expect(helper.byTestId("counter", pipelineRunContainer)).toHaveText("This is more then");
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

        const stageStatusContainer = stageContainer(pipelineRunInfo.label(), "integration");
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toBeInDOM();
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toHaveAttr("title", "Automatically approved");
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

        const stageStatusContainer = stageContainer(pipelineRunInfo.label(), "integration");
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toBeInDOM();
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toHaveAttr("title", "Awaiting approval");
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

        const stageStatusContainer = stageContainer(pipelineRunInfo.label(), "integration");
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toBeInDOM();
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toHaveAttr("title", "Approved by Bob");
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

        const stageStatusContainer = stageContainer(pipelineRunInfo.label(), "integration");
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toBeInDOM();
        expect(helper.byTestId("gate-icon", stageStatusContainer)).toHaveAttr("title", "Can not schedule stage as previous stage is failed");
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

      const integrationStageContainer = stageContainer(pipelineRunInfo.label(), "integration");
      expect(helper.byTestId("gate-icon", integrationStageContainer)).toBeInDOM();

      const releaseStageContainer = stageContainer(pipelineRunInfo.label(), "release");
      expect(helper.byTestId("gate-icon", releaseStageContainer)).toBeInDOM();
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

      const integrationStageContainer = stageContainer(pipelineRunInfo.label(), "integration");
      expect(helper.byTestId("gate-icon", integrationStageContainer)).toBeInDOM();
      expect(helper.byTestId("gate-icon", integrationStageContainer)).not.toBeDisabled();

      const releaseStageContainer = stageContainer(pipelineRunInfo.label(), "release");
      expect(helper.byTestId("gate-icon", releaseStageContainer)).toBeInDOM();
      expect(helper.byTestId("gate-icon", releaseStageContainer)).toBeDisabled();
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

      const integrationStageContainer = stageContainer(pipelineRunInfo.label(), "integration");
      expect(helper.byTestId("gate-icon", integrationStageContainer)).toBeInDOM();
      expect(helper.byTestId("gate-icon", integrationStageContainer)).not.toBeDisabled();
      expect(integrationStageContainer).toBeInDOM();
      expect(integrationStageContainer).not.toHaveClass(styles.disabledIcon);

      const releaseStageContainer = stageContainer(pipelineRunInfo.label(), "release");
      expect(helper.byTestId("gate-icon", releaseStageContainer)).toBeInDOM();
      expect(helper.byTestId("gate-icon", releaseStageContainer)).toBeDisabled();
      expect(releaseStageContainer).toBeInDOM();
      expect(releaseStageContainer).toHaveClass(styles.disabledIcon);
    });

    it("should run stage on click of manual gate", () => {
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        passed("unit", 1),
        unknown("release", 2))
      );
      const stageConfigs    = toStageConfigs(pipelineRunInfo.stages());
      makeStageManual(stageConfigs, "release");
      mount(pipelineRunInfo, stageConfigs);

      const releaseStageContainer = stageContainer(pipelineRunInfo.label(), "release");
      helper.clickByTestId("gate-icon", releaseStageContainer);
      expect(runStage).toHaveBeenCalled();
    });

    it("should run stage on click of auto approval gate when previous stage is failed", () => {
      const nextStage       = unknown("integration", 2);
      const pipelineRunInfo = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(
        failed("unit", 1),
        nextStage
      ));
      mount(pipelineRunInfo);

      const integrationStageContainer = stageContainer(pipelineRunInfo.label(), "integration");
      helper.clickByTestId("gate-icon", integrationStageContainer);

      expect(runStage).toHaveBeenCalled();
    });
  });

  describe("Stage actions", () => {
    it("should not have any action when it is not scheduled", () => {
      const unitTestStage     = passed("unit", 2);
      unitTestStage.scheduled = false;
      const pipelineRunInfo   = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));
      mount(pipelineRunInfo);

      const unitStageContainer = stageContainer(pipelineRunInfo.label(), "unit");
      expect(helper.byTestId("stage-info-icon", unitStageContainer)).not.toBeInDOM();
      expect(helper.byTestId("rerun-stage-icon", unitStageContainer)).not.toBeInDOM();
      expect(helper.byTestId("cancel-stage-icon", unitStageContainer)).not.toBeInDOM();
    });

    it("should have only info action when it is scheduled but can not perform rerun or cancel", () => {
      const unitTestStage        = passed("unit", 2);
      unitTestStage.scheduled    = true;
      unitTestStage.getCanRun    = false;
      unitTestStage.getCanCancel = false;
      const pipelineRunInfo      = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));

      mount(pipelineRunInfo);

      const unitStageContainer = stageContainer(pipelineRunInfo.label(), "unit");
      const infoActionIcon     = helper.byTestId("stage-info-icon", unitStageContainer);
      expect(infoActionIcon).toBeInDOM();
      expect(infoActionIcon).toBeHidden();
      expect(infoActionIcon.parentElement).toHaveAttr("target", "_blank");
      expect(infoActionIcon.parentElement).toHaveAttr("href", `/go/pipelines/${unitTestStage.stageLocator}`);

      expect(helper.byTestId("rerun-stage-icon", unitStageContainer)).not.toBeInDOM();
      expect(helper.byTestId("cancel-stage-icon", unitStageContainer)).not.toBeInDOM();
    });

    it("should have only info and cancel action when it is scheduled and user can cancel", () => {
      const unitTestStage        = passed("unit", 2);
      unitTestStage.scheduled    = true;
      unitTestStage.getCanRun    = false;
      unitTestStage.getCanCancel = true;
      const pipelineRunInfo      = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));
      mount(pipelineRunInfo);

      const unitStageContainer = stageContainer(pipelineRunInfo.label(), "unit");
      expect(helper.byTestId("stage-info-icon", unitStageContainer)).toBeInDOM();
      expect(helper.byTestId("stage-info-icon", unitStageContainer)).toBeHidden();

      expect(helper.byTestId("cancel-stage-icon", unitStageContainer)).toBeInDOM();
      expect(helper.byTestId("cancel-stage-icon", unitStageContainer)).toBeHidden();

      expect(helper.byTestId("rerun-stage-icon", unitStageContainer)).not.toBeInDOM();
    });

    it("should have only info and rerun action when it is scheduled and user can rerun ", () => {
      const unitTestStage        = passed("unit", 2);
      unitTestStage.scheduled    = true;
      unitTestStage.getCanRun    = true;
      unitTestStage.getCanCancel = false;
      const pipelineRunInfo      = PipelineRunInfo.fromJSON(PipelineActivityData.pipelineRunInfo(unitTestStage));
      mount(pipelineRunInfo);

      const unitStageContainer = stageContainer(pipelineRunInfo.label(), "unit");
      expect(helper.byTestId("stage-info-icon", unitStageContainer)).toBeInDOM();
      expect(helper.byTestId("stage-info-icon", unitStageContainer)).toBeHidden();

      expect(helper.byTestId("rerun-stage-icon", unitStageContainer)).toBeInDOM();
      expect(helper.byTestId("rerun-stage-icon", unitStageContainer)).toBeHidden();

      expect(helper.byTestId("cancel-stage-icon", unitStageContainer)).not.toBeInDOM();
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

  function stageContainer(label: string, stageName: string) {
    return helper.byTestId(`stage-status-container-${stageName}`, helper.byTestId(`pipeline-instance-${label}`));
  }
});
