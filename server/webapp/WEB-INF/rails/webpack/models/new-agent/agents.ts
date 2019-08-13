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

import _ = require("lodash");
import {Stream} from "mithril/stream";
import stream from "mithril/stream";
import {AgentsCRUD} from "models/new-agent/agents_crud";
import {TableSortHandler} from "views/components/table";

const filesize = require("filesize");

export enum AgentConfigState {
  Enabled, Disabled, Pending
}

export enum AgentState {
  Idle, Building, LostContact, Missing, Cancelled, Unknown
}

export enum BuildState {
  Idle, Building, Cancelled, Unknown
}

interface Origin {
  type: string;
}

interface AgentEnvironmentJSON {
  name: string;
  origin: Origin;
}

interface UrlJSON {
  href: string;
}

interface LinkJSON {
  job: UrlJSON;
  stage: UrlJSON;
  pipeline: UrlJSON;
}

export interface BuildDetailsJSON {
  pipeline_name: string;
  stage_name: string;
  job_name: string;
  _links: LinkJSON;
}

export interface AgentJSON {
  uuid: string;
  hostname: string;
  ip_address: string;
  sandbox: string;
  operating_system: string;
  free_space: string | number;
  agent_config_state: string;
  agent_state: string;
  resources: string[];
  build_state: string;
  build_details: BuildDetailsJSON
  environments: AgentEnvironmentJSON[]
}

interface EmbeddedJSON {
  agents: AgentJSON[];
}

export interface AgentsJSON {
  _embedded: EmbeddedJSON;
}

export class AgentsEnvironment {
  public readonly name: string;
  public readonly originType: string;

  constructor(name: string, originType: string) {
    this.name       = name;
    this.originType = originType;
  }
}

export class BuildDetails {
  public readonly pipelineName: string;
  public readonly stageName: string;
  public readonly jobName: string;
  public readonly pipelineUrl: string;
  public readonly stageUrl: string;
  public readonly jobUrl: string;

  constructor(pipelineName: string,
              stageName: string,
              jobName: string,
              pipelineUrl: string,
              stageUrl: string,
              jobUrl: string) {
    this.pipelineName = pipelineName;
    this.stageName    = stageName;
    this.jobName      = jobName;
    this.pipelineUrl  = pipelineUrl;
    this.stageUrl     = stageUrl;
    this.jobUrl       = jobUrl;
  }

  static fromJSON(json: BuildDetailsJSON) {
    return json ? new BuildDetails(json.pipeline_name,
                                   json.stage_name,
                                   json.job_name,
                                   json._links.pipeline.href,
                                   json._links.stage.href,
                                   json._links.job.href
    ) : undefined;
  }
}

export class Agent {
  public readonly uuid: string;
  public readonly hostname: string;
  public readonly ipAddress: string;
  public readonly freeSpace: string | number;
  public readonly sandbox: string;
  public readonly operatingSystem: string;
  public readonly resources: string[];
  public readonly environments: AgentsEnvironment[];
  public readonly agentConfigState: AgentConfigState;
  public readonly agentState: AgentState;
  public readonly buildState: BuildState;
  public readonly buildDetails?: BuildDetails;
  public readonly selected: Stream<boolean> = stream();

  constructor(uuid: string,
              hostname: string,
              ipAddress: string,
              freeSpace: string | number,
              sandbox: string,
              operatingSystem: string,
              agentConfigState: AgentConfigState,
              agentState: AgentState,
              buildState: BuildState,
              resources: string[],
              environments: AgentsEnvironment[],
              buildDetails?: BuildDetails) {
    this.uuid             = uuid;
    this.hostname         = hostname;
    this.ipAddress        = ipAddress;
    this.freeSpace        = freeSpace;
    this.sandbox          = sandbox;
    this.operatingSystem  = operatingSystem;
    this.agentConfigState = agentConfigState;
    this.agentState       = agentState;
    this.resources        = resources;
    this.buildState       = buildState;
    this.environments     = environments;
    this.buildDetails     = buildDetails;
  }

  hasFilterText(filterText: string): boolean {
    if (!filterText || filterText.trim().length === 0) {
      return true;
    }

    const lowerCaseFilterText = filterText.toLowerCase();
    return _.includes(this.getOrEmpty(this.hostname), lowerCaseFilterText) ||
      _.includes(this.getOrEmpty(this.sandbox), lowerCaseFilterText) ||
      _.includes(this.getOrEmpty(this.operatingSystem), lowerCaseFilterText) ||
      _.includes(this.getOrEmpty(this.ipAddress), lowerCaseFilterText) ||
      _.includes(this.getOrEmpty(this.freeSpace), lowerCaseFilterText) ||
      _.includes(this.getOrEmpty(this.status()), lowerCaseFilterText) ||
      _.includes(this.getOrEmpty(this.resources.join(", ")), lowerCaseFilterText) ||
      _.includes(this.getOrEmpty(this.environmentNames().join(", ")), lowerCaseFilterText);
  }

  static fromJSON(data: AgentJSON) {
    return new Agent(data.uuid,
                     data.hostname,
                     data.ip_address,
                     data.free_space,
                     data.sandbox,
                     data.operating_system,
                     (<any>AgentConfigState)[data.agent_config_state],
                     (<any>AgentState)[data.agent_state],
                     (<any>BuildState)[data.build_state],
                     data.resources,
                     data.environments.map((envJson) => new AgentsEnvironment(envJson.name, envJson.origin.type)),
                     BuildDetails.fromJSON(data.build_details));
  }

  readableFreeSpace() {
    try {
      if (_.isNumber(this.freeSpace)) {
        return filesize(this.freeSpace);
      } else {
        return "Unknown";
      }
    } catch (e) {
      return "Unknown";
    }
  };

  environmentNames() {
    if (!this.environments || this.environments.length === 0) {
      return [];
    }
    return this.environments.map((env) => env.name);
  };

  isBuilding() {
    return !!this.buildDetails;
  }

  status() {
    if (this.agentConfigState === AgentConfigState.Pending) {
      return "Pending";
    }

    if (this.agentConfigState === AgentConfigState.Disabled) {
      if (this.buildState === BuildState.Building) {
        return "Disabled (Building)";
      } else if (this.buildState === BuildState.Cancelled) {
        return "Disabled (Cancelled)";
      }
      return "Disabled";
    }

    if (this.agentState === AgentState.Building) {
      return this.buildState === BuildState.Cancelled ? "Building (Cancelled)" : "Building";
    }
    return AgentState[this.agentState];
  }

  private getOrEmpty(str: string | number) {
    return str ? str.toString().toLowerCase() : "";
  }
}

enum SortOrder {
  ASC, DESC
}

export class Agents extends TableSortHandler {
  private sortOnColumn: number;
  private sortOrder: SortOrder;
  private agentList: Agent[];
  public readonly filterText: Stream<string>           = stream("");
  public readonly buildDetailsForAgent: Stream<string> = stream();

  constructor(agentsList: Agent[]) {
    super();
    this.agentList    = agentsList;
    this.sortOrder    = SortOrder.ASC;
    this.sortOnColumn = this.getSortableColumns()[0] - 1;
    this.sort();
  }

  initializeWith(newAgents: Agents) {
    newAgents.agentList.forEach((agent) => {
      const existingAgent = this.find(agent.uuid);
      if (existingAgent) {
        agent.selected(existingAgent.selected());
      }
    });

    this.agentList = newAgents.agentList;
    this.sort();
  }

  static fromJSON(data: AgentsJSON): Agents {
    const agents: Agent[] = data._embedded.agents.map((json: AgentJSON) => {
      return Agent.fromJSON(json);
    });

    return new Agents(agents);
  }

  areAllFilteredAgentsSelected(): boolean {
    return this.list().every((agent: Agent) => agent.selected());
  }

  toggleFilteredAgentsSelection(): void {
    if (this.areAllFilteredAgentsSelected()) {
      this.list().forEach((agent: Agent) => agent.selected(false));
    } else {
      this.list().forEach((agent: Agent) => agent.selected(true));
    }
  }

  //TODO: should this be on all agents or on filtered list
  deleteSelectedAgents(): Promise<any> {
    let agentsUUID: string[] = (this.agentList.filter(agent => agent.selected())).map(agent => agent.uuid);
    return AgentsCRUD.delete(agentsUUID);
  }

  enableSelectedAgents(): Promise<any> {
    let agentsUUID: string[] = (this.agentList.filter(agent => agent.selected())).map(agent => agent.uuid);
    return AgentsCRUD.agentsToEnable(agentsUUID);
  }

  disableSelectedAgents(): Promise<any> {
    let agentsUUID: string[] = (this.agentList.filter(agent => agent.selected())).map(agent => agent.uuid);
    return AgentsCRUD.agentsToDisable(agentsUUID);
  }

  getSortableColumns(): number[] {
    return [1, 2, 3, 4, 5, 6, 7, 8];
  }

  onColumnClick(columnIndex: number): void {
    if (this.sortOnColumn === columnIndex - 1) {
      this.sortOrder = this.sortOrder === SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
    } else {
      this.sortOrder    = SortOrder.ASC;
      this.sortOnColumn = columnIndex - 1;
    }

    this.sort();
  }

  count(): number {
    return this.agentList.length;
  }

  list(): Agent[] {
    return this.agentList.filter((agent: Agent) => agent.hasFilterText(this.filterText()));
  }

  isKnownAgent(uuid: string): boolean {
    return this.agentList.findIndex((agent) => agent.uuid === uuid) > -1;
  }

  find(uuid: string): Agent | undefined {
    return this.agentList.find((agent) => agent.uuid === uuid);
  }

  private getSortableColumnsAssociates(): string[] {
    return ["hostname", "sandbox", "operatingSystem", "ipAddress", "agentState", "freeSpace", "resources", "environments"];
  }

  private sort() {
    const columnName = this.getSortableColumnsAssociates()[this.sortOnColumn];

    if (columnName === "freeSpace") {
      const agentsWithFreeSpace = this.agentList.filter(agent => agent[columnName] != "unknown")
                                      .sort((agent1: Agent, agent2: Agent) => {
                                        //@ts-ignore
                                        const first = agent1[columnName];
                                        //@ts-ignore
                                        const other = agent2[columnName];
                                        //@ts-ignore
                                        return this.sortOrder === SortOrder.ASC ? first - other : other - first;
                                      });

      const agentsWithUnknownFreeSpace = this.agentList.filter(agent => agent[columnName] === "unknown");

      this.agentList = this.sortOrder === SortOrder.ASC ? agentsWithFreeSpace.concat(agentsWithUnknownFreeSpace) : agentsWithUnknownFreeSpace.concat(
        agentsWithFreeSpace);

    } else if (columnName === "resources") {
      const agentsWithResource    = this.agentList.filter(agent => agent[columnName].join(", ") != "")
                                        .sort((agent1: Agent, agent2: Agent) => {
                                          //@ts-ignore
                                          const first = agent1[columnName].join(", ");
                                          //@ts-ignore
                                          const other = agent2[columnName].join(", ");

                                          return this.sortOrder === SortOrder.ASC ? first.localeCompare(other) : other.localeCompare(
                                            first);
                                        });
      const agentsWithNoResources = this.agentList.filter(agent => agent[columnName].join(", ") === "");

      this.agentList = this.sortOrder === SortOrder.ASC ? agentsWithResource.concat(agentsWithNoResources) : agentsWithNoResources.concat(
        agentsWithResource);

    } else if (columnName === "environments") {
      const agentsWithEnvironments = this.agentList.filter(agent => agent[columnName].join(", ") != "")
                                         .sort((agent1: Agent, agent2: Agent) => {
                                           //@ts-ignore
                                           const first = agent1[columnName].map(env => env.name).join(", ");
                                           //@ts-ignore
                                           const other = agent2[columnName].map(env => env.name).join(", ");

                                           return this.sortOrder === SortOrder.ASC ? first.localeCompare(other) : other.localeCompare(
                                             first);
                                         });

      const agentsWithNoEnvironment = this.agentList.filter(agent => agent[columnName].join(", ") === "");

      this.agentList = this.sortOrder === SortOrder.ASC ? agentsWithEnvironments.concat(agentsWithNoEnvironment) : agentsWithNoEnvironment.concat(
        agentsWithEnvironments);

    } else if (columnName === "agentState") {
      const enabledAgents  = this.agentList.filter(agent => agent["agentConfigState"] === AgentConfigState.Enabled)
                                 .sort((agent1: Agent, agent2: Agent) => {
                                   //@ts-ignore
                                   const first = AgentState[agent1[columnName]];
                                   //@ts-ignore
                                   const other = AgentState[agent2[columnName]];

                                   return this.sortOrder === SortOrder.ASC ? first.localeCompare(other) : other.localeCompare(
                                     first);
                                 });
      const disabledAgents = this.agentList.filter(agent => agent["agentConfigState"] === AgentConfigState.Disabled);

      this.agentList = this.sortOrder === SortOrder.ASC ? enabledAgents.concat(disabledAgents) : disabledAgents.concat(
        enabledAgents);

    } else {
      this.agentList.sort((agent1: Agent, agent2: Agent) => {
        //@ts-ignore
        const first = agent1[columnName];
        //@ts-ignore
        const other = agent2[columnName];

        return this.sortOrder === SortOrder.ASC ? first.localeCompare(other) : other.localeCompare(first);
      });
    }
  }
}
