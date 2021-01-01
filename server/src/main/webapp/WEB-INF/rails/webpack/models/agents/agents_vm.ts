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

import _ from "lodash";
import Stream from "mithril/stream";
import {Agent, AgentConfigState, Agents} from "models/agents/agents";
import {AgentComparator} from "models/agents/agent_comparator";
import {SortOrder, TableSortHandler} from "views/components/table";

export abstract class BaseVM {
  protected agents: Agents;
  readonly filterText: Stream<string>               = Stream();
  readonly showBuildDetailsForAgent: Stream<string> = Stream();
  readonly agentsSortHandler: AgentSortHandler;

  protected constructor(agents: Agents) {
    this.agents            = agents;
    this.agentsSortHandler = this.getAgentSortHandler();
  }

  sync(agents: Agents) {
    this.agents = _.cloneDeep(agents);
    this.agentsSortHandler.sort();
  }

  list(): Agent[] {
    if (!this.filterText()) {
      return this.agents;
    }
    return this.agents.filter((agent) => this.searchPredicate(agent));
  }

  filterBy(agentConfigState: AgentConfigState): Agent[] {
    return this.list().filter((agent) => agent.agentConfigState === agentConfigState);
  }

  all() {
    return this.agents;
  }

  protected abstract getAgentSortHandler(): AgentSortHandler;

  private static getOrEmpty(str: string | number) {
    return str ? str.toString().toLowerCase() : "";
  }

  private searchPredicate(agent: Agent) {
    const lowercaseFilterText = this.filterText().toLowerCase();
    return _.includes(BaseVM.getOrEmpty(agent.hostname), lowercaseFilterText) ||
      _.includes(BaseVM.getOrEmpty(agent.operatingSystem), lowercaseFilterText) ||
      _.includes(BaseVM.getOrEmpty(agent.sandbox), lowercaseFilterText) ||
      _.includes(BaseVM.getOrEmpty(agent.ipAddress), lowercaseFilterText) ||
      _.includes(BaseVM.getOrEmpty(agent.freeSpace.toString()), lowercaseFilterText) ||
      _.includes(BaseVM.getOrEmpty(agent.status()), lowercaseFilterText) ||
      _.includes(BaseVM.getOrEmpty(agent.resources.join(", ")), lowercaseFilterText) ||
      _.includes(BaseVM.getOrEmpty(agent.environmentNames().join(", ")), lowercaseFilterText);
  }
}

export class StaticAgentsVM extends BaseVM {
  readonly showResources: Stream<boolean>                = Stream();
  readonly showEnvironments: Stream<boolean>             = Stream();
  private readonly _selectedAgentsUUID: Stream<string[]> = Stream([] as string[]);

  constructor(agents: Agents = new Agents()) {
    super(agents);
  }

  sync(agents: Agents) {
    super.sync(agents);
    StaticAgentsVM.syncAgentSelection(this._selectedAgentsUUID, this.selectedAgentsUUID(), this.agents);
  }

  all() {
    return this.agents;
  }

  totalCount() {
    return this.agents.length;
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

  list() {
    return super.list().filter((agent) => !agent.isElastic());
  }

  protected getAgentSortHandler() {
    return new AgentSortHandler(this, new Map(
      [
        [1, "hostname"], [2, "sandbox"], [3, "operatingSystem"], [4, "ipAddress"],
        [5, "agentState"], [6, "freeSpace"], [7, "resources"], [8, "environments"],
      ])
    );
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
}

export class ElasticAgentVM extends BaseVM {
  constructor(agents: Agents = new Agents()) {
    super(agents);
  }

  sync(agents: Agents) {
    super.sync(agents);
  }

  list() {
    return super.list().filter((agent) => agent.isElastic());
  }

  protected getAgentSortHandler(): AgentSortHandler {
    return new AgentSortHandler(this, new Map(
      [
        [1, "hostname"], [2, "sandbox"], [3, "operatingSystem"], [4, "ipAddress"],
        [5, "agentState"], [6, "freeSpace"], [7, "environments"],
      ])
    );
  }
}

export class AgentSortHandler implements TableSortHandler {
  private readonly sortableColumns: Map<number, string>;
  private readonly agentsVM: ElasticAgentVM | StaticAgentsVM;
  private sortOnColumn: number = 5;
  private sortOrder: SortOrder = SortOrder.ASC;

  constructor(agentsVM: ElasticAgentVM | StaticAgentsVM, sortableColumns: Map<number, string>) {
    this.sortableColumns = sortableColumns;
    this.agentsVM        = agentsVM;
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

    this.sort();
  }

  sort() {
    const agentComparator = new AgentComparator(this.sortableColumns.get(this.sortOnColumn) as string);
    this.agentsVM.all().sort(agentComparator.compare.bind(agentComparator));
    if (this.sortOrder === SortOrder.DESC) {
      this.agentsVM.all().reverse();
    }
  }

  currentSortedColumnIndex(): number {
    return this.sortOnColumn;
  }

  getCurrentSortOrder(): SortOrder {
    return this.sortOrder;
  }
}
