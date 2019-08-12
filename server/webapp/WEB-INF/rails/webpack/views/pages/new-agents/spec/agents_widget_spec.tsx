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

import m from "mithril";

import {Agent, Agents} from "models/new-agent/agents";
import {AgentsTestData} from "models/new-agent/spec/agents_test_data";
import {AgentsWidget} from "views/pages/new-agents/agents_widget";
import {TestHelper} from "views/pages/spec/test_helper";
import * as simulateEvent from "simulate-event";

describe("NewAgentsWidget", () => {
  const helper = new TestHelper();
  let agents: Agents;

  beforeEach(() => {
    agents = Agents.fromJSON(AgentsTestData.list());
  });

  afterEach(helper.unmount.bind(helper));

  it("should render table headers", () => {
    mount(agents);

    const headers = helper.findByDataTestId("table-header-row");

    expect(headers.children()).toHaveLength(8);
    expect(headers.children()[0]).toContainText("Agent Name");
    expect(headers.children()[1]).toContainText("Sandbox");
    expect(headers.children()[2]).toContainText("OS");
    expect(headers.children()[3]).toContainText("IP Address");
    expect(headers.children()[4]).toContainText("Status");
    expect(headers.children()[5]).toContainText("Free Space");
    expect(headers.children()[6]).toContainText("Resources");
    expect(headers.children()[7]).toContainText("Environments");
  });

  it("should render agents in the table", () => {
    mount(agents);

    const tableBody = helper.findByDataTestId("table-body");
    expect(tableBody.children()).toHaveLength(3);

    assertAgentRow(agents.list()[0]);
    assertAgentRow(agents.list()[1]);
    assertAgentRow(agents.list()[2]);
  });

  it("should render a serach box", () => {
    mount(agents);

    const searchBox = helper.findByDataTestId("form-field-input-search-for-agents");

    expect(searchBox).toBeInDOM();
  });

  it("should filter agents based on the searched value", () => {
    mount(agents);

    const agentA    = agents.list()[0],
          agentB    = agents.list()[1],
          agentC    = agents.list()[2];
    const searchBox = helper.findByDataTestId("form-field-input-search-for-agents");

    expect(helper.findByDataTestId("table-body").children()).toHaveLength(3);
    assertAgentRow(agentA);
    assertAgentRow(agentB);
    assertAgentRow(agentC);

    searchBox.val("wind");
    simulateEvent.simulate(searchBox.get(0), "input");

    expect(helper.findByDataTestId("table-body").children()).toHaveLength(1);
    assertAgentRow(agentA);
  });

  describe("Resources", () => {
    it("should list comma separated resources", () => {
      const agent  = Agent.fromJSON(AgentsTestData.agentWithResources(["psql", "firefox", "chrome"]));
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.findByDataTestId(`agent-resources-of-${agent.uuid}`)).toContainText("psql, firefox, chrome");
    });

    it("should show 'none specified' when agent has no resources specified", () => {
      const agent  = Agent.fromJSON(AgentsTestData.agentWithResources([]));
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.findByDataTestId(`agent-resources-of-${agent.uuid}`)).toContainText("none specified");
    });

    it("should show 'none specified' when resources are null", () => {
      const agent  = Agent.fromJSON(AgentsTestData.elasticAgent("32197397439"));
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.findByDataTestId(`agent-resources-of-${agent.uuid}`)).toContainText("none specified");
    });
  });

  function mount(agents: Agents) {
    helper.mount(() => <AgentsWidget agents={agents}/>);
  }

  function assertAgentRow(agent: Agent) {
    expect(helper.findByDataTestId(`agent-hostname-of-${agent.uuid}`)).toContainText(agent.hostname);
    expect(helper.findByDataTestId(`agent-sandbox-of-${agent.uuid}`)).toContainText(agent.sandbox);
    expect(helper.findByDataTestId(`agent-operating-system-of-${agent.uuid}`)).toContainText(agent.operatingSystem);
    expect(helper.findByDataTestId(`agent-ip-address-of-${agent.uuid}`)).toContainText(agent.ipAddress);
    expect(helper.findByDataTestId(`agent-free-space-of-${agent.uuid}`)).toContainText(agent.readableFreeSpace());
    expect(helper.findByDataTestId(`agent-resources-of-${agent.uuid}`))
      .toContainText(AgentsWidget.joinOrNoneSpecified(agent.resources));
  }
});
