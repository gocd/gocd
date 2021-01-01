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
import {Agent} from "models/agents/agents";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {AnalyticsCapability} from "models/shared/plugin_infos_new/analytics_plugin_capabilities";
import {AgentAnalyticsWidget} from "views/pages/agents/agent_analytics_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AgentAnalyticsWidget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render analytics icon", () => {
    const agent = Agent.fromJSON(AgentsTestData.idleAgent());
    mount(agent, {} as { [key: string]: AnalyticsCapability[] });

    expect(helper.byTestId(`analytics-icon-${agent.uuid}`)).toBeInDOM();
  });

  it("should open a modal on click of analytics icon", () => {
    const agent = Agent.fromJSON(AgentsTestData.idleAgent());
    mount(agent, {} as { [key: string]: AnalyticsCapability[] });

    helper.clickByTestId(`analytics-icon-${agent.uuid}`);
    m.redraw.sync();

    expect(helper.fromModalByTestId("modal-title")).toBeInDOM();
    expect(helper.fromModalByTestId("modal-title")).toHaveText(`Analytics for agent: ${agent.hostname}`);

    helper.closeModal();
  });

  function mount(agent: Agent, supportedAnalytics: { [key: string]: AnalyticsCapability[] }) {
    helper.mount(() => <AgentAnalyticsWidget agent={agent}
                                             supportedAnalytics={supportedAnalytics}/>);
  }
});
