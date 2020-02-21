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

import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {JobTestData, PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {JobSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/job_settings_tab_content";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Job Settings Tab Content", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render job name", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.name()).toBe("test");
    expect(helper.byTestId("form-field-input-job-name")).toHaveValue("test");

    const newJobName = "job-name";
    helper.oninput("input", newJobName);

    expect(job.name()).toBe(newJobName);
    expect(helper.byTestId("form-field-input-job-name")).toHaveValue(newJobName);
  });

  it("should render job resources", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.resources()).toBe("");
    expect(helper.byTestId("form-field-input-resources")).toHaveValue("");
    expect(helper.byTestId("job-settings-tab")).toContainText("The agent resources that the current job requires to run. Specify multiple resources as a comma separated list");

    const newResources = "windows,jdk11";
    helper.oninput(`[data-test-id="form-field-input-resources"]`, newResources);

    expect(job.resources()).toBe(newResources);
    expect(helper.byTestId("form-field-input-resources")).toHaveValue(newResources);
  });

  it("should render job elastic agent id", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.elasticProfileId()).toBe(undefined!);
    expect(helper.byTestId("form-field-input-elastic-agent-profile-id")).toHaveValue("");

    expect(helper.byTestId("job-settings-tab"))
      .toContainText("The Elastic Agent Profile that the current job requires to run");

    const newElasticAgentProfileId = "profile1";
    helper.oninput(`[data-test-id="form-field-input-elastic-agent-profile-id"]`, newElasticAgentProfileId);

    expect(job.elasticProfileId()).toBe(newElasticAgentProfileId);
    expect(helper.byTestId("form-field-input-elastic-agent-profile-id")).toHaveValue(newElasticAgentProfileId);
  });

  it("should enable both resources and elastic agent id when none specified", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(helper.byTestId("form-field-input-resources")).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-elastic-agent-profile-id")).not.toBeDisabled();
  });

  it("should disable elastic agent id when resources are specified", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.resources("windows");
    mount(job);

    expect(helper.byTestId("form-field-input-resources")).not.toBeDisabled();
    expect(helper.byTestId("form-field-input-elastic-agent-profile-id")).toBeDisabled();
  });

  it("should disable resources when elastic agent id is specified", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.elasticProfileId("agent1");
    mount(job);

    expect(helper.byTestId("form-field-input-resources")).toBeDisabled();
    expect(helper.byTestId("form-field-input-elastic-agent-profile-id")).not.toBeDisabled();
  });

  it("should render job timeout", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(helper.byTestId("radio-never")).toBeInDOM();
    expect(helper.q("label", helper.byTestId("input-field-for-never")).innerText).toEqual("Never");
    expect(helper.q("span", helper.byTestId("input-field-for-never")).innerText).toEqual("Never cancel the job.");

    expect(helper.byTestId("radio-default")).toBeInDOM();
    expect(helper.q("label", helper.byTestId("input-field-for-default")).innerText).toEqual("Use Default");
    expect(helper.q("span", helper.byTestId("input-field-for-default")).innerText).toEqual("Use the default job timeout specified globally.");

    expect(helper.byTestId("radio-number")).toBeInDOM();
    expect(helper.q("span", helper.byTestId("input-field-for-number")).innerText).toEqual("When the current job is inactive for more than the specified time period (in minutes), GoCD will cancel the job.");
  });

  it("should select default job timeout by default", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.timeout()).toEqual(undefined!);
    expect(job.jobTimeoutType()).toEqual("default");
    expect(helper.byTestId("radio-never")).not.toBeChecked();
    expect(helper.byTestId("radio-default")).toBeChecked();
    expect(helper.byTestId("radio-number")).not.toBeChecked();
  });

  it("should toggle job timeout to never", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.timeout()).toEqual(undefined!);
    expect(job.jobTimeoutType()).toEqual("default");
    expect(helper.byTestId("radio-never")).not.toBeChecked();
    expect(helper.byTestId("radio-default")).toBeChecked();
    expect(helper.byTestId("radio-number")).not.toBeChecked();

    helper.click("input", helper.byTestId("input-field-for-never"));

    expect(job.timeout()).toEqual("never");
    expect(job.jobTimeoutType()).toEqual("never");
    expect(helper.byTestId("radio-never")).toBeChecked();
    expect(helper.byTestId("radio-default")).not.toBeChecked();
    expect(helper.byTestId("radio-number")).not.toBeChecked();
  });

  it("should toggle job timeout to a specified number", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.timeout()).toEqual(undefined!);
    expect(job.jobTimeoutType()).toEqual("default");
    expect(helper.byTestId("radio-never")).not.toBeChecked();
    expect(helper.byTestId("radio-default")).toBeChecked();
    expect(helper.byTestId("radio-number")).not.toBeChecked();

    expect(helper.q(`input[type="number"]`, helper.byTestId("input-field-for-number"))).toBeDisabled();

    helper.click("input", helper.byTestId("input-field-for-number"));
    expect(helper.q(`input[type="number"]`, helper.byTestId("input-field-for-number"))).not.toBeDisabled();

    helper.oninput(`input[type="number"]`, "20", helper.byTestId("input-field-for-number"));

    expect(`${job.timeout()}`).toEqual("20");
    expect(job.jobTimeoutType()).toEqual("number");
    expect(helper.byTestId("radio-never")).not.toBeChecked();
    expect(helper.byTestId("radio-default")).not.toBeChecked();
    expect(helper.byTestId("radio-number")).toBeChecked();
  });

  it("should render run on instance", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(helper.byTestId("radio-one")).toBeInDOM();
    expect(helper.q("label", helper.byTestId("input-field-for-one")).innerText).toEqual("Run on one instance");
    expect(helper.q("span", helper.byTestId("input-field-for-one")).innerText).toEqual("Job will run on only agent that match its resources (if any) and are in the same environment as this job’s pipeline.");

    expect(helper.byTestId("radio-all")).toBeInDOM();
    expect(helper.q("label", helper.byTestId("input-field-for-all")).innerText).toEqual("Run on all agents");
    expect(helper.q("span", helper.byTestId("input-field-for-all")).innerText).toEqual("Job will run on all agents that match its resources (if any) and are in the same environment as this job’s pipeline. This option is particularly useful when deploying to multiple servers.");

    expect(helper.byTestId("radio-number")).toBeInDOM();
    expect(helper.q("span", helper.allByTestId("input-field-for-number")[1]).innerText).toEqual("Specified number of instances of job will be created during schedule time.");
  });

  it("should select run on instance by default", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.runInstanceCount()).toEqual(undefined!);
    expect(job.runType()).toEqual("one");
    expect(helper.byTestId("radio-one")).toBeChecked();
    expect(helper.byTestId("radio-all")).not.toBeChecked();
    expect(helper.allByTestId("radio-number")[1]).not.toBeChecked();
  });

  it("should select run on all agents", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.runInstanceCount()).toEqual(undefined!);
    expect(job.runType()).toEqual("one");
    expect(helper.byTestId("radio-one")).toBeChecked();
    expect(helper.byTestId("radio-all")).not.toBeChecked();
    expect(helper.allByTestId("radio-number")[1]).not.toBeChecked();

    helper.click("input", helper.byTestId("input-field-for-all"));

    expect(job.runInstanceCount()).toEqual("all");
    expect(job.runType()).toEqual("all");
    expect(helper.byTestId("radio-one")).not.toBeChecked();
    expect(helper.byTestId("radio-all")).toBeChecked();
    expect(helper.allByTestId("radio-number")[1]).not.toBeChecked();
  });

  it("should toggle run instance count a specified number", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.runInstanceCount()).toEqual(undefined!);
    expect(job.runType()).toEqual("one");
    expect(helper.byTestId("radio-one")).toBeChecked();
    expect(helper.byTestId("radio-all")).not.toBeChecked();
    expect(helper.allByTestId("radio-number")[1]).not.toBeChecked();

    expect(helper.q(`input[type="number"]`, helper.allByTestId("input-field-for-number")[1])).toBeDisabled();

    helper.click("input", helper.allByTestId("input-field-for-number")[1]);
    expect(helper.q(`input[type="number"]`, helper.allByTestId("input-field-for-number")[1])).not.toBeDisabled();

    helper.oninput(`input[type="number"]`, "20", helper.allByTestId("input-field-for-number")[1]);

    expect(`${job.runInstanceCount()}`).toEqual('20');
    expect(job.runType()).toEqual("number");
    expect(helper.byTestId("radio-one")).not.toBeChecked();
    expect(helper.byTestId("radio-all")).not.toBeChecked();
    expect(helper.allByTestId("radio-number")[1]).toBeChecked();
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
    helper.mount(() => new JobSettingsTabContent().content(pipelineConfig, templateConfig, routeParams, true));
  }
});
