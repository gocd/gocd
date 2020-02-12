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

import m from "mithril";
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityData} from "models/pipeline_activity/spec/test_data";
import {TestHelper} from "../../spec/test_helper";
import {PipelineActivityHeader} from "../page_header";

describe("PipelineActivityHeader", () => {
  const helper = new TestHelper();
  let pauseFn: () => void, unpauseFn: () => void;

  beforeEach(() => {
    pauseFn   = jasmine.createSpy("pauseFn");
    unpauseFn = jasmine.createSpy("unpauseFn");
  });

  afterEach(helper.unmount.bind(helper));

  it("should render pipeline label and name", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    mount(pipelineActivity);

    expect(helper.byTestId("page-header-pipeline-label")).toBeInDOM();
    expect(helper.byTestId("page-header-pipeline-label")).toHaveText("Pipeline");

    expect(helper.byTestId("page-header-pipeline-name")).toBeInDOM();
    expect(helper.byTestId("page-header-pipeline-name")).toHaveText(pipelineActivity.pipelineName());
  });

  it("should render pause pipeline icon when pipeline is not paused", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    pipelineActivity.canPause(true);
    pipelineActivity.paused(false);
    mount(pipelineActivity);

    expect(helper.byTestId("page-header-pause-btn")).toBeInDOM();
    expect(helper.byTestId("page-header-unpause-btn")).not.toBeInDOM();
  });

  it("onclick of pause pipeline button it should call pauseFn", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    pipelineActivity.canPause(true);
    pipelineActivity.paused(false);
    mount(pipelineActivity);

    helper.clickByTestId("page-header-pause-btn");

    expect(pauseFn).toHaveBeenCalled();
    expect(unpauseFn).not.toHaveBeenCalled();
  });

  it("onclick of unpause pipeline button it should call unpauseFn", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    pipelineActivity.canPause(true);
    pipelineActivity.paused(true);
    mount(pipelineActivity);

    helper.clickByTestId("page-header-unpause-btn");

    expect(pauseFn).not.toHaveBeenCalled();
    expect(unpauseFn).toHaveBeenCalled();
  });

  it("should render unpause pipeline icon when pipeline is paused", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    pipelineActivity.canPause(true);
    pipelineActivity.paused(true);
    mount(pipelineActivity);

    expect(helper.byTestId("page-header-pause-btn")).not.toBeInDOM();
    expect(helper.byTestId("page-header-unpause-btn")).toBeInDOM();
  });

  it("should not render pause or unpause pipeline icon when can pause is set to false", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    pipelineActivity.canPause(false);
    mount(pipelineActivity);

    expect(helper.byTestId("page-header-pause-btn")).not.toBeInDOM();
    expect(helper.byTestId("page-header-unpause-btn")).not.toBeInDOM();
  });

  it("should not render pipeline settings icon when user is not admin or group admin", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    mount(pipelineActivity, false, false);

    expect(helper.byTestId("page-header-pipeline-settings")).not.toBeInDOM();
  });

  it("should render pipeline settings icon when user is admin", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    mount(pipelineActivity, true, false);

    expect(helper.byTestId("page-header-pipeline-settings")).toBeInDOM();
  });

  it("should render pipeline settings icon when user is group admin", () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    mount(pipelineActivity, false, true);

    expect(helper.byTestId("page-header-pipeline-settings")).toBeInDOM();
  });

  it('should not render pause message when pipeline is unpaused', () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    pipelineActivity.paused(false);

    mount(pipelineActivity, false, true);

    expect(helper.byTestId("pipeline-pause-message")).not.toBeInDOM();
  });

  it('should render pause message when pipeline is paused', () => {
    const pipelineActivity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());
    pipelineActivity.paused(true);
    pipelineActivity.pauseBy("Bob");
    pipelineActivity.pauseCause("Adding artifact config");

    mount(pipelineActivity, false, true);

    expect(helper.byTestId("pipeline-pause-message")).toBeInDOM();
    expect(helper.byTestId("pipeline-pause-message")).toContainText("Scheduling is paused by Bob (Adding artifact config)");
    expect(helper.byTestId("pipeline-pause-message")).toHaveAttr("title", "Scheduling is paused by Bob (Adding artifact config)");
  });

  function mount(pipelineActivity: PipelineActivity, isAdmin: boolean = true, isGroupAdmin: boolean = true) {
    helper.mount(() => <PipelineActivityHeader pausePipeline={pauseFn}
                                               unpausePipeline={unpauseFn}
                                               pipelineActivity={pipelineActivity}
                                               isAdmin={isAdmin}
                                               isGroupAdmin={isGroupAdmin}/>);
  }
});
