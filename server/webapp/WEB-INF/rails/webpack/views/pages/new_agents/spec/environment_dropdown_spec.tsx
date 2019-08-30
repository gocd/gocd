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
import {Agent, AgentsEnvironment, Agents} from "models/new_agent/agents";
import {GetAllService} from "models/new_agent/agents_crud";
import {AgentsVM} from "models/new_agent/agents_vm";
import {AgentsTestData} from "models/new_agent/spec/agents_test_data";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {EnvironmentsDropdownButton} from "views/pages/new_agents/environment_dropdown_button";
import {TestHelper} from "views/pages/spec/test_helper";

describe("EnvironmentsDropdownButton", () => {
  const helper             = new TestHelper(),
        updateEnvironments = jasmine.createSpy("updateEnvironments");

  let agent: Agent, agentsVM: AgentsVM;
  beforeEach(() => {
    agent    = Agent.fromJSON(AgentsTestData.idleAgent());
    agentsVM = new AgentsVM(new Agents(agent));
  });

  afterEach(helper.unmount.bind(helper));

  it("should render disable environment button when no agent is selected", () => {
    mount(agentsVM, new DummyService([]));

    expect(helper.byTestId("modify-environments-association")).toBeInDOM();
    expect(helper.byTestId("modify-environments-association")).toBeDisabled();
  });

  it("should enable environment button when agent is selected", () => {
    agentsVM.selectAgent(agent.uuid);
    mount(agentsVM, new DummyService([]));

    expect(helper.byTestId("modify-environments-association")).not.toBeDisabled();
  });

  it("should set showResources to false on agents when button is clicked", () => {
    agentsVM.selectAgent(agent.uuid);
    agentsVM.showResources(true);
    mount(agentsVM, new DummyService([]));

    expect(agentsVM.showResources()).toBeTruthy();
    expect(agentsVM.showEnvironments()).toBeFalsy();

    helper.clickByDataTestId("modify-environments-association");

    expect(agentsVM.showResources()).toBeFalsy();
    expect(agentsVM.showEnvironments()).toBeTruthy();
  });

  it("should render dropdown on click of environment button", () => {
    agentsVM.selectAgent(agent.uuid);
    mount(agentsVM, new DummyService([]));

    helper.clickByDataTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toBeInDOM();
  });

  it("should render spinner on click of the button while fetching the data", () => {
    agentsVM.selectAgent(agent.uuid);

    class MockService implements GetAllService {
      all(onSuccess: (data: string) => void, onError: (message: string) => void): void {
        m.redraw.sync();
        expect(helper.byTestId("spinner")).toBeInDOM();
        onSuccess(JSON.stringify(["Prod"]));
      }
    }

    mount(agentsVM, new MockService());

    helper.clickByDataTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("spinner")).not.toBeInDOM();
  });

  it("should show the message if no environments are available", () => {
    agentsVM.selectAgent(agent.uuid);
    mount(agentsVM, new DummyService([]));

    helper.clickByDataTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toContainText("No environments are defined.");
    expect(helper.byTestId("environment-to-apply")).not.toBeInDOM();
  });

  it("should render dropdown with environments when environments are available", () => {
    agentsVM.selectAgent(agent.uuid);
    mount(agentsVM, new DummyService(["prod", "test"]));

    helper.clickByDataTestId("modify-environments-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toContainText("prod");
    expect(helper.byTestId("association")).toContainText("test");
    expect(helper.byTestId("environment-to-apply")).toBeInDOM();
  });

  describe("TriStateCheckBox", () => {
    it("should render checkboxes unselected when none of the agents is associated with environments ", () => {
      agentsVM.selectAgent(agent.uuid);
      mount(agentsVM, new DummyService(["prod", "test"]));

      helper.clickByDataTestId("modify-environments-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-prod")).not.toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes selected when the agents is associated with environments ", () => {
      agentsVM.selectAgent(agent.uuid);
      agent.environments.push(new AgentsEnvironment("prod", "gocd"));
      mount(agentsVM, new DummyService(["prod", "test"]));

      helper.clickByDataTestId("modify-environments-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-prod")).toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes indeterminate when the agents is associated with environments ", () => {
      const agentWithoutEnv = Agent.fromJSON(AgentsTestData.buildingAgent());
      agentsVM = new AgentsVM(new Agents(agent, agentWithoutEnv));
      agentsVM.selectAgent(agentWithoutEnv.uuid);
      agentsVM.selectAgent(agent.uuid);
      agent.environments.push(new AgentsEnvironment("prod", "gocd"));
      mount(agentsVM, new DummyService(["prod", "test"]));

      helper.clickByDataTestId("modify-environments-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-prod")).toHaveProp("indeterminate", true);
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });
  });

  function mount(agentsVM: AgentsVM, dummyService: GetAllService) {
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
