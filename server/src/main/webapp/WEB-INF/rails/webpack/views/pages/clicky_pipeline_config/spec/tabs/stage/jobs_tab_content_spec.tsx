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

import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {JobsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/jobs_tab_content";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Jobs Tab Content", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render job", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "job1"));
    mount(stage);

    const jobs = helper.qa("tr", helper.byTestId("table-body"));
    expect(jobs.length).toBe(1);

    const jobRow = helper.qa("td", helper.byTestId("table-body"));

    expect(jobRow.length).toBe(5);

    expect(jobRow.item(0)).toContainText("job1");
    expect(jobRow.item(1)).toContainText("");
    expect(jobRow.item(2)).toContainText("No");
    expect(jobRow.item(3)).toContainText("No");
  });

  it("should render multiple jobs", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "job1", "job2"));
    mount(stage);

    const jobs = helper.qa("tr", helper.byTestId("table-body"));
    expect(jobs.length).toBe(2);
  });

  it("should disable delete icon for only job from the stage", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "job1"));
    mount(stage);

    expect(helper.byTestId("job1-delete-icon")).toBeInDOM();
    const expectedMsg = "Can not delete the only job from the stage.";
    expect(helper.byTestId("job1-delete-icon").title).toEqual(expectedMsg);
    expect(helper.byTestId("job1-delete-icon")).toBeDisabled();
  });

  it("should not disable delete icon when there are multiple jobs in a stage", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "job1", "job2"));
    mount(stage);

    expect(helper.byTestId("job1-delete-icon")).toBeInDOM();
    expect(helper.byTestId("job1-delete-icon")).not.toBeDisabled();
    expect(helper.byTestId("job2-delete-icon")).toBeInDOM();
    expect(helper.byTestId("job2-delete-icon")).not.toBeDisabled();
  });

  it("should delete job on click of delete icon", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "job1", "job2"));
    mount(stage);

    let jobs = helper.qa("tr", helper.byTestId("table-body"));
    expect(jobs.length).toBe(2);

    helper.clickByTestId("job1-delete-icon");

    const body = document.body;
    expect(helper.byTestId("modal-title", body)).toContainText("Delete Job");
    expect(helper.byTestId("modal-body", body)).toContainText("Do you want to delete the job 'job1'?");

    helper.clickByTestId("primary-action-button", body);

    jobs = helper.qa("tr", helper.byTestId("table-body"));
    expect(jobs.length).toBe(1);
  });

  function mount(stage: Stage) {
    const pipelineConfig = new PipelineConfig();
    pipelineConfig.stages().add(stage);
    const routeParams    = {stage_name: stage.name()} as PipelineConfigRouteParams;
    const templateConfig = new TemplateConfig("foo", []);
    helper.mount(() => new JobsTabContent().content(pipelineConfig,
                                                    templateConfig,
                                                    routeParams,
                                                    Stream<OperationState>(OperationState.UNKNOWN),
                                                    new FlashMessageModelWithTimeout(),
                                                    jasmine.createSpy().and.returnValue(Promise.resolve()),
                                                    jasmine.createSpy().and.returnValue(Promise.resolve())));
  }

});
