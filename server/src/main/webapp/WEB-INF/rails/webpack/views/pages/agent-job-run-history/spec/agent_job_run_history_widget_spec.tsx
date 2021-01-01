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

import m from "mithril";
import Stream from "mithril/stream";
import {AgentJobRunHistoryAPIJSON} from "models/agent_job_run_history";
import {AgentJobRunHistoryWidget} from "views/pages/agent-job-run-history/agent_job_run_history_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Agent Job Run History", () => {
  const helper = new TestHelper();

  const json: AgentJobRunHistoryAPIJSON = {
    uuid: "ebe15c24-181f-4e54-85a8-fb4c5b3ca3cb",
    jobs: [
      {
        job_state_transitions: [{
          state_change_time: "2019-11-12T00:21:06Z",
          state: "Assigned"
        }, {
          state_change_time: "2019-11-12T00:22:19Z",
          state: "Building"
        }, {
          state_change_time: "2019-11-12T00:21:17Z",
          state: "Preparing"
        }, {
          state_change_time: "2019-11-12T00:37:42Z",
          state: "Rescheduled"
        }, {
          state_change_time: "2019-11-12T00:20:56Z",
          state: "Scheduled"
        }],
        job_name: "jasmine",
        stage_name: "build-non-server",
        stage_counter: "1",
        pipeline_name: "build-windows-PR",
        pipeline_counter: 5282,
        result: "Unknown",
        rerun: false
      },
      {
        job_state_transitions: [{
          state_change_time: "2019-11-12T00:21:06Z",
          state: "Assigned"
        }, {
          state_change_time: "2019-11-12T00:22:19Z",
          state: "Building"
        }, {
          state_change_time: "2019-11-12T00:21:17Z",
          state: "Preparing"
        }, {
          state_change_time: "2019-11-12T00:37:42Z",
          state: "Rescheduled"
        }, {
          state_change_time: "2019-11-12T00:20:56Z",
          state: "Scheduled"
        }],
        job_name: "jasmine-2",
        stage_name: "build-non-server-2",
        stage_counter: "1",
        pipeline_name: "build-windows-PR-2",
        pipeline_counter: 5282,
        result: "Passed",
        rerun: false
      }
    ],
    pagination: {
      page_size: 10,
      offset: 792,
      total: 793
    }
  };

  beforeEach(() => {
    helper.mount(() => {
      return <AgentJobRunHistoryWidget jobHistory={Stream(json)} onPageChange={jasmine.createSpy("onPageChange")}/>;
    });
  });

  afterEach(() => helper.unmount());

  it("should render job run history table", () => {
    expect(helper.byTestId("table")).toBeInDOM();
  });

  it("should render job run history table header", () => {
    expect(helper.byTestId("table-header")).toBeInDOM();
    expect(helper.byTestId("table-header-row")).toBeInDOM();
    expect(helper.byTestId("table-header-row").children[0]).toContainText("Pipeline");
    expect(helper.byTestId("table-header-row").children[1]).toContainText("Stage");
    expect(helper.byTestId("table-header-row").children[2]).toContainText("Job");
    expect(helper.byTestId("table-header-row").children[3]).toContainText("Result");
  });

  it("should render job run history table row for all job history", () => {
    expect(helper.byTestId("table-body")).toBeInDOM();
    expect(helper.allByTestId("table-row")).toHaveLength(2);
  });

  it("should render job run history table values", () => {
    expect(helper.allByTestId("table-row")).toHaveLength(2);

    expect(helper.allByTestId("table-row")[0].children[0]).toContainText(json.jobs[0].pipeline_name);
    expect(helper.allByTestId("table-row")[0].children[1]).toContainText(json.jobs[0].stage_name);
    expect(helper.allByTestId("table-row")[0].children[2]).toContainText(json.jobs[0].job_name);
    expect(helper.allByTestId("table-row")[0].children[3]).toContainText(json.jobs[0].result);

    expect(helper.allByTestId("table-row")[1].children[0]).toContainText(json.jobs[1].pipeline_name);
    expect(helper.allByTestId("table-row")[1].children[1]).toContainText(json.jobs[1].stage_name);
    expect(helper.allByTestId("table-row")[1].children[2]).toContainText(json.jobs[1].job_name);
    expect(helper.allByTestId("table-row")[1].children[3]).toContainText(json.jobs[1].result);
  });

  it("should by default sort the table based on pipeline name in ascending order", () => {
    expect(helper.allByTestId("table-row")[0].children[0]).toContainText(json.jobs[0].pipeline_name);
    expect(helper.allByTestId("table-row")[1].children[0]).toContainText(json.jobs[1].pipeline_name);
  });

  it("should render job link to the job details page", () => {
    const expectedLink1 = `/go/tab/build/detail/${json.jobs[0].pipeline_name}/${json.jobs[0].pipeline_counter}/${json.jobs[0].stage_name}/${json.jobs[0].stage_counter}/${json.jobs[0].job_name}`;
    const expectedLink2 = `/go/tab/build/detail/${json.jobs[1].pipeline_name}/${json.jobs[1].pipeline_counter}/${json.jobs[1].stage_name}/${json.jobs[1].stage_counter}/${json.jobs[1].job_name}`;

    expect((helper.qa("a", helper.allByTestId("table-row")[0])[0] as HTMLLinkElement).href).toContain(expectedLink1);
    expect((helper.qa("a", helper.allByTestId("table-row")[1])[0] as HTMLLinkElement).href).toContain(expectedLink2);
  });
});
