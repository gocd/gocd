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

import {Agent, Agents} from "models/new-agent/agents";
import {AgentsTestData} from "models/new-agent/spec/agents_test_data";
import {AgentHeaderPanel} from "views/pages/new-agents/agent_header_panel";
import {TestHelper} from "views/pages/spec/test_helper";
import Style from "../index.scss";
import m from "mithril";

describe("AgentHeaderPanel", () => {
  const helper    = new TestHelper(),
        onEnable  = jasmine.createSpy("onEnable"),
        onDisable = jasmine.createSpy("onDisable"),
        onDelete  = jasmine.createSpy("onDelete");

  afterEach(helper.unmount.bind(helper));

  it("should render agents overview based on agent config state", () => {
    const agentA = Agent.fromJSON(AgentsTestData.pendingAgent()),
          agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentD = Agent.fromJSON(AgentsTestData.disabledAgent());
    const agents = new Agents([agentA, agentB, agentC, agentD]);
    mount(agents);

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
    const agentA = Agent.fromJSON(AgentsTestData.pendingAgent()),
          agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
          agentC = Agent.fromJSON(AgentsTestData.idleAgent()),
          agentD = Agent.fromJSON(AgentsTestData.disabledAgent());
    const agents = new Agents([agentA, agentB, agentC, agentD]);
    mount(agents);

    agents.filterText("building");
    m.redraw.sync();

    expect(helper.byTestId("key-value-value-total")).toHaveText("1");
    expect(helper.byTestId("key-value-value-pending")).toHaveText("0");
    expect(helper.byTestId("key-value-value-enabled")).toHaveText("1");
    expect(helper.byTestId("key-value-value-disabled")).toHaveText("0");
  });


  function mount(agents: Agents) {
    helper.mount(() => <AgentHeaderPanel agents={agents}
                                         onDelete={onDelete}
                                         onEnable={onEnable}
                                         onDisable={onDisable}/>);
  }

});