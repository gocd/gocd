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

import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {TestHelper} from "./test_helper";
import {PipelineActivityPage} from "../pipeline_activity";
import {ResultAwarePage} from "../page_operations";
import {PipelineActivity} from "../../../models/pipeline_activity/pipeline_activity";
import {PipelineActivityData} from "../../../models/pipeline_activity/spec/test_data";
import {PageState} from "../page";
import styles from "../pipeline_activity/index.scss";

describe("PipelineActivityPage", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render pipeline activity for new pipeline", () => {
    const stubbedActivities = (pipelineName: string, offset: number, page: ResultAwarePage<PipelineActivity>) => {
      page.onSuccess(PipelineActivity.fromJSON(PipelineActivityData.underConstruction()))
    };

    const service = new PipelineActivityService();
    Stub.of(service).when("activities").thenCall(stubbedActivities);

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
    const stubbedActivities = (pipelineName: string, offset: number, page: ResultAwarePage<PipelineActivity>) => {
      page.onSuccess(activity)
    };

    const service = new PipelineActivityService();
    Stub.of(service).when("activities").thenCall(stubbedActivities);

    mount(new PipelineActivityPageWrapper(service));

    const history = activity.groups()[0].history()[0];
    expect(helper.byTestId("counter-for-42")).toHaveText(history.label());
    expect(helper.byTestId("vsm-for-42")).toHaveText("VSM");
    expect(helper.q("a", helper.byTestId("vsm-for-42"))).toHaveAttr("href", "/go/pipelines/value_stream_map/up43/1");
    expect(helper.byClass(styles.revision)).toContainText(`Revision: ${history.revision()}`);
    expect(helper.byTestId("trigger-with-changes-button")).toBeInDOM();
    expect(helper.byTestId("trigger-with-changes-button")).toHaveText("Triggered by changes");
  });

  function mount(page: PipelineActivityPageWrapper) {
    helper.mountPage(() => page)
  }
});

class PipelineActivityPageWrapper extends PipelineActivityPage {
  constructor(service: PipelineActivityService) {
    super();
    this.service   = service;
    this.pageState = PageState.OK;
  }
}


export class Binder<T extends object> {
  private readonly object: T;
  private readonly method: string;

  constructor(object: T, method: string) {
    this.object = object;
    this.method = method;
  }

  thenCall(proxy: Function) {
    if (!proxy) {
      throw new Error("Can not bind null function to object!");
    }

    //@ts-ignore
    if (typeof this.object[this.method] === "function") {
      //@ts-ignore
      this.object[this.method] = proxy;
      return;
    }

    throw new Error(`Function ${this.method} does not exist.`);
  }

}

class Stub<T extends object> {
  private readonly object: T;

  constructor(object: T) {
    this.object = object;
  }

  static of<T extends object>(object: T): Stub<T> {
    return new Stub(object);
  }

  when(method: string): Binder<T> {
    return new Binder<T>(this.object, method)
  }

  original(): T {
    return this.object;
  }
}
