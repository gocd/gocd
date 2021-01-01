/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import m from "mithril";
import {v4 as uuid4} from "uuid";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {ModalManager} from "views/components/modal/modal_manager";
import {PipelineStatus} from "views/pages/pipeline_activity/common/models/pipeline_status";
import {PipelinePauseHeader} from "views/pages/pipeline_activity/common/pipeline_pause_header";
import {TestHelper} from "../../../spec/test_helper";

describe("Pipeline Pause Header", () => {
  const helper        = new TestHelper();
  const PIPELINE_NAME = `pipeline_name_${uuid4()}`;

  beforeEach(() => {
    jasmine.Ajax.install();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    helper.unmount();
    ModalManager.closeAll();
  });

  it("should render pipeline label and name", () => {
    const status = new PipelineStatus(PIPELINE_NAME, false);
    mount(status);

    expect(helper.byTestId("page-header-pipeline-label")).toBeInDOM();
    expect(helper.byTestId("page-header-pipeline-label")).toHaveText("Pipeline");

    expect(helper.byTestId("page-header-pipeline-name")).toBeInDOM();
    expect(helper.byTestId("page-header-pipeline-name")).toHaveText(PIPELINE_NAME);
  });

  it("should render pause pipeline icon when pipeline is not paused", () => {
    const status = new PipelineStatus(PIPELINE_NAME, false);
    mount(status);

    expect(helper.byTestId("page-header-pause-btn")).toBeInDOM();
    expect(helper.byTestId("page-header-unpause-btn")).not.toBeInDOM();
  });

  it("should pause a pipeline", (done) => {
    const status = new PipelineStatus(PIPELINE_NAME, false);
    mount(status);

    helper.clickByTestId("page-header-pause-btn");

    const modal = helper.modal();
    helper.oninput(helper.byTestId("pause-pipeline-textarea", modal), "This is a pause cause");
    helper.clickByTestId("primary-action-button", modal);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(SparkRoutes.pipelinePausePath(PIPELINE_NAME));
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual({pause_cause: "This is a pause cause"});
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    done();
  });

  it("should unpause a pipeline", (done) => {
    const status = new PipelineStatus(PIPELINE_NAME, true);
    mount(status);

    helper.clickByTestId("page-header-unpause-btn");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(SparkRoutes.pipelineUnpausePath(PIPELINE_NAME));
    expect(request.method).toEqual("POST");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    done();
  });

  it("should render unpause pipeline icon when pipeline is paused", () => {
    const status = new PipelineStatus(PIPELINE_NAME, true);
    mount(status);

    expect(helper.byTestId("page-header-pause-btn")).not.toBeInDOM();
    expect(helper.byTestId("page-header-unpause-btn")).toBeInDOM();
  });

  it("should not render pause or unpause pipeline icon when can pause is set to false", () => {
    const status = new PipelineStatus(PIPELINE_NAME, false);
    mount(status, true, false);

    expect(helper.byTestId("page-header-pause-btn")).not.toBeInDOM();
    expect(helper.byTestId("page-header-unpause-btn")).not.toBeInDOM();
  });

  it("should not render pipeline settings icon when disabled", () => {
    const status = new PipelineStatus(PIPELINE_NAME, false);
    mount(status, false);

    expect(helper.byTestId("page-header-pipeline-settings")).not.toBeInDOM();
  });

  it("should render pipeline settings icon", () => {
    const status = new PipelineStatus(PIPELINE_NAME, false);
    mount(status, true);

    expect(helper.byTestId("page-header-pipeline-settings")).toBeInDOM();
  });

  it("should not render pause message when pipeline is unpaused", () => {
    const status = new PipelineStatus(PIPELINE_NAME, false);
    mount(status);

    expect(helper.byTestId("pipeline-pause-message")).not.toBeInDOM();
  });

  it("should render pause message when pipeline is paused", () => {
    const status = new PipelineStatus(PIPELINE_NAME, true, "Bob", "Adding artifact config");
    mount(status);

    const pauseMessage = "Scheduling is paused by Bob (Adding artifact config)";

    expect(helper.byTestId("pipeline-pause-message")).toBeInDOM();
    expect(helper.byTestId("pipeline-pause-message")).toContainText(pauseMessage);
    expect(helper.byTestId("pipeline-pause-message")).toHaveAttr("title", pauseMessage);
  });

  function mount(pipelineStatus: PipelineStatus,
                 shouldShowPipelineSettings: boolean = true,
                 shouldShowPauseUnpause: boolean     = true) {

    helper.mount(() => <PipelinePauseHeader pipelineStatus={pipelineStatus}
                                            shouldShowPauseUnpause={shouldShowPauseUnpause}
                                            pipelineName={pipelineStatus.pipelineName()}
                                            flashMessage={new FlashMessageModelWithTimeout()}
                                            shouldShowPipelineSettings={shouldShowPipelineSettings}/>);
  }
});
