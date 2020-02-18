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
import {Agents} from "models/agents/agents";
import {AgentsJSON} from "models/agents/agents_json";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import test_data from "models/new-environments/spec/test_data";
import {ModalState} from "views/components/modal";
import {EditAgentsModal} from "views/pages/new-environments/edit_agents_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Edit Agents Modal", () => {
  const helper = new TestHelper();

  let environment: EnvironmentWithOrigin,
      environments: Environments,
      agentsJSON: AgentsJSON;

  let normalAgentAssociatedWithEnvInXml: string,
      normalAgentAssociatedWithEnvInConfigRepo: string,
      elasticAgentAssociatedWithEnvInXml: string,
      unassociatedStaticAgent: string,
      unassociatedElasticAgent: string;

  let modal: EditAgentsModal;

  beforeEach(() => {
    jasmine.Ajax.install();

    environments          = new Environments();
    const environmentJSON = test_data.environment_json();
    environmentJSON.agents.push(test_data.agent_association_in_xml_json());

    environment = EnvironmentWithOrigin.fromJSON(environmentJSON);
    environments.push(environment);

    agentsJSON = AgentsTestData.list();

    agentsJSON._embedded.agents.push(AgentsTestData.elasticAgent());
    agentsJSON._embedded.agents.push(AgentsTestData.elasticAgent());

    //normal agent associated with environment in xml
    normalAgentAssociatedWithEnvInXml   = environmentJSON.agents[0].uuid;
    agentsJSON._embedded.agents[0].uuid = normalAgentAssociatedWithEnvInXml;

    //normal agent associated with environment in config repo
    normalAgentAssociatedWithEnvInConfigRepo = environmentJSON.agents[1].uuid;
    agentsJSON._embedded.agents[1].uuid      = normalAgentAssociatedWithEnvInConfigRepo;

    //elastic agent associated with environment in xml
    elasticAgentAssociatedWithEnvInXml  = environmentJSON.agents[2].uuid;
    agentsJSON._embedded.agents[4].uuid = elasticAgentAssociatedWithEnvInXml;

    unassociatedStaticAgent  = agentsJSON._embedded.agents[2].uuid;
    unassociatedElasticAgent = agentsJSON._embedded.agents[3].uuid;

    modal = new EditAgentsModal(environment,
                                environments,
                                Agents.fromJSON(agentsJSON),
                                jasmine.createSpy("onSuccessfulSave"));

    helper.mount(() => modal.view());
  });

  afterEach(() => {
    helper.unmount();
    jasmine.Ajax.uninstall();
  });

  it("should render available agents", () => {
    const availableAgentsSection = helper.byTestId(`available-agents`);
    const agent1Selector         = `agent-checkbox-for-${normalAgentAssociatedWithEnvInXml}`;
    const agent2Selector         = `agent-checkbox-for-${unassociatedStaticAgent}`;

    expect(availableAgentsSection).toBeInDOM();
    expect(availableAgentsSection).toContainText("Available Agents");
    expect(helper.byTestId(agent1Selector, availableAgentsSection)).toBeInDOM();
    expect(helper.byTestId(agent2Selector, availableAgentsSection)).toBeInDOM();
  });

  it("should render agents defined in config repo", () => {
    const configRepoAgentsSection = helper.byTestId(`agents-associated-with-this-environment-in-configuration-repository`);
    const agent1Selector          = `agent-checkbox-for-${normalAgentAssociatedWithEnvInConfigRepo}`;

    expect(configRepoAgentsSection).toBeInDOM();
    expect(configRepoAgentsSection)
      .toContainText("Agents associated with this environment in configuration repository");
    expect(helper.byTestId(agent1Selector, configRepoAgentsSection)).toBeInDOM();
  });

  it("should render elastic agents associated with the current environment", () => {
    const elasticAgentsSection = helper.byTestId(`elastic-agents-associated-with-this-environment`);
    const agent1Selector       = `agent-checkbox-for-${elasticAgentAssociatedWithEnvInXml}`;

    expect(elasticAgentsSection).toBeInDOM();
    expect(elasticAgentsSection).toContainText("Elastic Agents associated with this environment");
    expect(helper.byTestId(agent1Selector, elasticAgentsSection)).toBeInDOM();
  });

  it("should render unavailable elastic agents not belonging to the current environment", () => {
    const elasticAgentsSection = helper.byTestId(`unavailable-agents-elastic-agents`);
    const agent1Selector       = `agent-list-item-for-${unassociatedElasticAgent}`;

    expect(elasticAgentsSection).toBeInDOM();
    expect(elasticAgentsSection).toContainText("Unavailable Agents (Elastic Agents)");
    expect(helper.byTestId(agent1Selector, elasticAgentsSection)).toBeInDOM();
  });

  it("should toggle agent selection from environment on click", () => {
    const agent1Checkbox      = helper.byTestId(`form-field-input-${normalAgentAssociatedWithEnvInXml}`) as HTMLInputElement;
    const agent2Checkbox      = helper.byTestId(`form-field-input-${unassociatedStaticAgent}`) as HTMLInputElement;

    expect(agent1Checkbox.checked).toBe(true);
    expect(modal.agentsVM.selectedAgentUuids.includes(normalAgentAssociatedWithEnvInXml)).toBe(true);

    expect(agent2Checkbox.checked).toBe(false);
    expect(modal.agentsVM.selectedAgentUuids.includes(unassociatedStaticAgent)).toBe(false);

    helper.clickByTestId(`form-field-input-${normalAgentAssociatedWithEnvInXml}`);

    expect(agent1Checkbox.checked).toBe(false);
    expect(modal.agentsVM.selectedAgentUuids.includes(normalAgentAssociatedWithEnvInXml)).toBe(false);

    helper.clickByTestId(`form-field-input-${unassociatedStaticAgent}`);

    expect(agent2Checkbox.checked).toBe(true);
    expect(modal.agentsVM.selectedAgentUuids.includes(unassociatedStaticAgent)).toBe(true);
  });

  it("should not allow toggling config repo agents", () => {
    const agent1Checkbox = helper.byTestId(`form-field-input-${normalAgentAssociatedWithEnvInConfigRepo}`) as HTMLInputElement;

    expect(agent1Checkbox.checked).toBe(true);
    expect(agent1Checkbox.disabled).toBe(true);
  });

  it("should not allow toggling elastic agents", () => {
    const agent1Checkbox = helper.byTestId(`form-field-input-${elasticAgentAssociatedWithEnvInXml}`) as HTMLInputElement;

    expect(agent1Checkbox.checked).toBe(true);
    expect(agent1Checkbox.disabled).toBe(true);
  });

  it("should render agent search box", () => {
    const searchInput = helper.byTestId("form-field-input-agent-search");
    expect(searchInput).toBeInDOM();
    expect(searchInput.getAttribute("placeholder")).toBe("agent hostname");
  });

  it("should bind search text with pipelines vm", () => {
    const searchText = "search-text";
    modal.agentsVM.searchText(searchText);
    helper.redraw();
    const searchInput = helper.byTestId("form-field-input-agent-search");
    expect(searchInput).toHaveValue(searchText);
  });

  it("should search for a particular agent", () => {
    const agent1Selector = `agent-checkbox-for-${normalAgentAssociatedWithEnvInXml}`;
    const agent2Selector = `agent-checkbox-for-${unassociatedStaticAgent}`;
    const agent3Selector = `agent-checkbox-for-${normalAgentAssociatedWithEnvInConfigRepo}`;
    const agent4Selector = `agent-checkbox-for-${elasticAgentAssociatedWithEnvInXml}`;
    const agent5Selector = `agent-list-item-for-${unassociatedElasticAgent}`;

    expect(helper.byTestId(agent1Selector)).toBeInDOM();
    expect(helper.byTestId(agent2Selector)).toBeInDOM();
    expect(helper.byTestId(agent3Selector)).toBeInDOM();
    expect(helper.byTestId(agent4Selector)).toBeInDOM();
    expect(helper.byTestId(agent5Selector)).toBeInDOM();

    modal.agentsVM.searchText(agentsJSON._embedded.agents[0].hostname);
    m.redraw.sync();

    expect(helper.byTestId(agent1Selector)).toBeInDOM();
    expect(helper.byTestId(agent2Selector)).toBeFalsy();
    expect(helper.byTestId(agent3Selector)).toBeFalsy();
    expect(helper.byTestId(agent4Selector)).toBeFalsy();
    expect(helper.byTestId(agent5Selector)).toBeFalsy();
  });

  it("should search for a partial agent name match", () => {
    const agent1Selector = `agent-checkbox-for-${normalAgentAssociatedWithEnvInXml}`;
    const agent2Selector = `agent-checkbox-for-${unassociatedStaticAgent}`;
    const agent3Selector = `agent-checkbox-for-${normalAgentAssociatedWithEnvInConfigRepo}`;
    const agent4Selector = `agent-checkbox-for-${elasticAgentAssociatedWithEnvInXml}`;
    const agent5Selector = `agent-list-item-for-${unassociatedElasticAgent}`;

    expect(helper.byTestId(agent1Selector)).toBeInDOM();
    expect(helper.byTestId(agent2Selector)).toBeInDOM();
    expect(helper.byTestId(agent3Selector)).toBeInDOM();
    expect(helper.byTestId(agent4Selector)).toBeInDOM();
    expect(helper.byTestId(agent5Selector)).toBeInDOM();

    modal.agentsVM.searchText("Hostname");
    m.redraw.sync();

    expect(helper.byTestId(agent1Selector)).toBeInDOM();
    expect(helper.byTestId(agent2Selector)).toBeInDOM();
    expect(helper.byTestId(agent3Selector)).toBeInDOM();
  });

  it("should show no agents available message", () => {
    modal.agentsVM.agents(new Agents());
    m.redraw.sync();
    const expectedMessage = "There are no agents available!";
    expect(helper.textByTestId("flash-message-info")).toContain(expectedMessage);
  });

  it("should show no agents matching search text message when no agents matched the search text", () => {
    const agent1Selector = `agent-checkbox-for-${normalAgentAssociatedWithEnvInXml}`;
    const agent2Selector = `agent-checkbox-for-${unassociatedStaticAgent}`;
    const agent3Selector = `agent-checkbox-for-${normalAgentAssociatedWithEnvInConfigRepo}`;
    const agent4Selector = `agent-checkbox-for-${elasticAgentAssociatedWithEnvInXml}`;
    const agent5Selector = `agent-list-item-for-${unassociatedElasticAgent}`;

    expect(helper.byTestId(agent1Selector)).toBeInDOM();
    expect(helper.byTestId(agent2Selector)).toBeInDOM();
    expect(helper.byTestId(agent3Selector)).toBeInDOM();
    expect(helper.byTestId(agent4Selector)).toBeInDOM();
    expect(helper.byTestId(agent5Selector)).toBeInDOM();

    modal.agentsVM.searchText("blah-is-my-agent-hostname");
    m.redraw.sync();

    expect(helper.byTestId(agent1Selector)).toBeFalsy();
    expect(helper.byTestId(agent2Selector)).toBeFalsy();
    expect(helper.byTestId(agent3Selector)).toBeFalsy();
    expect(helper.byTestId(agent4Selector)).toBeFalsy();
    expect(helper.byTestId(agent5Selector)).toBeFalsy();

    const expectedMessage = "No agents matching search text 'blah-is-my-agent-hostname' found!";
    expect(helper.textByTestId("flash-message-info")).toContain(expectedMessage);
  });

  it('should render buttons', () => {
    expect(helper.byTestId("cancel-button")).toBeInDOM();
    expect(helper.byTestId("cancel-button")).toHaveText("Cancel");
    expect(helper.byTestId("save-button")).toBeInDOM();
    expect(helper.byTestId("save-button")).toHaveText("Save");
  });

  it('should disable save and cancel button if modal state is loading', () => {
    modal.modalState = ModalState.LOADING;
    m.redraw.sync();
    expect(helper.byTestId("save-button")).toBeDisabled();
    expect(helper.byTestId("cancel-button")).toBeDisabled();
    expect(helper.byTestId("spinner")).toBeInDOM();
  });
});
