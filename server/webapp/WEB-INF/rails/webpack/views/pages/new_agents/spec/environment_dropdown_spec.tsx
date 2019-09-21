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
import {Agent, Agents, AgentsEnvironment} from "models/new_agent/agents";
import {GetAllService} from "models/new_agent/agents_crud";
import {StaticAgentsVM} from "models/new_agent/agents_vm";
import {AgentsTestData} from "models/new_agent/spec/agents_test_data";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {EnvironmentsDropdownButton} from "views/pages/new_agents/environment_dropdown_button";
import {TestHelper} from "views/pages/spec/test_helper";

describe("EnvironmentsDropdownButton", () => {
  const helper             = new TestHelper(),
        updateEnvironments = jasmine.createSpy("updateEnvironments");

  let agent: Agent, staticAgentsVM: StaticAgentsVM;
  beforeEach(() => {
    agent    = Agent.fromJSON(AgentsTestData.idleAgent());
    staticAgentsVM = new StaticAgentsVM(new Agents(agent));
  });

  afterEach(helper.unmount.bind(helper));

  it("should render disable environment button when no agent is selected", () => {
    mount(staticAgentsVM, new DummyService([]));

    expect(helper.byTestId("modify-environments-association")).toBeInDOM();
    expect(helper.byTestId("modify-environments-association")).toBeDisabled();
  });

  it("should enable environment button when agent is selected", () => {
    staticAgentsVM.selectAgent(agent.uuid);
    mount(staticAgentsVM, new DummyService([]));

    expect(helper.byTestId("modify-environments-association")).not.toBeDisabled();
  });

  it("should set showResources to false on agents when button is clicked", () => {
    staticAgentsVM.selectAgent(agent.uuid);
    staticAgentsVM.showResources(true);
    mount(staticAgentsVM, new DummyService([]));

    expect(staticAgentsVM.showResources()).toBeTruthy();
    expect(staticAgentsVM.showEnvironments()).toBeFalsy();

    helper.clickByTestId("modify-environments-association");

    expect(staticAgentsVM.showResources()).toBeFalsy();
    expect(staticAgentsVM.showEnvironments()).toBeTruthy();
  });

  it("should render dropdown on click of environment button", () => {
    staticAgentsVM.selectAgent(agent.uuid);
    mount(staticAgentsVM, new DummyService([]));

    helper.clickByTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toBeInDOM();
  });

  it("should render spinner on click of the button while fetching the data", () => {
    staticAgentsVM.selectAgent(agent.uuid);

    class MockService implements GetAllService {
      all(onSuccess: (data: string) => void, onError: (message: string) => void): void {
        m.redraw.sync();
        expect(helper.byTestId("spinner")).toBeInDOM();
        onSuccess(JSON.stringify(["Prod"]));
      }
    }

    mount(staticAgentsVM, new MockService());

    helper.clickByTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("spinner")).not.toBeInDOM();
  });

  it("should show the message if no environments are available", () => {
    staticAgentsVM.selectAgent(agent.uuid);
    mount(staticAgentsVM, new DummyService([]));

    helper.clickByTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toContainText("No environments are defined.");
    expect(helper.byTestId("environment-to-apply")).not.toBeInDOM();
  });

  it("should render dropdown with environments when environments are available", () => {
    staticAgentsVM.selectAgent(agent.uuid);
    mount(staticAgentsVM, new DummyService(["prod", "test"]));

    helper.clickByTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toContainText("prod");
    expect(helper.byTestId("association")).toContainText("test");
    expect(helper.byTestId("environment-to-apply")).toBeInDOM();
  });

  describe("TriStateCheckBox", () => {
    it("should render checkboxes unselected when none of the agents is associated with environments ", () => {
      staticAgentsVM.selectAgent(agent.uuid);
      mount(staticAgentsVM, new DummyService(["prod", "test"]));

      helper.clickByTestId("modify-environments-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-prod")).not.toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes selected when the agents is associated with environments ", () => {
      staticAgentsVM.selectAgent(agent.uuid);
      agent.environments.push(new AgentsEnvironment("prod", "gocd"));
      mount(staticAgentsVM, new DummyService(["prod", "test"]));

      helper.clickByTestId("modify-environments-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-prod")).toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes indeterminate when the agents is associated with environments ", () => {
      const agentWithoutEnv = Agent.fromJSON(AgentsTestData.buildingAgent());
      staticAgentsVM = new StaticAgentsVM(new Agents(agent, agentWithoutEnv));
      staticAgentsVM.selectAgent(agentWithoutEnv.uuid);
      staticAgentsVM.selectAgent(agent.uuid);
      agent.environments.push(new AgentsEnvironment("prod", "gocd"));
      mount(staticAgentsVM, new DummyService(["prod", "test"]));

      helper.clickByTestId("modify-environments-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-prod")).toHaveProp("indeterminate", true);
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });
  });

  function mount(agentsVM: StaticAgentsVM, dummyService: GetAllService) {
    helper.mount(() => <EnvironmentsDropdownButton agentsVM={agentsVM}
                                                   updateEnvironments={updateEnvironments}
                                                   show={agentsVM.showEnvironments}
                                                   flashMessage={new FlashMessageModelWithTimeout()}
                                                   service={dummyService}/>);
  }
});

class DummyService implements GetAllService {
  private readonly data: string[];

  constructor(data: string[]) {
    this.data = data;
  }

  all(onSuccess: (data: string) => void, onError: (message: string) => void): void {
    onSuccess(JSON.stringify(this.data));
  }
}
