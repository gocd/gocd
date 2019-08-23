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
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {AgentsWidget} from "views/pages/new-agents/agents_widget";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";

describe("NewAgentsWidget", () => {
  const helper                 = new TestHelper(),
        onEnable: jasmine.Spy  = jasmine.createSpy("onEnable"),
        onDisable: jasmine.Spy = jasmine.createSpy("onDisable"),
        onDelete: jasmine.Spy  = jasmine.createSpy("onDelete"),
        updateEnvironments     = jasmine.createSpy("updateEnvironments"),
        updateResources        = jasmine.createSpy("updateResources");

  let agents: Agents, agentA: Agent, agentB: Agent, agentC: Agent;

  beforeEach(() => {
    agents = Agents.fromJSON(AgentsTestData.list());
    agentA = agents.list()[0];
    agentB = agents.list()[1];
    agentC = agents.list()[2];
  });

  afterEach(helper.unmount.bind(helper));

  it("should render table headers", () => {
    mount(agents);

    const headers = helper.byTestId("table-header-row");

    expect(headers.children).toHaveLength(9);
    expect(headers.children[1]).toContainText("Agent Name");
    expect(headers.children[2]).toContainText("Sandbox");
    expect(headers.children[3]).toContainText("OS");
    expect(headers.children[4]).toContainText("IP Address");
    expect(headers.children[5]).toContainText("Status");
    expect(headers.children[6]).toContainText("Free Space");
    expect(headers.children[7]).toContainText("Resources");
    expect(headers.children[8]).toContainText("Environments");
  });

  it("should render agents in the table", () => {
    mount(agents);

    const tableBody = helper.byTestId("table-body");
    expect(tableBody.children).toHaveLength(3);

    assertAgentRow(agents.list()[0]);
    assertAgentRow(agents.list()[1]);
    assertAgentRow(agents.list()[2]);
  });

  it("should render a search box", () => {
    mount(agents);

    const searchBox = helper.byTestId("form-field-input-search-for-agents");

    expect(searchBox).toBeInDOM();
  });

  it("should filter agents based on the searched value", () => {
    mount(agents);
    const searchBox = helper.byTestId("form-field-input-search-for-agents");

    expect(helper.byTestId("table-body").children).toHaveLength(3);
    assertAgentRow(agentA);
    assertAgentRow(agentB);
    assertAgentRow(agentC);

    helper.oninput(searchBox, "wind");

    expect(helper.byTestId("table-body").children).toHaveLength(1);
    assertAgentRow(agentA);
  });

  it("should highlight building agents", () => {
    const agentA = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC = Agent.fromJSON(AgentsTestData.pendingAgent());
    const agents = new Agents([agentA, agentB, agentC]);
    mount(agents);

    assertAgentBuilding(agentB);
    assertAgentNotBuilding(agentA);
    assertAgentNotBuilding(agentC);
  });

  it("should hide the build details when clicked outside", () => {
    const agentA = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC = Agent.fromJSON(AgentsTestData.pendingAgent());
    const agents = new Agents([agentA, agentB, agentC]);
    mount(agents);

    helper.click(helper.byTestId(`agent-status-text-${agentB.uuid}`));
    expect(helper.byTestId(`agent-build-details-of-${agentB.uuid}`)).toBeVisible();

    helper.click(helper.byTestId(`agent-hostname-of-${agentA.uuid}`));

    expect(helper.byTestId(`agent-build-details-of-${agentB.uuid}`)).not.toBeVisible();
  });

  describe("Resources", () => {
    it("should list comma separated resources", () => {
      const agent  = Agent.fromJSON(AgentsTestData.agentWithResources(["psql", "firefox", "chrome"]));
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.byTestId(`agent-resources-of-${agent.uuid}`)).toContainText("psql, firefox, chrome");
    });

    it("should show 'none specified' when agent has no resources specified", () => {
      const agent  = Agent.fromJSON(AgentsTestData.agentWithResources([]));
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.byTestId(`agent-resources-of-${agent.uuid}`)).toContainText("none specified");
    });

    it("should show 'none specified' when resources are null", () => {
      const agent  = Agent.fromJSON(AgentsTestData.elasticAgent("32197397439"));
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.byTestId(`agent-resources-of-${agent.uuid}`)).toContainText("none specified");
    });
  });

  describe("Environments", () => {
    it("should list comma separated environments", () => {
      const agent  = Agent.fromJSON(AgentsTestData.agentWithEnvironments("prod", "dev", "qa"));
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).toContainText("prod, dev, qa");
    });

    it("should show 'none specified' when agent has no resources specified", () => {
      const agent  = Agent.fromJSON(AgentsTestData.agentWithEnvironments());
      const agents = new Agents([agent]);

      mount(agents);

      expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).toContainText("none specified");
    });
  });

  describe("Hostname", () => {
    it("should render hostname as link when user is an admin", () => {
      mount(new Agents([agentA]), true);

      const anchor = helper.q("a", helper.byTestId(`agent-hostname-of-${agentA.uuid}`));
      expect(anchor).toContainText(agentA.hostname);
      expect(anchor).toHaveAttr("href", `/go/agents/${agentA.uuid}/job_run_history`);
    });

    it("should render hostname as text when user is not an admin", () => {
      mount(new Agents([agentA]), false);

      const hostnameCell = helper.byTestId(`agent-hostname-of-${agentA.uuid}`);
      const anchor       = helper.q("a", hostnameCell);
      expect(hostnameCell).toBeInDOM();
      expect(hostnameCell).toContainText(agentA.hostname);
      expect(anchor).not.toBeInDOM();
    });
  });

  describe("AgentSelection", () => {
    it("should not render checkboxes when user is not an admin", () => {
      mount(agents, false);

      expect(helper.byTestId("select-all-agents")).not.toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).not.toBeInDOM();
    });

    it("should render checkboxes when user is an admin", () => {
      mount(agents, true);

      expect(helper.byTestId("select-all-agents")).toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).toBeInDOM();
    });

    it("should render page with no agent selected", () => {
      mount(agents);

      expect(helper.byTestId("select-all-agents")).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).not.toBeChecked();
    });

    it("should select all on click of global checkbox", () => {
      mount(agents);
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).not.toBeChecked();

      helper.click(helper.byTestId("select-all-agents"));

      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).toBeChecked();
    });
  });

  function mount(agents: Agents, isUserAdmin: boolean = true) {
    helper.mount(() => <AgentsWidget agents={agents}
                                     onEnable={onEnable}
                                     onDisable={onDisable}
                                     onDelete={onDelete}
                                     updateEnvironments={updateEnvironments}
                                     updateResources={updateResources}
                                     isUserAdmin={isUserAdmin}
                                     flashMessage={new FlashMessageModelWithTimeout()}/>);
  }

  function assertAgentRow(agent: Agent) {
    expect(helper.byTestId(`agent-hostname-of-${agent.uuid}`)).toContainText(agent.hostname);
    expect(helper.byTestId(`agent-sandbox-of-${agent.uuid}`)).toContainText(agent.sandbox);
    expect(helper.byTestId(`agent-operating-system-of-${agent.uuid}`)).toContainText(agent.operatingSystem);
    expect(helper.byTestId(`agent-ip-address-of-${agent.uuid}`)).toContainText(agent.ipAddress);
    expect(helper.byTestId(`agent-free-space-of-${agent.uuid}`)).toContainText(agent.readableFreeSpace());
    expect(helper.byTestId(`agent-resources-of-${agent.uuid}`))
      .toContainText(AgentsWidget.joinOrNoneSpecified(agent.resources) as string);
    expect(helper.byTestId(`agent-environments-of-${agent.uuid}`))
      .toContainText(AgentsWidget.joinOrNoneSpecified(agent.environmentNames()) as string);
  }

  function assertAgentBuilding(agent: Agent) {
    expect(helper.byTestId(`agent-hostname-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-sandbox-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-operating-system-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-ip-address-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-free-space-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-resources-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).toHaveClass(styles.building);
  }

  function assertAgentNotBuilding(agent: Agent) {
    expect(helper.byTestId(`agent-hostname-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-sandbox-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-operating-system-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-ip-address-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-free-space-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-resources-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).not.toHaveClass(styles.building);
  }
});
