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

import _ from "lodash";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {JobTestData, PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {Tab} from "models/pipeline_configs/tab";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {CustomTabTabContent} from "views/pages/clicky_pipeline_config/tabs/job/custom_tab_tab_content";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Custom Tab Tab Content", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render custom tab info message", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    const helpText = "Custom Tabs lets you add new tabs within the Job Details page.";
    const link     = (helper.q("a", helper.byTestId("custom-tab-doc-link")) as HTMLLinkElement);

    expect(helper.byTestId("flash-message-info")).toContainText(helpText);
    expect(_.includes(link.href, "/faq/dev_see_artifact_as_tab.html")).toBeTrue();
  });

  it("should render tab headers", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    const tabNameDescription = "Name of the tab which will appear in the Job Detail Page.";
    const tabPathDescription = "The artifact that will be rendered in the custom tab. This is typically a html or xml file.";

    expect(helper.byTestId("name-header")).toContainText("Tab Name");
    expect(helper.allByTestId("tooltip-wrapper")[0]).toContainText(tabNameDescription);

    expect(helper.byTestId("path-header")).toContainText("Path");
    expect(helper.allByTestId("tooltip-wrapper")[1]).toContainText(tabPathDescription);
  });

  it("should render an empty tab when no custom tabs are available", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(helper.byTestId("tab")).toBeInDOM();
  });

  it("should render custom tabs", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tabs().push(new Tab("tab1", "/dev/random"), new Tab("tab2", "/dev/random2"));
    mount(job);

    expect(helper.allByTestId("tab")).toHaveLength(2);

    expect(helper.byTestId("tab-name-tab1")).toHaveValue("tab1");
    expect(helper.byTestId("tab-path-/dev/random")).toHaveValue("/dev/random");

    expect(helper.byTestId("tab-name-tab2")).toHaveValue("tab2");
    expect(helper.byTestId("tab-path-/dev/random2")).toHaveValue("/dev/random2");
  });

  it("should add a custom tab", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tabs().push(new Tab("tab1", "/dev/random"));
    mount(job);

    expect(helper.allByTestId("tab")).toHaveLength(1);

    helper.click("button", helper.byTestId("custom-tabs"));

    helper.oninput(`[data-test-id="tab-name-"]`, "tab2");
    helper.oninput(`[data-test-id="tab-path-"]`, "path2");

    expect(job.tabs()[0].name()).toEqual("tab1");
    expect(job.tabs()[0].path()).toEqual("/dev/random");

    expect(job.tabs()[1].name()).toEqual("tab2");
    expect(job.tabs()[1].path()).toEqual("path2");

    expect(helper.allByTestId("tab")).toHaveLength(2);

    expect(helper.byTestId("tab-name-tab1")).toHaveValue("tab1");
    expect(helper.byTestId("tab-path-/dev/random")).toHaveValue("/dev/random");

    expect(helper.byTestId("tab-name-tab2")).toHaveValue("tab2");
    expect(helper.byTestId("tab-path-path2")).toHaveValue("path2");
  });

  it("should remove the custom tab", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tabs().push(new Tab("tab1", "/dev/random"), new Tab("tab2", "/dev/random2"));
    mount(job);

    expect(job.tabs()).toHaveLength(2);
    expect(job.tabs()[0].name()).toEqual("tab1");
    expect(job.tabs()[0].path()).toEqual("/dev/random");
    expect(job.tabs()[1].name()).toEqual("tab2");
    expect(job.tabs()[1].path()).toEqual("/dev/random2");
    expect(helper.byTestId("tab-name-tab1")).toHaveValue("tab1");
    expect(helper.byTestId("tab-path-/dev/random")).toHaveValue("/dev/random");
    expect(helper.byTestId("tab-name-tab2")).toHaveValue("tab2");
    expect(helper.byTestId("tab-path-/dev/random2")).toHaveValue("/dev/random2");

    helper.click(`[data-test-id="remove-tab-tab1"]`);

    expect(job.tabs()).toHaveLength(1);
    expect(job.tabs()[0].name()).toEqual("tab2");
    expect(job.tabs()[0].path()).toEqual("/dev/random2");
    expect(helper.byTestId("tab-name-tab2")).toHaveValue("tab2");
    expect(helper.byTestId("tab-path-/dev/random2")).toHaveValue("/dev/random2");
  });

  function mount(job: Job) {
    const pipelineConfig = new PipelineConfig();

    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.jobs(new NameableSet([job]));
    pipelineConfig.stages().add(stage);

    const routeParams = {
      stage_name: stage.name(),
      job_name: job.name()
    } as PipelineConfigRouteParams;

    const templateConfig = new TemplateConfig("foo", []);
    helper.mount(() => new CustomTabTabContent().content(pipelineConfig, templateConfig, routeParams, true));
  }
});
