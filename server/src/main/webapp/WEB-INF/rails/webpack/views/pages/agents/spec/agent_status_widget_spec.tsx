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
import {Agent} from "models/agents/agents";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {AgentStatusWidget} from "views/pages/agents/agent_status_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AgentStatusWidget", () => {
  const helper = new TestHelper();
  let buildDetailsForAgent: Stream<string>;
  beforeEach(() => {
    buildDetailsForAgent = Stream("");
  });
  afterEach(helper.unmount.bind(helper));

  it("should render agent status", () => {
    const agent = Agent.fromJSON(AgentsTestData.disabledBuildingAgent());

    mount(agent);

    expect(helper.byTestId(`agent-status-of-${agent.uuid}`)).toContainText("Disabled (Building)");
  });

  it("should show a hyperlink when agent is building", () => {
    const agent = Agent.fromJSON(AgentsTestData.buildingAgent());
    mount(agent);

    const statusCell = helper.byTestId(`agent-status-of-${agent.uuid}`);
    expect(helper.q("a", statusCell)).toContainText("Building");

    const buildDetails     = helper.byTestId(`agent-build-details-of-${agent.uuid}`);
    const buildDetailsItem = helper.qa("a", buildDetails);
    expect(buildDetailsItem).toHaveLength(3);
    expect(buildDetailsItem[0]).toContainText("up42");
    expect(buildDetailsItem[0]).toHaveAttr("href", "pipeline_url");
    expect(buildDetailsItem[1]).toContainText("up42_stage");
    expect(buildDetailsItem[1]).toHaveAttr("href", "stage_url");
    expect(buildDetailsItem[2]).toContainText("up42_job");
    expect(buildDetailsItem[2]).toHaveAttr("href", "job_url");
  });

  it("should show build details info on click of status", () => {
    const agent = Agent.fromJSON(AgentsTestData.buildingAgent());
    mount(agent);
    expect(helper.byTestId(`agent-build-details-of-${agent.uuid}`)).not.toBeVisible();

    helper.click(helper.byTestId(`agent-status-text-${agent.uuid}`));

    expect(helper.byTestId(`agent-build-details-of-${agent.uuid}`)).toBeVisible();
  });

  it("should hide the build details when clicked on status twice", () => {
    const agent = Agent.fromJSON(AgentsTestData.buildingAgent());
    mount(agent);
    expect(helper.byTestId(`agent-build-details-of-${agent.uuid}`)).not.toBeVisible();

    helper.click(helper.byTestId(`agent-status-text-${agent.uuid}`));
    expect(helper.byTestId(`agent-build-details-of-${agent.uuid}`)).toBeVisible();

    helper.click(helper.byTestId(`agent-status-text-${agent.uuid}`));
    expect(helper.byTestId(`agent-build-details-of-${agent.uuid}`)).not.toBeVisible();
  });

  function mount(agent: Agent) {
    helper.mount(() => <AgentStatusWidget agent={agent}
                                          buildDetailsForAgent={buildDetailsForAgent}
                                          cssClasses=""/>);
  }
});
