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

import {Agents} from "models/agents/agents";
import {AgentsJSON} from "models/agents/agents_json";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import test_data from "models/new-environments/spec/test_data";
import {AgentsViewModel} from "views/pages/new-environments/models/agents_view_model";

describe("Agents View Model", () => {
  let environment: EnvironmentWithOrigin;
  let agentsViewModel: AgentsViewModel;
  let agentsJSON: AgentsJSON;

  let normalAgentAssociatedWithEnvInXml: string,
      normalAgentAssociatedWithEnvInConfigRepo: string,
      elasticAgentAssociatedWithEnvInXml: string,
      unassociatedStaticAgent: string,
      unassociatedElasticAgent: string;

  beforeEach(() => {
    const environmentJSON = test_data.environment_json();
    environmentJSON.agents.push(test_data.agent_association_in_xml_json());

    environment = EnvironmentWithOrigin.fromJSON(environmentJSON);
    agentsJSON  = AgentsTestData.list();

    agentsJSON._embedded.agents.push(AgentsTestData.elasticAgent());
    agentsJSON._embedded.agents.push(AgentsTestData.elasticAgent());

    //normal agent associated with environment in xml
    normalAgentAssociatedWithEnvInXml       = environmentJSON.agents[0].uuid;
    agentsJSON._embedded.agents[0].uuid     = normalAgentAssociatedWithEnvInXml;
    agentsJSON._embedded.agents[0].hostname = environmentJSON.agents[0].hostname;

    //normal agent associated with environment in config repo
    normalAgentAssociatedWithEnvInConfigRepo = environmentJSON.agents[1].uuid;
    agentsJSON._embedded.agents[1].uuid      = normalAgentAssociatedWithEnvInConfigRepo;
    agentsJSON._embedded.agents[1].hostname  = environmentJSON.agents[1].hostname;

    //elastic agent associated with environment in xml
    elasticAgentAssociatedWithEnvInXml      = environmentJSON.agents[2].uuid;
    agentsJSON._embedded.agents[4].uuid     = elasticAgentAssociatedWithEnvInXml;
    agentsJSON._embedded.agents[4].hostname = environmentJSON.agents[2].hostname;

    unassociatedStaticAgent  = agentsJSON._embedded.agents[2].uuid;
    unassociatedElasticAgent = agentsJSON._embedded.agents[3].uuid;

    agentsViewModel = new AgentsViewModel(environment, Agents.fromJSON(agentsJSON));
  });

  it("should return available agents", () => {
    const agents = agentsViewModel.availableAgents();

    expect(agents.length).toBe(2);
    expect(agents[0].uuid).toBe(normalAgentAssociatedWithEnvInXml);
    expect(agents[1].uuid).toBe(unassociatedStaticAgent);
  });

  it("should return agents belonging to the environment whose association is defined in config repo", () => {
    const agents = agentsViewModel.configRepoEnvironmentAgents();

    expect(agents.length).toBe(1);
    expect(agents[0].uuid).toBe(normalAgentAssociatedWithEnvInConfigRepo);
  });

  it("should return elastic agents belonging to the environment", () => {
    const agents = agentsViewModel.environmentElasticAgents();

    expect(agents.length).toBe(1);
    expect(agents[0].uuid).toBe(elasticAgentAssociatedWithEnvInXml);
  });

  it("should return elastic agents not belonging to the environment", () => {
    const agents = agentsViewModel.elasticAgentsNotBelongingToCurrentEnv();

    expect(agents.length).toBe(1);
    expect(agents[0].uuid).toBe(unassociatedElasticAgent);
  });

  it("should answer whether an agent belongs to the current environment", () => {
    const agents = agentsViewModel.agents();

    expect(agentsViewModel.agentSelectedFn(agents[0])()).toBe(true);
    expect(agentsViewModel.agentSelectedFn(agents[0])()).toBe(environment.containsAgent(agents[0].uuid));

    expect(agentsViewModel.agentSelectedFn(agents[1])()).toBe(true);
    expect(agentsViewModel.agentSelectedFn(agents[1])()).toBe(environment.containsAgent(agents[1].uuid));

    expect(agentsViewModel.agentSelectedFn(agents[2])()).toBe(false);
    expect(agentsViewModel.agentSelectedFn(agents[2])()).toBe(environment.containsAgent(agents[2].uuid));

    expect(agentsViewModel.agentSelectedFn(agents[3])()).toBe(false);
    expect(agentsViewModel.agentSelectedFn(agents[3])()).toBe(environment.containsAgent(agents[3].uuid));

    expect(agentsViewModel.agentSelectedFn(agents[4])()).toBe(true);
    expect(agentsViewModel.agentSelectedFn(agents[4])()).toBe(environment.containsAgent(agents[4].uuid));
  });

  it("should add an agent to an environment using agentSelectedFn", () => {
    const agents = agentsViewModel.agents();

    expect(agentsViewModel.agentSelectedFn(agents[2])()).toBe(false);
    expect(environment.containsAgent(agents[2].uuid)).toBe(false);

    agentsViewModel.agentSelectedFn(agents[2])(agents[2]);

    expect(agentsViewModel.agentSelectedFn(agents[2])()).toBe(true);
  });

  it("should return filtered agents by hostname", () => {
    expect(agentsViewModel.searchText()).toBe(undefined);
    expect(agentsViewModel.filteredAgents().length).toBe(5);

    agentsViewModel.searchText(agentsViewModel.agents()[0].hostname);

    expect(agentsViewModel.searchText()).toBe(agentsViewModel.agents()[0].hostname);
    expect(agentsViewModel.filteredAgents().length).toBe(1);
  });

  it("should return available agents matching search text", () => {
    let agents = agentsViewModel.availableAgents();

    expect(agents.length).toBe(2);
    expect(agents[0].uuid).toBe(normalAgentAssociatedWithEnvInXml);
    expect(agents[1].uuid).toBe(unassociatedStaticAgent);

    agentsViewModel.searchText(agents[0].hostname);

    agents = agentsViewModel.availableAgents();

    expect(agents.length).toBe(1);
    expect(agents[0].uuid).toBe(normalAgentAssociatedWithEnvInXml);
  });
});
