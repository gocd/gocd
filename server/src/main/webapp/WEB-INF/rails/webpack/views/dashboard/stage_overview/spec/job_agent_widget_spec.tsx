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
import Stream from "mithril/stream";
import {Agents} from "../../../../models/agents/agents";
import {AgentsTestData} from "../../../../models/agents/spec/agents_test_data";
import {TestHelper} from "../../../pages/spec/test_helper";
import {JobAgentWidget} from "../job_agent_widget";
import {JobJSON} from "../models/types";
import {TestData} from "./test_data";

describe("Job Agent Widget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render unassigned when no agent is assigned for the job", () => {
    const agents = AgentsTestData.list();
    const job = TestData.stageInstanceJSON().jobs[0];
    job.agent_uuid = undefined;
    mount(job, Agents.fromJSON(agents));
    expect(helper.root).toContainText('unassigned');
  });

  it("should render agent hostname", () => {
    const agents = AgentsTestData.list();
    const job = TestData.stageInstanceJSON().jobs[0];
    job.agent_uuid = agents._embedded.agents[0].uuid;
    mount(job, Agents.fromJSON(agents));
    expect(helper.root).toContainText(agents._embedded.agents[0].hostname);
  });

  it('should link to agent run history', () => {
    const agents = AgentsTestData.list();
    const job = TestData.stageInstanceJSON().jobs[0];
    job.agent_uuid = agents._embedded.agents[0].uuid;
    mount(job, Agents.fromJSON(agents));

    const expectedLink = `/go/agents/${job.agent_uuid}/job_run_history`;
    expect(helper.root).toContainText(agents._embedded.agents[0].hostname);
    expect(helper.q('a').href.indexOf(expectedLink)).not.toBe(-1);
  });

  it("should render agent uuid when no agent with the specified uuid is present in the agents", () => {
    const agents = AgentsTestData.list();
    const job = TestData.stageInstanceJSON().jobs[0];
    mount(job, Agents.fromJSON(agents));
    expect(helper.root).toContainText(job.agent_uuid);
  });

  function mount(job: JobJSON, agents: Agents) {
    helper.mount(() => {
      return <JobAgentWidget job={job} agents={Stream(agents)}/>;
    });
  }

});
