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
import {Agent, Agents} from "models/new_agent/agents";
import {GetAllService} from "models/new_agent/agents_crud";
import {AgentsTestData} from "models/new_agent/spec/agents_test_data";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {ResourcesDropdownButton} from "views/pages/new_agents/resources_dropdown_button";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ResourcesDropdownButton", () => {
  const helper          = new TestHelper(),
        updateResources = jasmine.createSpy("updateResources");

  let agent: Agent, agents: Agents;
  beforeEach(() => {
    agent  = Agent.fromJSON(AgentsTestData.idleAgent());
    agents = new Agents([agent]);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render disable resources button when no agent is checked", () => {
    mount(agents, new DummyService([]));

    expect(helper.byTestId("modify-resources-association")).toBeInDOM();
    expect(helper.byTestId("modify-resources-association")).toBeDisabled();
  });

  it("should enable resources button when agent is checked", () => {
    agent.selected(true);
    mount(agents, new DummyService([]));

    expect(helper.byTestId("modify-resources-association")).not.toBeDisabled();
  });

  it("should set showEnvironments to false on agents when button is clicked", () => {
    agent.selected(true);
    agents.showEnvironments(true);
    mount(agents, new DummyService([]));

    expect(agents.showEnvironments()).toBeTruthy();
    expect(agents.showResources()).toBeFalsy();

    helper.clickByDataTestId("modify-resources-association");

    expect(agents.showEnvironments()).toBeFalsy();
    expect(agents.showResources()).toBeTruthy();
  });

  it("should render dropdown on click of resources button", () => {
    agent.selected(true);
    mount(agents, new DummyService([]));

    helper.clickByDataTestId("modify-resources-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toBeInDOM();
  });

  it("should render dropdown with resources when resources are available", () => {
    agent.selected(true);
    mount(agents, new DummyService(["firefox", "chrome"]));

    helper.clickByDataTestId("modify-resources-association");
    m.redraw.sync();

    expect(helper.byTestId("association")).toContainText("firefox");
    expect(helper.byTestId("association")).toContainText("chrome");
    expect(helper.byTestId("resource-to-add")).toBeInDOM();
    expect(helper.byTestId("resources-to-apply")).toBeInDOM();
  });

  describe("TriStateCheckBox", () => {
    it("should render checkboxes unchecked when none of the agents is associated with resources ", () => {
      agent.selected(true);
      mount(agents, new DummyService(["chrome", "test"]));

      helper.clickByDataTestId("modify-resources-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).not.toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes checked when the agents is associated with resources ", () => {
      agent.selected(true);
      agent.resources.push("chrome");
      mount(agents, new DummyService(["chrome", "test"]));

      helper.clickByDataTestId("modify-resources-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).toBeChecked();
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should render checkboxes indeterminate when the agents is associated with resources ", () => {
      const agentWithoutResource = Agent.fromJSON(AgentsTestData.buildingAgent());
      agentWithoutResource.selected(true);
      agent.selected(true);
      agent.resources.push("chrome");
      agents = new Agents([agent, agentWithoutResource]);
      mount(agents, new DummyService(["chrome", "test"]));

      helper.clickByDataTestId("modify-resources-association");
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).toHaveProp("indeterminate", true);
      expect(helper.byTestId("form-field-input-test")).not.toBeChecked();
    });

    it("should add new resource and render checkbox in checked state", () => {
      agent.selected(true);
      mount(agents, new DummyService(["chrome"]));

      helper.clickByDataTestId("modify-resources-association");
      m.redraw.sync();
      helper.oninput("input", "firefox", helper.byTestId("resource-to-add"));
      m.redraw.sync();
      helper.click("button", helper.byTestId("resource-to-add"));
      m.redraw.sync();

      expect(helper.byTestId("form-field-input-chrome")).not.toBeChecked();
      expect(helper.byTestId("form-field-input-firefox")).toBeChecked();
    });
  });

  function mount(agents: Agents, dummyService: GetAllService) {
    helper.mount(() => <ResourcesDropdownButton agents={agents}
                                                updateResources={updateResources}
                                                show={agents.showResources}
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
