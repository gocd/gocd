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

enum AgentConfigState {
  Enabled, Disabled
}

enum AgentState {
  Idle, Building, LostContact, Missing, Cancelled, Unknown
}

enum BuildState {
  Idle, Building, Cancelled, Unknown
}

export interface AgentJSON {
  uuid: string;
  hostname: string;
  ip_address: string;
  sandbox: string;
  operating_system: string;
  free_space: number;
  agent_config_state: AgentConfigState;
  agent_state: AgentState;
  resources: string[];
  build_state: BuildState;
}

interface EmbeddedJSON {
  agents: AgentJSON[];
}

export interface AgentsJSON {
  _embedded: EmbeddedJSON;
}

export class Agent {
  public readonly uuid: string;
  public readonly hostname: string;
  public readonly ipAddress: string;
  public readonly freeSpace: number;
  public readonly sandbox: string;
  public readonly operatingSystem: string;
  public readonly agentConfigState: AgentConfigState;
  public readonly agentState: AgentState;
  public readonly resources: string[];
  public readonly buildState: BuildState;

  constructor(uuid: string,
              hostname: string,
              ipAddress: string,
              freeSpace: number,
              sandbox: string,
              operatingSystem: string,
              agentConfigState: AgentConfigState,
              agentState: AgentState,
              resources: string[],
              buildState: BuildState) {
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
                     data.agent_config_state,
                     data.agent_state,
                     data.resources,
                     data.build_state);
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
