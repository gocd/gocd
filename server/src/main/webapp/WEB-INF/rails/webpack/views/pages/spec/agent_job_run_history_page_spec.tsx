/*
 * Copyright Thoughtworks, Inc.
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

import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {AgentJobRunHistoryPage} from "views/pages/agent_job_run_history";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AgentJobRunHistoryPage", () => {
  const helper = new TestHelper();
  const agent  = AgentsTestData.withHostname("build-agent");

  beforeEach(() => {
    jasmine.Ajax.install();
    document.body.setAttribute("data-meta", JSON.stringify({uuid: agent.uuid}));
    jasmine.Ajax.stubRequest(`/go/api/agents/${agent.uuid}/job_run_history?offset=0`).andReturn({
      status: 200,
      responseText: JSON.stringify({uuid: agent.uuid, jobs: [], pagination: {page_size: 10, offset: 0, total: 0}})
    });
  });

  afterEach(() => {
    helper.unmount();
    jasmine.Ajax.uninstall();
    document.body.removeAttribute("data-meta");
  });

  it("shows the agent name and UUID above job history", async () => {
    jasmine.Ajax.stubRequest(`/go/api/agents/${agent.uuid}`).andReturn({
      status: 200,
      responseText: JSON.stringify(agent)
    });

    helper.mountPage(() => new AgentJobRunHistoryPage());
    await helper.delayRedraw();

    expect(helper.byTestId("key-value-value-agent-name")).toHaveText(agent.hostname);
    expect(helper.byTestId("key-value-value-uuid")).toHaveText(agent.uuid);
    expect(helper.byTestId("table")).toBeInDOM();
  });

  it("keeps the UUID and history visible when the agent no longer exists", async () => {
    jasmine.Ajax.stubRequest(`/go/api/agents/${agent.uuid}`).andReturn({status: 404});

    helper.mountPage(() => new AgentJobRunHistoryPage());
    await helper.delayRedraw();

    expect(helper.byTestId("key-value-value-agent-name")).toHaveText("Unavailable");
    expect(helper.byTestId("key-value-value-uuid")).toHaveText(agent.uuid);
    expect(helper.byTestId("table")).toBeInDOM();
  });
});
