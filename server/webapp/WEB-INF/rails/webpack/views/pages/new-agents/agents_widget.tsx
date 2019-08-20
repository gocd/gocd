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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Agent, AgentConfigState, Agents} from "models/new-agent/agents";
import {ButtonGroup} from "views/components/buttons";
import * as Buttons from "views/components/buttons";
import {CheckboxField, SearchField} from "views/components/forms/input_fields";
import {KeyValuePair} from "views/components/key_value_pair";
import {Table} from "views/components/table";
import {AgentStatusWidget} from "views/pages/new-agents/agent_status_widget";
import style from "./index.scss";

const classnames = bind(style);

interface AgentsWidgetAttrs {
  agents: Agents;
  onEnable: (e: MouseEvent) => void;
  onDisable: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
}

export class AgentsWidget extends MithrilViewComponent<AgentsWidgetAttrs> {
  view(vnode: m.Vnode<AgentsWidgetAttrs>) {
    const tableData = vnode.attrs.agents.list().map((agent: Agent) => {
      return [
        <div key={agent.uuid}
             class={classnames(style.tableCell, style.agentCheckbox, {[style.building]: agent.isBuilding()})}>
          <CheckboxField dataTestId={`agent-checkbox-of-${agent.uuid}`}
                         required={true}
                         property={agent.selected}/>
        </div>,
        <div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
             data-test-id={`agent-hostname-of-${agent.uuid}`}>{agent.hostname}</div>,
        <div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
             data-test-id={`agent-sandbox-of-${agent.uuid}`}>{agent.sandbox}</div>,
        <div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
             data-test-id={`agent-operating-system-of-${agent.uuid}`}>{agent.operatingSystem}</div>,
        <div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
             data-test-id={`agent-ip-address-of-${agent.uuid}`}>{agent.ipAddress}</div>,
        <AgentStatusWidget agent={agent} buildDetailsForAgent={vnode.attrs.agents.buildDetailsForAgent}/>,
        <div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
             data-test-id={`agent-free-space-of-${agent.uuid}`}>{agent.readableFreeSpace()}</div>,
        <div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
             data-test-id={`agent-resources-of-${agent.uuid}`}>{AgentsWidget.joinOrNoneSpecified(agent.resources)}</div>,
        <div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
             data-test-id={`agent-environments-of-${agent.uuid}`}>{AgentsWidget.joinOrNoneSpecified(agent.environmentNames())}</div>,
      ];
    });

    return <div class={style.agentsTable} onclick={AgentsWidget.hideBuildDetails.bind(this, vnode.attrs.agents)}>
      <KeyValuePair inline={true} data={new Map(
        [
          ["Total", vnode.attrs.agents.count()],
          ["Pending", this.filter(vnode.attrs.agents, AgentConfigState.Pending)],
          ["Enabled", this.filter(vnode.attrs.agents, AgentConfigState.Enabled)],
          ["Disabled", this.filter(vnode.attrs.agents, AgentConfigState.Disabled)]
        ])
      }/>

      <ButtonGroup>
        <Buttons.Primary onclick={vnode.attrs.onDelete}>DELETE</Buttons.Primary>
        <Buttons.Primary onclick={vnode.attrs.onEnable}>ENABLE</Buttons.Primary>
        <Buttons.Primary onclick={vnode.attrs.onDisable}>DISABLE</Buttons.Primary>
      </ButtonGroup>

      <div class={style.searchField}>
        <SearchField placeholder="Filter Agents"
                     label="Search for agents"
                     property={vnode.attrs.agents.filterText}/>
      </div>

      <Table data={tableData}
             headers={[
               <input type="checkbox"
                      data-test-id={"select-all-agents"}
                      checked={vnode.attrs.agents.areAllFilteredAgentsSelected()}
                      onclick={() => vnode.attrs.agents.toggleFilteredAgentsSelection()}/>,
               "Agent Name", "Sandbox", "OS", "IP Address", "Status", "Free Space", "Resources", "Environments"]}
             sortHandler={vnode.attrs.agents}/>

    </div>;
  }

  private filter(agents: Agents, matcher: number) {
    return agents.list().filter(agent => agent.agentConfigState === matcher).length;
  }

  static joinOrNoneSpecified(array: string[]): m.Children {
    if (array && array.length > 0) {
      return array.join(", ");
    } else {
      return (<em>none specified</em>);
    }
  }

  private static hideBuildDetails(agents: Agents) {
    agents.buildDetailsForAgent("");
  }
}
