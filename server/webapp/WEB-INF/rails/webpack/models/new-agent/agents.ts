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
import * as stream from "mithril/stream";
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

export interface AgentEnvironmentJSON {
  name: string;
  origin: Origin;
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

  constructor(uuid: string,
              hostname: string,
              ipAddress: string,
              freeSpace: string | number,
              sandbox: string,
              operatingSystem: string,
              agentConfigState: AgentConfigState,
              agentState: AgentState,
              resources: string[],
              buildState: BuildState,
              environments: AgentsEnvironment[]) {
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
  }

  hasFilterText(filterText: string): boolean {
    const lowerCaseFilterText = filterText.toLowerCase();

    return _.includes(this.hostname.toLowerCase(), lowerCaseFilterText) ||
      _.includes(this.sandbox.toLowerCase(), lowerCaseFilterText) ||
      _.includes(this.operatingSystem.toLowerCase(), lowerCaseFilterText) ||
      _.includes(this.ipAddress.toLowerCase(), lowerCaseFilterText) ||
      _.includes(this.freeSpace.toString().toLowerCase(), lowerCaseFilterText) ||
      _.includes(this.resources.join(", ").toLowerCase(), lowerCaseFilterText);
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
                     data.resources,
                     (<any>BuildState)[data.build_state],
                     data.environments.map((envJson) => new AgentsEnvironment(envJson.name, envJson.origin.type)));
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
}

enum SortOrder {
  ASC, DESC
}

export class Agents extends TableSortHandler {
  private sortOnColumn: number;
  private sortOrder: SortOrder;
  private agentList: Agent[];
  public readonly filterText: Stream<string> = stream("");

  constructor(agentsList: Agent[]) {
    super();
    this.agentList    = agentsList;
    this.sortOrder    = SortOrder.ASC;
    this.sortOnColumn = this.getSortableColumns()[0];
    this.sort();
  }

  initializeWith(agents: Agents) {
    //todo: This method is also responsible to preserve the agent checkbox selections once implemented.
    this.agentList = agents.agentList;
    this.sort();
  }

  static fromJSON(data: AgentsJSON): Agents {
    const agents: Agent[] = data._embedded.agents.map((json: AgentJSON) => {
      return Agent.fromJSON(json);
    });

    return new Agents(agents);
  }

  getSortableColumns(): number[] {
    return [0, 1, 2, 3, 4, 5, 6, 7];
  }

  onColumnClick(columnIndex: number): void {
    if (this.sortOnColumn === columnIndex) {
      this.sortOrder = this.sortOrder === SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
    } else {
      this.sortOrder    = SortOrder.ASC;
      this.sortOnColumn = columnIndex;
    }

    this.sort();
  }

  count(): number {
    return this.agentList.length;
  }

  list(): Agent[] {
    return this.agentList.filter((agent: Agent) => agent.hasFilterText(this.filterText()));
  }

  private getSortableColumnsAssociates(): string[] {
    return ["hostname", "sandbox", "operatingSystem", "ipAddress", "status", "freeSpace", "resources", "environments"];
  }

  private sort() {
    const columnName = this.getSortableColumnsAssociates()[this.sortOnColumn];

    this.agentList.sort((agent1: Agent, agent2: Agent) => {
      //@ts-ignore
      const first = agent1[columnName];
      //@ts-ignore
      const other = agent2[columnName];

      return this.sortOrder === SortOrder.ASC ? first.localeCompare(other) : other.localeCompare(first);
    });
  }
}
