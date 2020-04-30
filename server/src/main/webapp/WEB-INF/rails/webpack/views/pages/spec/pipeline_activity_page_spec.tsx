/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {SparkRoutes} from "helpers/spark_routes";
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {passed, PipelineActivityData} from "models/pipeline_activity/spec/test_data";
import {ModalManager} from "views/components/modal/modal_manager";
import {PageState} from "../page";
import {PipelineActivityPage} from "../pipeline_activity";
import styles from "../pipeline_activity/index.scss";
import {TestHelper} from "./test_helper";

describe("PipelineActivityPage", () => {
  const helper = new TestHelper();

  beforeEach(() => {
    jasmine.Ajax.install();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    helper.unmount();
    ModalManager.closeAll();
  });

  it("should render pipeline activity for new pipeline", () => {
    mount(new PipelineActivityPageWrapper(PipelineActivity.fromJSON(PipelineActivityData.underConstruction())));

    expect(helper.byTestId("counter")).toHaveText("unknown");

    expect(helper.byTestId("vsm")).toHaveText("VSM");
    expect(helper.q("span", helper.byTestId("vsm"))).toHaveClass(styles.disabled);

    expect(helper.byClass(styles.revision)).toContainText("Revision:");

    expect(helper.byTestId("time")).toHaveText("N/A");
    expect(helper.byTestId("trigger-with-changes-button")).not.toBeInDOM();
  });

  it("should render pipeline runs for an old pipeline", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    mount(new PipelineActivityPageWrapper(activity));

    const history = activity.groups()[0].history()[0];
    expect(helper.byTestId("counter")).toHaveText(history.label());
    expect(helper.byTestId("vsm")).toHaveText("VSM");
    expect(helper.q("a", helper.byTestId("vsm"))).toHaveAttr("href", "/go/pipelines/value_stream_map/up43/1");
    expect(helper.byClass(styles.revision)).toContainText(`Revision: ${history.revision()}`);
    expect(helper.byTestId("trigger-with-changes-button")).toBeInDOM();
    expect(helper.byTestId("trigger-with-changes-button")).toHaveText("Triggered by changes");
  });

  it("should render search field", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    mount(new PipelineActivityPageWrapper(activity));

    expect(helper.byTestId("search-field")).toBeInDOM();
  });

  it("should update the pagination when pageChangeCallback called", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    const page     = new PipelineActivityPageWrapper(activity);
    mount(page);

    page.pageChangeCallback(2);
    expect(page.offset()).toEqual(10);

    page.pageChangeCallback(3);
    expect(page.offset()).toEqual(20);
  });

  describe("ForceTrigger", () => {
    it("should show force trigger icon", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
      activity.canForce(true);
      activity.showForceBuildButton(true);
      mount(new PipelineActivityPageWrapper(activity));

      helper.clickByTestId("force-trigger-pipeline");

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(SparkRoutes.pipelineTriggerPath(activity.pipelineName()));
      expect(request.method).toEqual("POST");
      expect(request.data()).toEqual({});
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    });
  });

  describe("ManualGate", () => {
    it("should show confirmation dialog", () => {
      const pipelineName = "up42";
      const activity     = PipelineActivity.fromJSON(PipelineActivityData.withStages(pipelineName,
                                                                                     passed("UnitTest", 1),
                                                                                     PipelineActivityData.stage(2,
                                                                                                                "Deploy",
                                                                                                                "Unknown",
                                                                                                                2,
                                                                                                                pipelineName,
                                                                                                                "1")));
      activity.groups()[0].config().stages()[1].isAutoApproved(false);

      mount(new PipelineActivityPageWrapper(activity));

      const unitStageContainer = stageContainer(activity.groups()[0].history()[0].label(), "deploy");
      helper.clickByTestId("gate-icon", unitStageContainer);
      const modal = helper.modal();
      helper.clickByTestId("primary-action-button", modal);

      const manualStage = activity.groups()[0].history()[0].stages()[1];
      const request     = jasmine.Ajax.requests.mostRecent();
      expect(request.url)
        .toEqual(SparkRoutes.runStage(activity.pipelineName(), manualStage.pipelineCounter(), manualStage.stageName()));
      expect(request.method).toEqual("POST");
      expect(request.data()).toEqual({});
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    });
  });

  function mount(page: PipelineActivityPageWrapper) {
    helper.mountPage(() => page);
  }

  function stageContainer(label: string, stageName: string) {
    return helper.byTestId(`stage-status-container-${stageName}`, helper.byTestId(`pipeline-instance-${label}`));
  }
});

class PipelineActivityPageWrapper extends PipelineActivityPage {
  private readonly _activity: PipelineActivity;

  constructor(activity: PipelineActivity) {
    super();
    this._activity = activity;
    this.pageState = PageState.OK;
    //@ts-ignore
    this.meta      = {pipelineName: activity.pipelineName()};
  }

  offset() {
    return this.pagination().offset;
  }

  protected fetchPipelineHistory(offset: number = this.pagination().offset): Promise<void> {
    this.pipelineActivity(this._activity);
    return Promise.resolve();
  }
}
