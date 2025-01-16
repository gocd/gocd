/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import m from 'mithril';
import Stream from "mithril/stream";
import moment from 'moment';
import {timeFormatter} from "../../../helpers/time_formatter";
import {JobInstanceJSON} from "../../../models/job_detail/job_detail";
import {JobIdentifier} from "../../../models/shared/job_identifier";
import {JobStateTransitionJSON} from "../../../models/shared/job_state_transition";
import {TestHelper} from "../spec/test_helper";
import {Attrs, BuildDetailWidget} from "./build_detail_widget";
import {Agent} from "../../../models/agents/agents";
import {AgentsTestData} from "../../../models/agents/spec/agents_test_data";

describe("BuildDetailWidget", () => {
  const helper = new TestHelper();
  let attrs: Attrs;
  const jobIdentifier: JobIdentifier = {
    pipelineName: 'some-pipeline',
    pipelineCounter: 42,
    stageName: 'some-stage',
    stageCounter: 418,
    jobName: 'some-job'
  };

  const scheduled: JobStateTransitionJSON = {
    state: "Scheduled",
    state_change_time: 1660796197576
  };
  const assigned: JobStateTransitionJSON = {
    state: "Assigned",
    state_change_time: 1660796250118
  };
  const preparing: JobStateTransitionJSON = {
    state: "Preparing",
    state_change_time: 1660796260305
  };
  const building: JobStateTransitionJSON = {
    state: "Building",
    state_change_time: 1660796261548
  };
  const completing: JobStateTransitionJSON = {
    state: "Completing",
    state_change_time: 1660796291682
  };
  const completed: JobStateTransitionJSON = {
    state: "Completed",
    state_change_time: 1660796291719
  };

  let jobInstanceJSON: JobInstanceJSON;
  let agent: Agent;

  beforeEach(() => {
    jobInstanceJSON = {
      name: "some-job",
      state: "Completed",
      result: "Passed",
      original_job_id: null,
      scheduled_date: 1660796197576,
      rerun: false,
      agent_uuid: "8002f0dc-5f60-4767-a0c9-f6af55076a0a",
      pipeline_name: "some-pipeline",
      pipeline_counter: 42,
      stage_name: "some-stage",
      stage_counter: "418",
      job_state_transitions: [scheduled, assigned, preparing, building, completing, completed]
    };

    agent = Agent.fromJSON(AgentsTestData.buildingAgent());

    attrs = {
      buildCause: "modified by Pick E Reader <pick.e.reader@example.com>",
      jobIdentifier,
      agent: Stream<Agent>(agent),
      jobInstance: Stream<JobInstanceJSON>(jobInstanceJSON)
    };
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render basic details for completed jobs", () => {
    helper.mount(() => <BuildDetailWidget {...attrs}/>);

    expect(helper.textByTestId("scheduled-at")).toEqual(timeFormatter.format(moment(1660796197576)));
    expect(helper.textByTestId("agent-id")).toEqual('8002f0dc-5f60-4767-a0c9-f6af55076a0a');
    expect(helper.textByTestId("completed-at")).toEqual(timeFormatter.format(moment(1660796291719)));
    expect(helper.textByTestId("build-cause")).toEqual("modified by Pick E Reader <pick.e.reader@example.com>");
    expect(helper.textByTestId("progress")).toEqual("30 seconds");
    expect(helper.textByTestId("progress-label")).toEqual("Duration:");
  });

  it("should render basic details for scheduled jobs", () => {
    jobInstanceJSON.state = 'Scheduled';
    jobInstanceJSON.result = 'Unknown';
    jobInstanceJSON.job_state_transitions = [scheduled];
    helper.mount(() => <BuildDetailWidget {...attrs}/>);

    expect(helper.textByTestId("scheduled-at")).toEqual(timeFormatter.format(moment(1660796197576)));
    expect(helper.textByTestId("agent-id")).toEqual('8002f0dc-5f60-4767-a0c9-f6af55076a0a');
    expect(helper.textByTestId("completed-at")).toEqual('In progress');
    expect(helper.textByTestId("build-cause")).toEqual("modified by Pick E Reader <pick.e.reader@example.com>");
    expect(helper.textByTestId("progress")).toEqual("30 seconds");
    expect(helper.textByTestId("progress-label")).toEqual("Duration:");
  });

});
