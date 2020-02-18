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
import {Job} from "models/pipeline_configs/job";
import {JobTestData} from "models/pipeline_configs/spec/test_data";
import {JobEditor} from "views/pages/clicky_pipeline_config/widgets/job_editor_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("JobEditor", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render job name textfield", () => {
    const job = Job.fromJSON(JobTestData.with("Test"));
    mount(job);

    expect(helper.byTestId("job-name-input")).toBeInDOM();
  });

  it("should update job name when job name textfield is updated", () => {
    const job = Job.fromJSON(JobTestData.with("Test"));
    mount(job);
    helper.oninput(helper.byTestId("job-name-input"), "Test");
    expect(job.name()).toEqual("Test");

    helper.oninput(helper.byTestId("job-name-input"), "IntegrationTest");

    expect(job.name()).toEqual("IntegrationTest");
  });

  it("should render resources textfield", () => {
    const job = Job.fromJSON(JobTestData.with("Test"));
    mount(job);

    expect(helper.byTestId("resources-input")).toBeInDOM();
  });

  it("should update resources when textfield for resources is updated", () => {
    const job = Job.fromJSON(JobTestData.with("Test"));
    mount(job);
    helper.oninput(helper.byTestId("resources-input"), "");
    expect(job.resources()).toEqual("");

    helper.oninput(helper.byTestId("resources-input"), "MySql, Firefox, Chrome");

    expect(job.resources()).toEqual("MySql, Firefox, Chrome");
  });

  it("should render elastic profile id textfield", () => {
    const job = Job.fromJSON(JobTestData.with("Test"));
    mount(job);

    expect(helper.byTestId("elastic-profile-id-input")).toBeInDOM();
  });

  it("should update resources when textfield for elastic profile id is updated", () => {
    const job = Job.fromJSON(JobTestData.with("Test"));
    mount(job);
    helper.oninput(helper.byTestId("elastic-profile-id-input"), "");
    expect(job.elasticProfileId()).toEqual("");

    helper.oninput(helper.byTestId("elastic-profile-id-input"), "docker-elastic-agent");

    expect(job.elasticProfileId()).toEqual("docker-elastic-agent");
  });

  describe("Job timeout", () => {
    it("should render job timeout textfield", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      mount(job);

      const container = helper.byTestId("job-timeout");
      expect(container).toBeInDOM();
      expect(helper.byTestId("form-field-label", container)).toContainText("Job timeout");

      expect(helper.q("input", helper.byTestId("input-field-for-never"))).toBeInDOM();
      expect(helper.q("input", helper.byTestId("input-field-for-10"))).toBeInDOM();
      expect(helper.q("input", helper.byTestId("input-field-for-custom"))).toBeInDOM();

      expect(helper.q("input", helper.byTestId("input-field-for-10"))).toBeChecked();
    });

    it("should render custom radio button selected", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.timeout(4);
      mount(job);

      expect(helper.q("input", helper.byTestId("input-field-for-custom"))).toBeChecked();
      expect(helper.byTestId("custom-timeout-value")).toHaveValue("4");
    });

    it("should render never radio button selected", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.timeout("never");
      mount(job);

      expect(helper.q("input", helper.byTestId("input-field-for-never"))).toBeChecked();
    });

    it("should render default(10 minutes) radio button selected", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.timeout(10);
      mount(job);

      expect(helper.q("input", helper.byTestId("input-field-for-10"))).toBeChecked();
    });

    it("should change timeout on click of radio button", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.timeout("never");
      mount(job);
      expect(helper.q("input", helper.byTestId("input-field-for-never"))).toBeChecked();

      helper.click(helper.q("input", helper.byTestId("input-field-for-10")));
      expect(helper.q("input", helper.byTestId("input-field-for-10"))).toBeChecked();
      expect(job.timeout()).toEqual(10);

      helper.click(helper.q("input", helper.byTestId("input-field-for-custom")));
      expect(helper.q("input", helper.byTestId("input-field-for-custom"))).toBeChecked();
      expect(job.timeout()).toEqual(null);
    });

    it("should change timeout value on change of custom input field value", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.timeout(2);
      mount(job);
      expect(helper.byTestId("custom-timeout-value")).toHaveValue("2");

      helper.oninput(helper.byTestId("custom-timeout-value"), "13");

      expect(helper.byTestId("custom-timeout-value")).toHaveValue("13");
      //@ts-ignore
      expect(job.timeout()).toEqual("13");
    });
  });

  describe("Run type", () => {
    it("should render run type textfield", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      mount(job);

      const container = helper.byTestId("run-type");
      expect(container).toBeInDOM();
      expect(helper.byTestId("form-field-label", container)).toContainText("Run type");

      expect(helper.q("input", helper.byTestId("input-field-for-runSingleInstance"))).toBeInDOM();
      expect(helper.q("input", helper.byTestId("input-field-for-runOnAllAgents"))).toBeInDOM();
      expect(helper.q("input", helper.byTestId("input-field-for-runMultipleInstance"))).toBeInDOM();

      expect(helper.q("input", helper.byTestId("input-field-for-runSingleInstance"))).toBeChecked();
    });

    it("should render runMultipleInstance radio button selected", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.runInstanceCount(4);
      mount(job);

      expect(helper.q("input", helper.byTestId("input-field-for-runMultipleInstance"))).toBeChecked();
      expect(helper.byTestId("run-multiple-instances")).toHaveValue("4");
    });

    it("should render run single radio button selected", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.runInstanceCount(1);
      mount(job);

      expect(helper.q("input", helper.byTestId("input-field-for-runSingleInstance"))).toBeChecked();
    });

    it("should render run on all radio button selected", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.runInstanceCount("all");
      mount(job);

      expect(helper.q("input", helper.byTestId("input-field-for-runOnAllAgents"))).toBeChecked();
    });

    it("should change run type on click of radio button", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.runInstanceCount(1);
      mount(job);
      expect(helper.q("input", helper.byTestId("input-field-for-runSingleInstance"))).toBeChecked();

      helper.click(helper.q("input", helper.byTestId("input-field-for-runOnAllAgents")));
      expect(helper.q("input", helper.byTestId("input-field-for-runOnAllAgents"))).toBeChecked();
      expect(job.runInstanceCount()).toEqual("all");

      helper.click(helper.q("input", helper.byTestId("input-field-for-runMultipleInstance")));
      expect(helper.q("input", helper.byTestId("input-field-for-runMultipleInstance"))).toBeChecked();
      expect(job.runInstanceCount()).toEqual(null);
    });

    it("should change run type value on change of run on multiple instances input field value", () => {
      const job = Job.fromJSON(JobTestData.with("Test"));
      job.runInstanceCount(2);
      mount(job);
      expect(helper.byTestId("run-multiple-instances")).toHaveValue("2");

      helper.oninput(helper.byTestId("run-multiple-instances"), "13");

      expect(helper.byTestId("run-multiple-instances")).toHaveValue("13");
      //@ts-ignore
      expect(job.runInstanceCount()).toEqual("13");
    });
  });

  function mount(job: Job) {
    helper.mount(() => <JobEditor job={job}/>);
  }
});
