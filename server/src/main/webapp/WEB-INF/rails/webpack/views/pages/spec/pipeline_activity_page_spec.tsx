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

import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {PipelineActivityData} from "models/pipeline_activity/spec/test_data";
import {PageState} from "../page";
import {ResultAwarePage} from "../page_operations";
import {PipelineActivityPage} from "../pipeline_activity";
import styles from "../pipeline_activity/index.scss";
import {TestHelper} from "./test_helper";

describe("PipelineActivityPage", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render pipeline activity for new pipeline", () => {
    const stubbedActivities = (pipelineName: string, offset: number, filterText: string, page: ResultAwarePage<PipelineActivity>) => {
      page.onSuccess(PipelineActivity.fromJSON(PipelineActivityData.underConstruction()));
    };

    const service = new PipelineActivityService();
    spyOn(service, "activities").and.callFake(stubbedActivities);

    mount(new PipelineActivityPageWrapper(service));

    expect(helper.byTestId("counter-for-1")).toHaveText("unknown");

    expect(helper.byTestId("vsm-for-1")).toHaveText("VSM");
    expect(helper.q("span", helper.byTestId("vsm-for-1"))).toHaveClass(styles.disabled);

    expect(helper.byClass(styles.revision)).toContainText("Revision:");

    expect(helper.byTestId("time-for-1")).toHaveText("N/A");
    expect(helper.byTestId("trigger-with-changes-button")).not.toBeInDOM();
  });

  it("should render pipeline runs for an old pipeline", () => {
    const activity          = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    const stubbedActivities = (pipelineName: string, offset: number, filterText: string, page: ResultAwarePage<PipelineActivity>) => {
      page.onSuccess(activity);
    };

    const service = new PipelineActivityService();
    spyOn(service, "activities").and.callFake(stubbedActivities);

    mount(new PipelineActivityPageWrapper(service));

    const history = activity.groups()[0].history()[0];
    expect(helper.byTestId("counter-for-42")).toHaveText(history.label());
    expect(helper.byTestId("vsm-for-42")).toHaveText("VSM");
    expect(helper.q("a", helper.byTestId("vsm-for-42"))).toHaveAttr("href", "/go/pipelines/value_stream_map/up43/1");
    expect(helper.byClass(styles.revision)).toContainText(`Revision: ${history.revision()}`);
    expect(helper.byTestId("trigger-with-changes-button")).toBeInDOM();
    expect(helper.byTestId("trigger-with-changes-button")).toHaveText("Triggered by changes");
  });

  it("should render search field", () => {
    const activity          = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    const stubbedActivities = (pipelineName: string, offset: number, filterText: string, page: ResultAwarePage<PipelineActivity>) => {
      page.onSuccess(activity);
    };

    const service = new PipelineActivityService();
    spyOn(service, "activities").and.callFake(stubbedActivities);

    mount(new PipelineActivityPageWrapper(service));

    expect(helper.byTestId("search-field")).toBeInDOM();
  });

  function mount(page: PipelineActivityPageWrapper) {
    helper.mountPage(() => page);
  }
});

class PipelineActivityPageWrapper extends PipelineActivityPage {
  constructor(service: PipelineActivityService) {
    super();
    this.service   = service;
    this.pageState = PageState.OK;
  }
}
