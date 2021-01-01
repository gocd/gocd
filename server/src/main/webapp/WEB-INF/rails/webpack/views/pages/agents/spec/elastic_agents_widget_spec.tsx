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
import {Agent, Agents} from "models/agents/agents";
import {ElasticAgentVM} from "models/agents/agents_vm";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ElasticAgentsWidget} from "views/pages/agents/elastic_agents_widget";
import styles from "views/pages/agents/index.scss";
import {StaticAgentsWidget} from "views/pages/agents/static_agents_widget";
import {TestHelper} from "views/pages/spec/test_helper";
import Style from "../index.scss";

describe("NewElasticAgentsWidget", () => {
  const helper = new TestHelper();

  let elasticAgentsVM: ElasticAgentVM, staticAgent: Agent, elasticAgent: Agent;

  beforeEach(() => {
    staticAgent     = Agent.fromJSON(AgentsTestData.withOs("Windows"));
    elasticAgent    = Agent.fromJSON(AgentsTestData.elasticAgent());
    elasticAgentsVM = new ElasticAgentVM(new Agents(staticAgent, elasticAgent));
  });

  afterEach(helper.unmount.bind(helper));

  it("should render table headers", () => {
    mount(elasticAgentsVM);

    const headers = helper.byTestId("table-header-row");

    expect(headers.children).toHaveLength(8);
    expect(headers.children[1]).toContainText("Agent Name");
    expect(headers.children[2]).toContainText("Sandbox");
    expect(headers.children[3]).toContainText("OS");
    expect(headers.children[4]).toContainText("IP Address");
    expect(headers.children[5]).toContainText("Status");
    expect(headers.children[6]).toContainText("Free Space");
    expect(headers.children[7]).toContainText("Environments");
  });

  it("should render only elastic agents in the table", () => {
    mount(elasticAgentsVM);

    const tableBody = helper.byTestId("table-body");
    expect(tableBody.children).toHaveLength(1);

    assertAgentRow(elasticAgent);
  });

  it("should render plugin icon for elastic agent", () => {
    //@ts-ignore
    const elasticAgentPluginInfo = new PluginInfo(elasticAgent.elasticPluginId,
                                                  null,
                                                  "foo",
                                                  null,
                                                  null,
                                                  false,
                                                  []);
    const pluginInfos            = new PluginInfos(elasticAgentPluginInfo);
    mount(elasticAgentsVM, true, Stream(pluginInfos));

    const pluginIcon = helper.byTestId("plugin-icon");

    expect(pluginIcon).toBeInDOM();
    expect(pluginIcon.getAttribute("alt")).toBe("Plugin Icon");
    expect(pluginIcon.getAttribute("src")).toBe(elasticAgentPluginInfo.imageUrl!);
  });

  it("should not render plugin icon for elastic agent when plugin info for the specified plugin is missing", () => {
    mount(elasticAgentsVM);

    const pluginIcon = helper.byTestId("plugin-does-not-have-an-icon");

    expect(pluginIcon).toBeInDOM();
    expect(pluginIcon.getAttribute("aria-label")).toEqual("Plugin does not have an icon");
  });

  it("should not render plugin icon for elastic agent when plugin info does not contain image url", () => {
    //@ts-ignore
    const elasticAgentPluginInfo = new PluginInfo(elasticAgent.elasticPluginId,
                                                  null,
                                                  undefined,
                                                  null,
                                                  null,
                                                  false,
                                                  []);
    const pluginInfos            = new PluginInfos(elasticAgentPluginInfo);
    mount(elasticAgentsVM, true, Stream(pluginInfos));

    const pluginIcon = helper.byTestId("plugin-does-not-have-an-icon");

    expect(pluginIcon).toBeInDOM();
    expect(pluginIcon.getAttribute("aria-label")).toEqual("Plugin does not have an icon");
  });

  it("should highlight building agents", () => {
    const idleElasticAgent     = Agent.fromJSON(AgentsTestData.idleElasticAgent()),
          buildingElasticAgent = Agent.fromJSON(AgentsTestData.buildingElasticAgent());
    elasticAgentsVM            = new ElasticAgentVM(new Agents(idleElasticAgent, buildingElasticAgent));

    mount(elasticAgentsVM);

    assertAgentBuilding(buildingElasticAgent);
    assertAgentNotBuilding(idleElasticAgent);
  });

  it("should hide the build details when clicked outside", () => {
    const idleElasticAgent     = Agent.fromJSON(AgentsTestData.idleElasticAgent()),
          buildingElasticAgent = Agent.fromJSON(AgentsTestData.buildingElasticAgent());
    elasticAgentsVM            = new ElasticAgentVM(new Agents(idleElasticAgent, buildingElasticAgent));

    mount(elasticAgentsVM);

    helper.click(helper.byTestId(`agent-status-text-${buildingElasticAgent.uuid}`));
    expect(helper.byTestId(`agent-build-details-of-${buildingElasticAgent.uuid}`)).toBeVisible();

    helper.click(helper.byTestId(`agent-hostname-of-${idleElasticAgent.uuid}`));

    expect(helper.byTestId(`agent-build-details-of-${buildingElasticAgent.uuid}`)).not.toBeVisible();
  });

  describe("Hostname", () => {
    it("should render hostname as link when user is an admin", () => {
      mount(new ElasticAgentVM(new Agents(elasticAgent)), true);

      const anchor = helper.q("a", helper.byTestId(`agent-hostname-of-${elasticAgent.uuid}`));
      expect(anchor).toContainText(elasticAgent.hostname);
      expect(anchor).toHaveAttr("href", `/go/agents/${elasticAgent.uuid}/job_run_history`);
    });

    it("should render hostname as text when user is not an admin", () => {
      mount(new ElasticAgentVM(new Agents(elasticAgent)), false);

      const hostnameCell = helper.byTestId(`agent-hostname-of-${elasticAgent.uuid}`);
      const anchor       = helper.q("a", hostnameCell);
      expect(hostnameCell).toBeInDOM();
      expect(hostnameCell).toContainText(elasticAgent.hostname);
      expect(anchor).not.toBeInDOM();
    });
  });

  describe("Environments", () => {
    it("should list comma separated environments", () => {
      const elasticAgent = Agent.fromJSON(AgentsTestData.elasticAgentWithEnvironments("prod", "dev", "qa"));
      elasticAgentsVM    = new ElasticAgentVM(new Agents(elasticAgent));

      mount(elasticAgentsVM);

      expect(helper.byTestId(`agent-environments-of-${elasticAgent.uuid}`)).toContainText("prod, dev, qa");
    });

    it("should show 'none specified' when agent has no resources specified", () => {
      const agent     = Agent.fromJSON(AgentsTestData.elasticAgentWithEnvironments());
      elasticAgentsVM = new ElasticAgentVM(new Agents(agent));

      mount(elasticAgentsVM);

      expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).toContainText("none specified");
    });
  });

  describe("HeaderPanel", () => {
    it("should render a search box", () => {
      const agentsVM = new ElasticAgentVM(new Agents());
      mount(agentsVM);

      const searchBox = helper.byTestId("form-field-input-search-for-agents");

      expect(searchBox).toBeInDOM();
    });

    it("should filter agents based on the searched value", () => {
      const idleElasticAgent     = Agent.fromJSON(AgentsTestData.idleElasticAgent()),
            buildingElasticAgent = Agent.fromJSON(AgentsTestData.buildingElasticAgent());
      elasticAgentsVM            = new ElasticAgentVM(new Agents(idleElasticAgent, buildingElasticAgent));

      mount(elasticAgentsVM);
      const searchBox = helper.byTestId("form-field-input-search-for-agents");

      expect(elasticAgentsVM.list()).toHaveLength(2);

      helper.oninput(searchBox, "build");

      expect(elasticAgentsVM.list()).toHaveLength(1);
    });

    it("should render agents overview based on agent config state", () => {
      const idleElasticAgent     = Agent.fromJSON(AgentsTestData.idleElasticAgent()),
            buildingElasticAgent = Agent.fromJSON(AgentsTestData.buildingElasticAgent());
      elasticAgentsVM            = new ElasticAgentVM(new Agents(idleElasticAgent, buildingElasticAgent));

      mount(elasticAgentsVM);

      expect(helper.byTestId("key-value-key-total")).toBeInDOM();
      expect(helper.byTestId("key-value-key-pending")).toBeInDOM();
      expect(helper.byTestId("key-value-key-enabled")).toBeInDOM();
      expect(helper.byTestId("key-value-key-disabled")).toBeInDOM();

      expect(helper.byTestId("key-value-value-total")).toHaveText("2");
      expect(helper.byTestId("key-value-value-pending")).toHaveText("0");
      expect(helper.byTestId("key-value-value-enabled")).toHaveText("2");
      expect(helper.byTestId("key-value-value-disabled")).toHaveText("0");

      expect(helper.q("span", helper.byTestId("key-value-value-enabled"))).toHaveClass(Style.enabled);
      expect(helper.q("span", helper.byTestId("key-value-value-disabled"))).toHaveClass(Style.disabled);
    });

    it("should render agents overview based on agent config state on filtered agents", () => {
      const idleElasticAgent     = Agent.fromJSON(AgentsTestData.idleElasticAgent()),
            buildingElasticAgent = Agent.fromJSON(AgentsTestData.buildingElasticAgent());
      elasticAgentsVM            = new ElasticAgentVM(new Agents(idleElasticAgent, buildingElasticAgent));

      mount(elasticAgentsVM);

      elasticAgentsVM.filterText("building");
      m.redraw.sync();

      expect(helper.byTestId("key-value-value-total")).toHaveText("1");
      expect(helper.byTestId("key-value-value-pending")).toHaveText("0");
      expect(helper.byTestId("key-value-value-enabled")).toHaveText("1");
      expect(helper.byTestId("key-value-value-disabled")).toHaveText("0");
    });
  });

  function mount(elasticAgent: ElasticAgentVM,
                 isUserAdmin: boolean             = true,
                 pluginInfos: Stream<PluginInfos> = Stream(new PluginInfos())) {
    helper.mount(() => <ElasticAgentsWidget agentsVM={elasticAgentsVM}
                                            pluginInfos={pluginInfos}
                                            isUserAdmin={isUserAdmin}/>);
  }

  function assertAgentRow(agent: Agent) {
    expect(helper.byTestId(`agent-hostname-of-${agent.uuid}`)).toContainText(agent.hostname);
    expect(helper.byTestId(`agent-sandbox-of-${agent.uuid}`)).toContainText(agent.sandbox);
    expect(helper.byTestId(`agent-operating-system-of-${agent.uuid}`)).toContainText(agent.operatingSystem);
    expect(helper.byTestId(`agent-ip-address-of-${agent.uuid}`)).toContainText(agent.ipAddress);
    expect(helper.byTestId(`agent-free-space-of-${agent.uuid}`)).toContainText(agent.readableFreeSpace());
    expect(helper.byTestId(`agent-environments-of-${agent.uuid}`))
      .toContainText(StaticAgentsWidget.joinOrNoneSpecified(agent.environmentNames()) as string);
  }

  function assertAgentBuilding(agent: Agent) {
    expect(helper.byTestId(`agent-hostname-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-sandbox-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-operating-system-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-ip-address-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-free-space-of-${agent.uuid}`)).toHaveClass(styles.building);
    expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).toHaveClass(styles.building);
  }

  function assertAgentNotBuilding(agent: Agent) {
    expect(helper.byTestId(`agent-hostname-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-sandbox-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-operating-system-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-ip-address-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-free-space-of-${agent.uuid}`)).not.toHaveClass(styles.building);
    expect(helper.byTestId(`agent-environments-of-${agent.uuid}`)).not.toHaveClass(styles.building);
  }
});
