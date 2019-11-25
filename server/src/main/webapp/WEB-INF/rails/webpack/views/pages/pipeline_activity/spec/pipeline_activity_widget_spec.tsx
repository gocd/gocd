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
import {TestHelper} from "../../spec/test_helper";
import {PipelineActivityWidget} from "../pipeline_activity_widget";
import Stream from "mithril/stream";
import {
  building,
  cancelled,
  failed,
  passed,
  PipelineActivityData,
  unknown
} from "models/pipeline_activity/spec/test_data";
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import styles from "../index.scss";

describe("PipelineActivityWidget", () => {
  const helper           = new TestHelper();
  const showBuildCaseFor = Stream<string>();

  afterEach(helper.unmount.bind(helper));

  describe("Pipeline has no run", () => {
    it("should render header", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
      mount(activity);

      expect(helper.byTestId("instance-header")).toBeInDOM();
      expect(helper.byTestId("instance-header")).toHaveText("Instance");
    });

    it("should render counter as unknown", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
      mount(activity);

      const history = activity.groups()[0].history()[0];
      expect(helper.byTestId(`counter-for${history.pipelineId()}`)).toBeInDOM();
      expect(helper.byTestId(`counter-for${history.pipelineId()}`)).toHaveText("unknown");
    });

    it("should render vsm as text instead of link", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
      mount(activity);

      const history = activity.groups()[0].history()[0];
      expect(helper.byTestId(`vsm-for${history.pipelineId()}`)).toBeInDOM();
      expect(helper.byTestId(`vsm-for${history.pipelineId()}`)).toHaveText("VSM");
      expect(helper.byTestId(`vsm-for${history.pipelineId()}`)).not.toHaveAttr("href");
    });

    it("should render stage name", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
      mount(activity);

      expect(helper.byTestId("stage-foo")).toBeInDOM();
      expect(helper.byTestId("stage-foo")).toHaveText("foo");
    });
  });

  it("should render time as N/A when timestamp on modification is null", () => {
    const pipelineActivityJSON                                    = PipelineActivityData.underConstruction();
    pipelineActivityJSON.groups[0].history[0].scheduled_timestamp = null;
    const activity                                                = PipelineActivity.fromJSON(pipelineActivityJSON);
    mount(activity);

    const history = activity.groups()[0].history()[0];
    expect(helper.byTestId(`time-for${history.pipelineId()}`)).toBeInDOM();
    expect(helper.byTestId(`time-for${history.pipelineId()}`)).toHaveText("N/A");
  });

  describe("Stage status", () => {
    it("should render passed stage", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(passed("Test")));
      mount(activity);

      const history = activity.groups()[0].history()[0];
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toHaveClass(styles.passed);
    });

    it("should render failed stage", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(failed("Test")));
      mount(activity);

      const history = activity.groups()[0].history()[0];
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toHaveClass(styles.failed);
    });

    it("should render unknown stage", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(unknown("Test")));
      mount(activity);

      const history = activity.groups()[0].history()[0];
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toHaveClass(styles.unknown);
    });

    it("should render cancelled stage", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(cancelled("Test")));
      mount(activity);

      const history = activity.groups()[0].history()[0];
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toHaveClass(styles.cancelled);
    });

    it("should render building stage", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(building("Test")));
      mount(activity);

      const history = activity.groups()[0].history()[0];
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toBeInDOM();
      expect(helper.byTestId(`stage-status-${history.pipelineId()}-test`)).toHaveClass(styles.building);
    });
  });

  it("should render multiple stages", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(passed("unit"), building("integration")));
    mount(activity);

    const history = activity.groups()[0].history()[0];
    expect(helper.byTestId(`stage-status-${history.pipelineId()}-unit`)).toBeInDOM();
    expect(helper.byTestId(`stage-status-${history.pipelineId()}-unit`)).toHaveClass(styles.passed);

    expect(helper.byTestId(`stage-status-${history.pipelineId()}-integration`)).toBeInDOM();
    expect(helper.byTestId(`stage-status-${history.pipelineId()}-integration`)).toHaveClass(styles.building);
  });

  it("should turncate stage counter when it has more than 17 chars", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(passed("unit")));
    const history  = activity.groups()[0].history()[0];
    history.label("This is more then 17 letters as pipeline label");
    mount(activity);

    expect(helper.byTestId(`counter-for-${history.pipelineId()}`)).toBeInDOM();
    expect(helper.byTestId(`counter-for-${history.pipelineId()}`)).toHaveText("This is more then");
  });


  describe("Stage approval icon", () => {
    it("should render icon before the stage", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.withStages(
        passed("unit", 1),
        building("integration", 2),
        unknown("release", 3))
      );
      makeStageManual(activity, "release");
      mount(activity);

      expect(helper.byTestId("approval-icon-2")).toBeInDOM();
      expect(helper.byTestId("approval-icon-2")).toHaveAttr("title", "Forward");

      expect(helper.byTestId("approval-icon-3")).toBeInDOM();
      expect(helper.byTestId("approval-icon-3")).toHaveAttr("title", "Step Forward");
    });

    it("should render disabled icon when can not run stage", () => {
      const releaseStage     = unknown("release", 3);
      releaseStage.getCanRun = false;
      const activity         = PipelineActivity.fromJSON(PipelineActivityData.withStages(
        passed("unit", 1),
        building("integration", 2),
        releaseStage
      ));
      mount(activity);


      expect(helper.byTestId("approval-icon-2")).toBeInDOM();
      expect(helper.byTestId("approval-icon-2")).not.toBeDisabled();

      expect(helper.byTestId("approval-icon-3")).toBeInDOM();
      expect(helper.byTestId("approval-icon-3")).toBeDisabled();
    });
  });

  function mount(activity: PipelineActivity) {
    helper.mount(() => <PipelineActivityWidget pipelineActivity={Stream(activity)}
                                               showBuildCaseFor={showBuildCaseFor}/>)
  }

  function makeStageManual(activity: PipelineActivity, name: string) {
    const stage = activity.groups()[0].config().stages().find((stage) => stage.name() === name)
    if (!stage) {
      throw new Error("Stage with name " + name + " not found!!")
    }

    stage.isAutoApproved(false);
  }
});
