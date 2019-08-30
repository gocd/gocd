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

import _ from "lodash";
import Stream from "mithril/stream";
import {AgentComparator, SortOrder} from "models/new_agent/agent_comparator";
import {Agent, AgentConfigState, Agents} from "models/new_agent/agents";
import {TableSortHandler} from "views/components/table";

export class AgentsVM {
  readonly filterText: Stream<string>                    = Stream();
  readonly buildDetailsForAgent: Stream<string>          = Stream();
  readonly showResources: Stream<boolean>                = Stream();
  readonly showEnvironments: Stream<boolean>             = Stream();
  private agents: Agents;
  private readonly _staticAgentSortHandler: AgentSortHandler;
  private readonly _elasticAgentSortHandler: AgentSortHandler;
  private readonly _selectedAgentsUUID: Stream<string[]> = Stream([] as string[]);

  constructor(agents: Agents = new Agents()) {
    this.agents                   = agents;
    this._staticAgentSortHandler  = new AgentSortHandler(agents, new Map(
      [
        [1, "hostname"], [2, "sandbox"], [3, "operatingSystem"], [4, "ipAddress"],
        [5, "agentState"], [6, "freeSpace"], [7, "resources"], [8, "environments"],
      ])
    );
    this._elasticAgentSortHandler = new AgentSortHandler(agents, new Map(
      [
        [0, "hostname"], [1, "sandbox"], [2, "operatingSystem"], [3, "ipAddress"],
        [4, "agentState"], [5, "freeSpace"], [6, "environments"],
      ])
    );
  }

  totalCount() {
    return this.agents.length;
  }

  sync(agents: Agents) {
    this.agents = agents;
    AgentsVM.syncAgentSelection(this._selectedAgentsUUID, this.selectedAgentsUUID(), this.agents);
  }

  list() {
    if (!this.filterText()) {
      return this.agents;
    }

    return this.agents.filter((agent) => this.searchPredicate(agent));
  }

  staticAgentSortHandler() {
    return this._staticAgentSortHandler;
  }

  elasticAgentSortHandler() {
    return this._elasticAgentSortHandler;
  }

  selectedAgentsUUID() {
    return this._selectedAgentsUUID();
  }

  selectAgent(uuid: string) {
    if (!this.isAgentSelected(uuid)) {
      this._selectedAgentsUUID().push(uuid);
    }
  }

  toggleAgentSelection(uuid: string) {
    if (this.isAgentSelected(uuid)) {
      this.selectedAgentsUUID().splice(this.selectedAgentsUUID().indexOf(uuid), 1);
    } else {
      this.selectedAgentsUUID().push(uuid);
    }
  }

  isAgentSelected(uuid: string) {
    return this.selectedAgentsUUID().indexOf(uuid) !== -1;
  }

  isAllStaticAgentSelected(): boolean {
    return this.selectedAgentsUUID().length === this.list().length;
  }

  toggleAgentsSelection() {
    this.isAllStaticAgentSelected() ? this.unselectAll() : this.list().forEach((agent) => this.selectAgent(agent.uuid));
  }

  filterBy(agentConfigState: AgentConfigState): Agent[] {
    return this.list().filter((agent) => agent.agentConfigState === agentConfigState);
  }

  unselectAll() {
    this._selectedAgentsUUID([]);
  }

  private static getOrEmpty(str: string | number) {
    return str ? str.toString().toLowerCase() : "";
  }

  private static syncAgentSelection(resultStream: Stream<string[]>,
                                    currentSelection: string[],
                                    agentsFromServer: Agents) {
    const reducerFn = (accumulator: string[], agent: Agent): string[] => {
      const indexOfUUID = currentSelection.indexOf(agent.uuid);
      if (indexOfUUID !== -1) {
        accumulator.push(agent.uuid);
        currentSelection.splice(indexOfUUID, 1);
      }
      return accumulator;
    };

    resultStream(agentsFromServer.reduce(reducerFn, []));
  }

  private searchPredicate(agent: Agent) {
    const lowercaseFilterText = this.filterText().toLowerCase();
    return _.includes(AgentsVM.getOrEmpty(agent.hostname), lowercaseFilterText) ||
      _.includes(AgentsVM.getOrEmpty(agent.operatingSystem), lowercaseFilterText) ||
      _.includes(AgentsVM.getOrEmpty(agent.sandbox), lowercaseFilterText) ||
      _.includes(AgentsVM.getOrEmpty(agent.ipAddress), lowercaseFilterText) ||
      _.includes(AgentsVM.getOrEmpty(agent.freeSpace.toString()), lowercaseFilterText) ||
      _.includes(AgentsVM.getOrEmpty(agent.status()), lowercaseFilterText) ||
      _.includes(AgentsVM.getOrEmpty(agent.resources.join(", ")), lowercaseFilterText) ||
      _.includes(AgentsVM.getOrEmpty(agent.environmentNames().join(", ")), lowercaseFilterText);
  }
}

export class AgentSortHandler implements TableSortHandler {
  private readonly sortableColumns: Map<number, string>;
  private readonly agents: Agents;
  private sortOnColumn: number        = -1;
  private sortOrder: SortOrder | null = null;

  constructor(agents: Agents, sortableColumns: Map<number, string>) {
    this.sortableColumns = sortableColumns;
    this.agents          = agents;
  }

  getSortableColumns(): number[] {
    return Array.from(this.sortableColumns.keys()).sort();
  }

  onColumnClick(columnIndex: number): void {
    if (this.sortOnColumn === columnIndex) {
      this.sortOrder = this.sortOrder === SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
    } else {
      this.sortOrder    = SortOrder.ASC;
      this.sortOnColumn = columnIndex;
    }

    this.agents.sort(new AgentComparator(this.sortableColumns.get(columnIndex) as string, this.sortOrder).compare);
  }

  currentSortedColumnIndex(): number {
    return this.sortOnColumn;
  }

  currentSortOrder() {
    return this.sortOrder;
  }
}
