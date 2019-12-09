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
import {SparkRoutes} from "helpers/spark_routes";
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityData} from "models/pipeline_activity/spec/test_data";
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

    expect(helper.byTestId("counter-for-1")).toHaveText("unknown");

    expect(helper.byTestId("vsm-for-1")).toHaveText("VSM");
    expect(helper.q("span", helper.byTestId("vsm-for-1"))).toHaveClass(styles.disabled);

    expect(helper.byClass(styles.revision)).toContainText("Revision:");

    expect(helper.byTestId("time-for-1")).toHaveText("N/A");
    expect(helper.byTestId("trigger-with-changes-button")).not.toBeInDOM();
  });

  it("should render pipeline runs for an old pipeline", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    mount(new PipelineActivityPageWrapper(activity));

    const history = activity.groups()[0].history()[0];
    expect(helper.byTestId("counter-for-42")).toHaveText(history.label());
    expect(helper.byTestId("vsm-for-42")).toHaveText("VSM");
    expect(helper.q("a", helper.byTestId("vsm-for-42"))).toHaveAttr("href", "/go/pipelines/value_stream_map/up43/1");
    expect(helper.byClass(styles.revision)).toContainText(`Revision: ${history.revision()}`);
    expect(helper.byTestId("trigger-with-changes-button")).toBeInDOM();
    expect(helper.byTestId("trigger-with-changes-button")).toHaveText("Triggered by changes");
  });

  it("should render search field", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    mount(new PipelineActivityPageWrapper(activity));

    expect(helper.byTestId("search-field")).toBeInDOM();
  });

  describe("Pause", () => {
    it("should show dialog on click of pause button", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
      activity.paused(false);
      activity.canPause(true);

      mount(new PipelineActivityPageWrapper(activity));

      helper.clickByTestId("page-header-pause-btn");

      const modal = helper.modal();
      expect(helper.byTestId("form-field-label-specify-the-reason-why-you-want-to-stop-scheduling-on-this-pipeline", modal))
        .toHaveText("Specify the reason why you want to stop scheduling on this pipeline");
      expect(helper.byTestId("pause-pipeline-textarea", modal)).toBeInDOM();
    });

    it("should make pipeline pause request with cause", (done) => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
      activity.paused(false);
      activity.canPause(true);

      mount(new PipelineActivityPageWrapper(activity));

      helper.clickByTestId("page-header-pause-btn");

      const modal = helper.modal();
      helper.oninput(helper.byTestId("pause-pipeline-textarea", modal), "This is a pause cause");
      helper.clickByTestId("primary-action-button", modal);

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(SparkRoutes.pipelinePausePath(activity.pipelineName()));
      expect(request.method).toEqual("POST");
      expect(request.data()).toEqual({pause_cause: 'This is a pause cause'});
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
      done();
    });
  });

  describe("Unpause", () => {
    it("should unpause the pipeline on click of unpause button", () => {
      const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
      activity.paused(true);
      activity.canPause(true);

      mount(new PipelineActivityPageWrapper(activity));

      helper.clickByTestId("page-header-unpause-btn");

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(SparkRoutes.pipelineUnpausePath(activity.pipelineName()));
      expect(request.method).toEqual("POST");
      expect(request.data()).toEqual({});
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    });
  });

  function mount(page: PipelineActivityPageWrapper) {
    helper.mountPage(() => page);
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

  fetchPipelineHistory(offset: number) {
    this.pipelineActivity(this._activity);
  }
}
