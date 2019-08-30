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
import Stream from "mithril/stream";
import {Agent, Agents} from "models/new_agent/agents";
import {AgentsVM} from "models/new_agent/agents_vm";
import {AgentsTestData} from "models/new_agent/spec/agents_test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {AnalyticsPluginInfo, AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {AgentsWidget} from "views/pages/new_agents/agents_widget";
import {StaticAgentsWidget} from "views/pages/new_agents/static_agents_widget";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";

describe("NewAgentsWidget", () => {
  const helper                 = new TestHelper(),
        onEnable: jasmine.Spy  = jasmine.createSpy("onEnable"),
        onDisable: jasmine.Spy = jasmine.createSpy("onDisable"),
        onDelete: jasmine.Spy  = jasmine.createSpy("onDelete"),
        updateEnvironments     = jasmine.createSpy("updateEnvironments"),
        updateResources        = jasmine.createSpy("updateResources");

  let agentsVM: AgentsVM, agentA: Agent, agentB: Agent, agentC: Agent;

  beforeEach(() => {
    agentA   = Agent.fromJSON(AgentsTestData.withOs("Windows"));
    agentB   = Agent.fromJSON(AgentsTestData.withOs("MacOS"));
    agentC   = Agent.fromJSON(AgentsTestData.withOs("Linux"));
    agentsVM = new AgentsVM(new Agents(agentA, agentB, agentC));
  });

  afterEach(helper.unmount.bind(helper));

  it("should render table headers", () => {
    mount(agentsVM);

    const headers = helper.byTestId("table-header-row");

    expect(headers.children).toHaveLength(10);
    expect(headers.children[0].children).toContain(helper.byTestId("select-all-agents"));
    expect(headers.children[1]).toContainText("Agent Name");
    expect(headers.children[2]).toContainText("Sandbox");
    expect(headers.children[3]).toContainText("OS");
    expect(headers.children[4]).toContainText("IP Address");
    expect(headers.children[5]).toContainText("Status");
    expect(headers.children[6]).toContainText("Free Space");
    expect(headers.children[7]).toContainText("Resources");
    expect(headers.children[8]).toContainText("Environments");
    expect(headers.children[9]).toContainText("");
  });

  it("should render agents in the table", () => {
    mount(agentsVM);

    const tableBody = helper.byTestId("table-body");
    expect(tableBody.children).toHaveLength(3);

    assertAgentRow(agentsVM.list()[0]);
    assertAgentRow(agentsVM.list()[1]);
    assertAgentRow(agentsVM.list()[2]);
  });

  it("should render a search box", () => {
    mount(agentsVM);

    const searchBox = helper.byTestId("form-field-input-search-for-agents");

    expect(searchBox).toBeInDOM();
  });

  it("should filter agents based on the searched value", () => {
    mount(agentsVM);
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
    const agentA   = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC   = Agent.fromJSON(AgentsTestData.pendingAgent());
    const agentsVM = new AgentsVM(new Agents(agentA, agentB, agentC));
    mount(agentsVM);

    assertAgentBuilding(agentB);
    assertAgentNotBuilding(agentA);
    assertAgentNotBuilding(agentC);
  });

  it("should hide the build details when clicked outside", () => {
    const agentA   = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC   = Agent.fromJSON(AgentsTestData.pendingAgent());
    const agentsVM = new AgentsVM(new Agents(agentA, agentB, agentC));
    mount(agentsVM);

    helper.click(helper.byTestId(`agent-status-text-${agentB.uuid}`));
    expect(helper.byTestId(`agent-build-details-of-${agentB.uuid}`)).toBeVisible();

    helper.click(helper.byTestId(`agent-hostname-of-${agentA.uuid}`));

    expect(helper.byTestId(`agent-build-details-of-${agentB.uuid}`)).not.toBeVisible();
  });

  describe("Resources", () => {
    it("should list comma separated resources", () => {
      const agent    = Agent.fromJSON(AgentsTestData.withResources("psql", "firefox", "chrome"));
      const agentsVM = new AgentsVM(new Agents(agent));

      mount(agentsVM);

      expect(helper.byTestId(`agent-resources-of-${agent.uuid}`)).toContainText("psql, firefox, chrome");
    });

    it("should show 'none specified' when agent has no resources specified", () => {
      const agent    = Agent.fromJSON(AgentsTestData.withResources());
      const agentsVM = new AgentsVM(new Agents(agent));

      mount(agentsVM);

      expect(helper.byTestId(`agent-resources-of-${agent.uuid}`)).toContainText("none specified");
    });
  });

  describe("Environments", () => {
    it("should list comma separated environments", () => {
      const agent    = Agent.fromJSON(AgentsTestData.withEnvironments("prod", "dev", "qa"));
      const agentsVM = new AgentsVM(new Agents(agent));

      mount(agentsVM);

      expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).toContainText("prod, dev, qa");
    });

    it("should show 'none specified' when agent has no resources specified", () => {
      const agent    = Agent.fromJSON(AgentsTestData.withEnvironments());
      const agentsVM = new AgentsVM(new Agents(agent));

      mount(agentsVM);

      expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).toContainText("none specified");
    });
  });

  describe("Hostname", () => {
    it("should render hostname as link when user is an admin", () => {
      mount(new AgentsVM(new Agents(agentA)), true);

      const anchor = helper.q("a", helper.byTestId(`agent-hostname-of-${agentA.uuid}`));
      expect(anchor).toContainText(agentA.hostname);
      expect(anchor).toHaveAttr("href", `/go/agents/${agentA.uuid}/job_run_history`);
    });

    it("should render hostname as text when user is not an admin", () => {
      mount(new AgentsVM(new Agents(agentA)), false);

      const hostnameCell = helper.byTestId(`agent-hostname-of-${agentA.uuid}`);
      const anchor       = helper.q("a", hostnameCell);
      expect(hostnameCell).toBeInDOM();
      expect(hostnameCell).toContainText(agentA.hostname);
      expect(anchor).not.toBeInDOM();
    });
  });

  describe("AgentSelection", () => {
    it("should not render checkboxes when user is not an admin", () => {
      mount(agentsVM, false);

      expect(helper.byTestId("select-all-agents")).not.toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).not.toBeInDOM();
    });

    it("should render checkboxes when user is an admin", () => {
      mount(agentsVM, true);

      expect(helper.byTestId("select-all-agents")).toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).toBeInDOM();
    });

    it("should render page with no agent selected", () => {
      mount(agentsVM);

      expect(helper.byTestId("select-all-agents")).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).not.toBeChecked();
    });

    it("should select all on click of global checkbox", () => {
      mount(agentsVM);
      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).not.toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).not.toBeChecked();

      helper.click(helper.byTestId("select-all-agents"));

      expect(helper.byTestId(`agent-checkbox-of-${agentA.uuid}`)).toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentB.uuid}`)).toBeChecked();
      expect(helper.byTestId(`agent-checkbox-of-${agentC.uuid}`)).toBeChecked();
    });
  });

  describe("AnalyticsIcon", () => {
    it("should not render analytics icon when attribute showAnalyticsIcon is set to false", () => {
      const pluginInfos = new PluginInfos(PluginInfo.fromJSON(AnalyticsPluginInfo.analytics()));
      mount(agentsVM, true, false, Stream(pluginInfos));

      expect(helper.byTestId(`analytics-icon-${agentA.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentB.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentC.uuid}`)).not.toBeInDOM();
    });

    it("should show analytics icon when attribute showAnalyticsIcon is set to true", () => {
      const pluginInfos = new PluginInfos(PluginInfo.fromJSON(AnalyticsPluginInfo.analytics()));

      mount(agentsVM, true, true, Stream(pluginInfos));

      expect(helper.byTestId(`analytics-icon-${agentA.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentB.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentC.uuid}`)).toBeInDOM();
    });

    it("should not render analytics icon when none of the plugin supports", () => {
      const pluginInfos = new PluginInfos(PluginInfo.fromJSON(AuthorizationPluginInfo.github()));

      mount(agentsVM, true, true, Stream(pluginInfos));

      expect(helper.byTestId(`analytics-icon-${agentA.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentB.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentC.uuid}`)).not.toBeInDOM();
    });

    it("should not render analytics icon when none of the plugin supports agent analytics", () => {
      const pipelineAnalyticsCapability = {type: "pipeline", id: "foo", title: "Foo"};
      const pluginInfoJSON              = AnalyticsPluginInfo.withCapabilities(pipelineAnalyticsCapability);
      const pluginInfos                 = new PluginInfos(PluginInfo.fromJSON(pluginInfoJSON));

      mount(agentsVM, true, true, Stream(pluginInfos));

      expect(helper.byTestId(`analytics-icon-${agentA.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentB.uuid}`)).not.toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentC.uuid}`)).not.toBeInDOM();
    });

    it("should show analytics icon when plugin supports agent analytics", () => {
      const agentAnalyticsCapability = {type: "agent", id: "foo", title: "Foo"};
      const pluginInfoJSON           = AnalyticsPluginInfo.withCapabilities(agentAnalyticsCapability);
      const pluginInfos              = new PluginInfos(PluginInfo.fromJSON(pluginInfoJSON));

      mount(agentsVM, true, true, Stream(pluginInfos));

      expect(helper.byTestId(`analytics-icon-${agentA.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentB.uuid}`)).toBeInDOM();
      expect(helper.byTestId(`analytics-icon-${agentC.uuid}`)).toBeInDOM();
    });
  });

  function mount(agentsVM: AgentsVM,
                 isUserAdmin: boolean             = true,
                 showAnalyticsIcon: boolean       = true,
                 pluginInfos: Stream<PluginInfos> = Stream(new PluginInfos())) {
    helper.mount(() => <AgentsWidget agentsVM={agentsVM}
                                     onEnable={onEnable}
                                     onDisable={onDisable}
                                     onDelete={onDelete}
                                     updateEnvironments={updateEnvironments}
                                     updateResources={updateResources}
                                     isUserAdmin={isUserAdmin}
                                     pluginInfos={pluginInfos}
                                     showAnalyticsIcon={showAnalyticsIcon}
                                     flashMessage={new FlashMessageModelWithTimeout()}/>);
  }

  function assertAgentRow(agent: Agent) {
    expect(helper.byTestId(`agent-hostname-of-${agent.uuid}`)).toContainText(agent.hostname);
    expect(helper.byTestId(`agent-sandbox-of-${agent.uuid}`)).toContainText(agent.sandbox);
    expect(helper.byTestId(`agent-operating-system-of-${agent.uuid}`)).toContainText(agent.operatingSystem);
    expect(helper.byTestId(`agent-ip-address-of-${agent.uuid}`)).toContainText(agent.ipAddress);
    expect(helper.byTestId(`agent-free-space-of-${agent.uuid}`)).toContainText(agent.readableFreeSpace());
    expect(helper.byTestId(`agent-resources-of-${agent.uuid}`))
      .toContainText(StaticAgentsWidget.joinOrNoneSpecified(agent.resources) as string);
    expect(helper.byTestId(`agent-environments-of-${agent.uuid}`))
      .toContainText(StaticAgentsWidget.joinOrNoneSpecified(agent.environmentNames()) as string);
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
