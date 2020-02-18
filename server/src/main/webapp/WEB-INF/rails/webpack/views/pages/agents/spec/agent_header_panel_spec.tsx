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
import {Agent, Agents} from "models/agents/agents";
import {StaticAgentsVM} from "models/agents/agents_vm";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {AgentHeaderPanel} from "views/pages/agents/agent_header_panel";
import {TestHelper} from "views/pages/spec/test_helper";
import Style from "../index.scss";

describe("AgentHeaderPanel", () => {
  const helper             = new TestHelper(),
        onEnable           = jasmine.createSpy("onEnable"),
        onDisable          = jasmine.createSpy("onDisable"),
        onDelete           = jasmine.createSpy("onDelete"),
        updateEnvironments = jasmine.createSpy("updateEnvironments"),
        updateResources    = jasmine.createSpy("updateResources"),
        flashMessage       = new FlashMessageModelWithTimeout();

  afterEach(helper.unmount.bind(helper));

  it("should render agents overview based on agent config state", () => {
    const agentA   = Agent.fromJSON(AgentsTestData.pendingAgent()),
          agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC   = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentD   = Agent.fromJSON(AgentsTestData.disabledAgent());
    const agentsVM = new StaticAgentsVM(new Agents(agentA, agentB, agentC, agentD));
    mount(agentsVM);

    expect(helper.byTestId("key-value-key-total")).toBeInDOM();
    expect(helper.byTestId("key-value-key-pending")).toBeInDOM();
    expect(helper.byTestId("key-value-key-enabled")).toBeInDOM();
    expect(helper.byTestId("key-value-key-disabled")).toBeInDOM();

    expect(helper.byTestId("key-value-value-total")).toHaveText("4");
    expect(helper.byTestId("key-value-value-pending")).toHaveText("1");
    expect(helper.byTestId("key-value-value-enabled")).toHaveText("2");
    expect(helper.byTestId("key-value-value-disabled")).toHaveText("1");

    expect(helper.q("span", helper.byTestId("key-value-value-enabled"))).toHaveClass(Style.enabled);
    expect(helper.q("span", helper.byTestId("key-value-value-disabled"))).toHaveClass(Style.disabled);
  });

  it("should render agents overview based on agent config state on filtered agents", () => {
    const agentA   = Agent.fromJSON(AgentsTestData.pendingAgent()),
          agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC   = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentD   = Agent.fromJSON(AgentsTestData.disabledAgent());
    const agentsVM = new StaticAgentsVM(new Agents(agentA, agentB, agentC, agentD));
    mount(agentsVM);

    agentsVM.filterText("building");
    m.redraw.sync();

    expect(helper.byTestId("key-value-value-total")).toHaveText("1");
    expect(helper.byTestId("key-value-value-pending")).toHaveText("0");
    expect(helper.byTestId("key-value-value-enabled")).toHaveText("1");
    expect(helper.byTestId("key-value-value-disabled")).toHaveText("0");
  });

  describe("Actions", () => {
    it("should render disabled action buttons when no agent is selected", () => {
      const agentA   = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC   = Agent.fromJSON(AgentsTestData.idleAgent());
      const agentsVM = new StaticAgentsVM(new Agents(agentA, agentB, agentC));
      mount(agentsVM);

      expect(helper.byTestId("delete-agents")).toBeDisabled();
      expect(helper.byTestId("enable-agents")).toBeDisabled();
      expect(helper.byTestId("disable-agents")).toBeDisabled();
    });

    it("should enable the actions when one of the agent is selected", () => {
      const agentA   = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC   = Agent.fromJSON(AgentsTestData.idleAgent());
      const agentsVM = new StaticAgentsVM(new Agents(agentA, agentB, agentC));
      mount(agentsVM);

      expect(helper.byTestId("delete-agents")).toBeDisabled();
      expect(helper.byTestId("enable-agents")).toBeDisabled();
      expect(helper.byTestId("disable-agents")).toBeDisabled();

      agentsVM.selectAgent(agentA.uuid);
      m.redraw.sync();

      expect(helper.byTestId("delete-agents")).not.toBeDisabled();
      expect(helper.byTestId("enable-agents")).not.toBeDisabled();
      expect(helper.byTestId("disable-agents")).not.toBeDisabled();
    });

    it("should call onDelete on click of delete button", () => {
      const agentA   = Agent.fromJSON(AgentsTestData.pendingAgent());
      const agentsVM = new StaticAgentsVM(new Agents(agentA));
      agentsVM.selectAgent(agentA.uuid);
      mount(agentsVM);

      helper.clickByTestId("delete-agents");

      expect(onDelete).toHaveBeenCalled();
    });

    it("should call onEnable when enable button is clicked", () => {
      const agentA   = Agent.fromJSON(AgentsTestData.pendingAgent());
      const agentsVM = new StaticAgentsVM(new Agents(agentA));
      agentsVM.selectAgent(agentA.uuid);
      mount(agentsVM);

      helper.clickByTestId("enable-agents");

      expect(onEnable).toHaveBeenCalled();
    });

    it("should call onDisable when disable button is clicked", () => {
      const agentA   = Agent.fromJSON(AgentsTestData.idleAgent());
      const agentsVM = new StaticAgentsVM(new Agents(agentA));
      agentsVM.selectAgent(agentA.uuid);
      mount(agentsVM);

      helper.clickByTestId("disable-agents");

      expect(onDisable).toHaveBeenCalled();
    });
  });

  it("should render a search box", () => {
    const agentsVM = new StaticAgentsVM(new Agents());
    mount(agentsVM);

    const searchBox = helper.byTestId("form-field-input-search-for-agents");

    expect(searchBox).toBeInDOM();
  });

  it("should filter agents based on the searched value", () => {
    const agentA   = Agent.fromJSON(AgentsTestData.withOs("Windows")),
          agentB   = Agent.fromJSON(AgentsTestData.withOs("MacOS")),
          agentC   = Agent.fromJSON(AgentsTestData.withOs("Linux")),
          agentsVM = new StaticAgentsVM(new Agents(agentA, agentB, agentC));
    mount(agentsVM);
    const searchBox = helper.byTestId("form-field-input-search-for-agents");

    expect(agentsVM.list()).toHaveLength(3);

    helper.oninput(searchBox, "wind");

    expect(agentsVM.list()).toHaveLength(1);
  });

  function mount(staticAgentsVM: StaticAgentsVM) {
    helper.mount(() => <AgentHeaderPanel agentsVM={staticAgentsVM}
                                         onDelete={onDelete}
                                         onEnable={onEnable}
                                         onDisable={onDisable}
                                         flashMessage={flashMessage}
                                         updateEnvironments={updateEnvironments}
                                         updateResources={updateResources}/>);
  }

});
