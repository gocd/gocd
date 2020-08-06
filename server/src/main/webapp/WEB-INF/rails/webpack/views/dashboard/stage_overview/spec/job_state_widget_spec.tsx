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
import {State} from "../../../../models/agent_job_run_history";
import {TestHelper} from "../../../pages/spec/test_helper";
import {JobStateWidget} from "../job_state_widget";
import {Result} from "../models/types";
import {TestData} from "./test_data";

describe("Job State Widget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render scheduled job state", () => {
    mount("Scheduled");
    expect(helper.root).toContainText('waiting for an agent');
  });

  it("should render assigned job state", () => {
    mount("Assigned");
    expect(helper.root).toContainText('agent assigned');
  });

  it("should render preparing job state", () => {
    mount("Preparing");
    expect(helper.root).toContainText('checking out materials');
  });

  it("should render building job state", () => {
    mount("Building");
    expect(helper.root).toContainText('building');
  });

  it("should render completing job state", () => {
    mount("Completing");
    expect(helper.root).toContainText('uploading artifacts');
  });

  it("should render completed passed job state", () => {
    mount("Completed", Result[Result.Passed]);
    expect(helper.root).toContainText('Passed');
  });

  it("should render completed failed job state", () => {
    mount("Completed", Result[Result.Failed]);
    expect(helper.root).toContainText('Failed');
  });

  it("should render completed cancelled job state", () => {
    mount("Completed", Result[Result.Cancelled]);
    expect(helper.root).toContainText('Cancelled');
  });

  function mount(state: State, result: Result = Result[Result.Passed]) {
    const jobJSON = TestData.stageInstanceJSON().jobs[0];
    jobJSON.state = state;
    jobJSON.result = result;

    helper.mount(() => {
      return <JobStateWidget job={jobJSON}/>;
    });
  }
});
