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
import {Agent, Agents} from "models/agents/agents";
import {GetAllService} from "models/agents/agents_crud";
import {StaticAgentsVM} from "models/agents/agents_vm";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {ResourcesDropdownButton} from "views/pages/agents/resources_dropdown_button";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ResourcesDropdownButton", () => {
  const helper          = new TestHelper(),
        updateResources = jasmine.createSpy("updateResources");

  let agent: Agent, agentsVM: StaticAgentsVM;
  beforeEach(() => {
    agent    = Agent.fromJSON(AgentsTestData.idleAgent());
    agentsVM = new StaticAgentsVM(new Agents(agent));
  });

  afterEach(helper.unmount.bind(helper));

  it("should render disable resources button when no agent is checked", () => {
    mount(agentsVM, new DummyService([]));

    expect(helper.byTestId("modify-resources-association")).toBeInDOM();
    expect(helper.byTestId("modify-resources-association")).toBeDisabled();
  });

  it("should enable resources button when agent is checked", () => {
    agentsVM.selectAgent(agent.uuid);
    mount(agentsVM, new DummyService([]));

    expect(helper.byTestId("modify-resources-association")).not.toBeDisabled();
  });

  it("should set showEnvironments to false on agents when button is clicked", () => {
    agentsVM.selectAgent(agent.uuid);
    agentsVM.showEnvironments(true);
    mount(agentsVM, new DummyService([]));

    expect(agentsVM.showEnvironments()).toBeTruthy();
    expect(agentsVM.showResources()).toBeFalsy();

    helper.clickByTestId("modify-resources-association");

    expect(agentsVM.showEnvironments()).toBeFalsy();
    expect(agentsVM.showResources()).toBeTruthy();
  });

  it("should render dropdown on click of resources button", () => {
    agentsVM.selectAgent(agent.uuid);
    mount(agentsVM, new DummyService([]));

    helper.clickByTestId("modify-resources-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toBeInDOM();
  });

  it("should render dropdown with resources when resources are available", () => {
    agentsVM.selectAgent(agent.uuid);
    mount(agentsVM, new DummyService(["firefox", "chrome"]));

    helper.clickByTestId("modify-resources-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toContainText("firefox");
    expect(helper.byTestId("association")).toContainText("chrome");
    expect(helper.byTestId("resource-to-add")).toBeInDOM();
    expect(helper.byTestId("resources-to-apply")).toBeInDOM();
  });

  describe("TriStateCheckBox", () => {
    it("should render checkboxes unchecked when none of the agents is associated with resources ", () => {
      agentsVM.selectAgent(agent.uuid);
      mount(agentsVM, new DummyService(["chrome", "test"]));

      helper.clickByTestId("modify-resources-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).not.toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes checked when the agents is associated with resources ", () => {
      agentsVM.selectAgent(agent.uuid);
      agent.resources.push("chrome");
      mount(agentsVM, new DummyService(["chrome", "test"]));

      helper.clickByTestId("modify-resources-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes indeterminate when the agents is associated with resources ", () => {
      const agentWithoutResource = Agent.fromJSON(AgentsTestData.buildingAgent());
      agentsVM                   = new StaticAgentsVM(new Agents(agent, agentWithoutResource));
      agentsVM.selectAgent(agentWithoutResource.uuid);
      agentsVM.selectAgent(agent.uuid);
      agent.resources.push("chrome");
      mount(agentsVM, new DummyService(["chrome", "test"]));

      helper.clickByTestId("modify-resources-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).toHaveProp("indeterminate", true);
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should add new resource and render checkbox in checked state", () => {
      agentsVM.selectAgent(agent.uuid);
      mount(agentsVM, new DummyService(["chrome"]));

      helper.clickByTestId("modify-resources-association");
      m.redraw.sync();
      helper.oninput("input", "firefox", helper.byTestId("resource-to-add"));
      m.redraw.sync();
      helper.click("button", helper.byTestId("resource-to-add"));
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).not.toBeChecked();
      expect(helper.byTestId("form-field-input-firefox")).toBeChecked();
    });
  });

  function mount(agentsVM: StaticAgentsVM, dummyService: GetAllService) {
    helper.mount(() => <ResourcesDropdownButton agentsVM={agentsVM}
                                                updateResources={updateResources}
                                                show={agentsVM.showResources}
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
