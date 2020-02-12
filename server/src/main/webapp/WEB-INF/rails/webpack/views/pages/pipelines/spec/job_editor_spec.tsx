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
import {TestHelper} from "views/pages/spec/test_helper";
import {JobEditor} from "../job_editor";

describe("AddPipeline: JobEditor", () => {
  const helper = new TestHelper();
  let job: Job;

  beforeEach(() => {
    job = new Job("", []);
    helper.mount(() => <JobEditor job={job}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates structure", () => {
    expect(helper.byTestId("form-field-label-job-name")).toBeTruthy();
    expect(helper.byTestId("form-field-label-job-name").textContent).toBe("Job Name*");

    expect(helper.byTestId("form-field-input-job-name")).toBeTruthy();
  });

  it("Binds to model", () => {
    expect(job.name()).toBe("");

    helper.oninput(helper.byTestId("form-field-input-job-name"), "my-job");
    expect(job.name()).toBe("my-job");
  });
});
