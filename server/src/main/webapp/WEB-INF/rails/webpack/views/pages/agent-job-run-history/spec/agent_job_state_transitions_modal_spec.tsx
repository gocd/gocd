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

import {timeFormatter as TimeFormatter} from "helpers/time_formatter";
import {JobRunHistoryJSON} from "models/agent_job_run_history";
import moment from "moment";
import {AgentJobStateTransitionModal} from "views/pages/agent-job-run-history/agent_job_state_transitions_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Agent Job State Transitions Modal", () => {
  const defaultFormat = "YYYY-MM-DDTHH:mm:ssZ";
  const helper        = new TestHelper();

  const json = {
    job_state_transitions: [{
      state_change_time: "2019-11-22T10:43:25Z",
      state: "Scheduled"
    }, {
      state_change_time: "2019-11-22T10:43:46Z",
      state: "Assigned"
    }, {
      state_change_time: "2019-11-22T10:43:57Z",
      state: "Preparing"
    }, {
      state_change_time: "2019-11-22T10:44:00Z",
      state: "Building"
    }, {
      state_change_time: "2019-11-22T10:44:11Z",
      state: "Completing"
    }, {
      state_change_time: "2019-11-22T10:44:11Z",
      state: "Completed"
    }],
    job_name: "up42_job",
    stage_name: "up42_stage",
    stage_counter: "1",
    pipeline_name: "up42",
    pipeline_counter: 3,
    result: "Passed",
    rerun: false
  } as JobRunHistoryJSON;

  beforeEach(() => {
    helper.mount(() => new AgentJobStateTransitionModal(json).body());
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render job information", () => {
    expect(helper.byTestId("key-job")).toContainText("Job");
    expect(helper.byTestId("value-job")).toContainText("up42/3/up42_stage/1/up42_job");
  });

  it("should render wait time", () => {
    expect(helper.byTestId("key-wait-time")).toContainText("Wait Time");
    const start = moment(json.job_state_transitions[0].state_change_time, defaultFormat);
    const end   = moment(json.job_state_transitions[1].state_change_time, defaultFormat);

    expect(helper.byTestId("value-wait-time")).toContainText("00:00:21");
    expect(helper.byTestId("value-wait-time")).toContainText(moment.utc(end.diff(start)).format("HH:mm:ss"));
  });

  it("should render help text for wait time", () => {
    const expected = "Wait Time is the time spent by the job waiting for an agent to be assigned. This is the total time spent by the job from the time it is Scheduled to it is Assigned.";
    expect(helper.byTestId("key-wait-time")).toContainText(expected);
  });

  it("should render help text for building time", () => {
    const expected = "Building time is the time spent building the job on an agent. This is the total time spent by the job from the time it is Assigned to it is Completed.";
    expect(helper.byTestId("key-building-time")).toContainText(expected);
  });

  it("should render help text for total time", () => {
    const expected = "Total time is the time taken by the job from scheduling to completion. This is the total time spent by the job from the time it is Scheduled to it is Completed.";
    expect(helper.byTestId("key-total-time")).toContainText(expected);
  });

  it("should render building time", () => {
    expect(helper.byTestId("key-building-time")).toContainText("Building Time");
    const start = moment(json.job_state_transitions[1].state_change_time, defaultFormat);
    const end   = moment(json.job_state_transitions[5].state_change_time, defaultFormat);

    expect(helper.byTestId("value-building-time")).toContainText("00:00:25");
    expect(helper.byTestId("value-building-time")).toContainText(moment.utc(end.diff(start)).format("HH:mm:ss"));
  });

  it("should render total time", () => {
    expect(helper.byTestId("key-total-time")).toContainText("Total Time");
    const start = moment(json.job_state_transitions[0].state_change_time, defaultFormat);
    const end   = moment(json.job_state_transitions[5].state_change_time, defaultFormat);

    expect(helper.byTestId("value-total-time")).toContainText("00:00:46");
    expect(helper.byTestId("value-total-time")).toContainText(moment.utc(end.diff(start)).format("HH:mm:ss"));
  });

  for (let i = 0; i < json.job_state_transitions.length; i++) {
    it(`should render ${json.job_state_transitions[i].state} column in job state transition table`, () => {
      expect(helper.q(`tr[data-id="${i}"] i`)).toContainText(json.job_state_transitions[i].state);
      const expectedTime = TimeFormatter.format(json.job_state_transitions[i].state_change_time);
      expect(helper.q(`tr[data-id="${i}"] td:nth-of-type(2)`)).toContainText(expectedTime);
    });
  }
});
