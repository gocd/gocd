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
import {FlashMessage, FlashMessageModelWithTimeout} from "views/components/flash_message";
import {CheckboxField} from "views/components/forms/input_fields";
import {Table} from "views/components/table";
import {AgentHeaderPanel} from "views/pages/new-agents/agent_header_panel";
import {AgentStatusWidget} from "views/pages/new-agents/agent_status_widget";
import style from "./index.scss";

const classnames = bind(style);

interface AgentsWidgetAttrs {
  agents: Agents;
  onEnable: (e: MouseEvent) => void;
  onDisable: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  flashMessage: FlashMessageModelWithTimeout;
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<any>;
  updateResources: (resourcesToAdd: string[], resourcesToRemove: string[]) => Promise<any>;
}

export class AgentsWidget extends MithrilViewComponent<AgentsWidgetAttrs> {
  view(vnode: m.Vnode<AgentsWidgetAttrs>) {
    let flashMessage;

    if (vnode.attrs.flashMessage) {
      flashMessage = <FlashMessage message={vnode.attrs.flashMessage.message} type={vnode.attrs.flashMessage.type}/>;
    }

    const tableData = vnode.attrs.agents.list().map((agent: Agent) => {
      const tableCellClasses = this.tableCellClasses(agent);
      return [
        <div key={agent.uuid}
             class={classnames(tableCellClasses, style.agentCheckbox)}>
          <CheckboxField dataTestId={`agent-checkbox-of-${agent.uuid}`}
                         required={true}
                         property={agent.selected}/>
        </div>,
        <div class={tableCellClasses}
             data-test-id={`agent-hostname-of-${agent.uuid}`}>{agent.hostname}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-sandbox-of-${agent.uuid}`}>{agent.sandbox}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-operating-system-of-${agent.uuid}`}>{agent.operatingSystem}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-ip-address-of-${agent.uuid}`}>{agent.ipAddress}</div>,
        <AgentStatusWidget agent={agent} buildDetailsForAgent={vnode.attrs.agents.buildDetailsForAgent}
                           cssClasses={tableCellClasses}/>,
        <div class={tableCellClasses}
             data-test-id={`agent-free-space-of-${agent.uuid}`}>{agent.readableFreeSpace()}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-resources-of-${agent.uuid}`}>{AgentsWidget.joinOrNoneSpecified(agent.resources)}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-environments-of-${agent.uuid}`}>{AgentsWidget.joinOrNoneSpecified(agent.environmentNames())}</div>,
      ];
    });

    return <div class={style.agentsTable} onclick={AgentsWidget.hideBuildDetails.bind(this, vnode.attrs.agents)}>
      <AgentHeaderPanel {...vnode.attrs}/>
      {flashMessage}
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

  private tableCellClasses(agent: Agent) {
    return classnames(style.tableCell,
                      {[style.building]: agent.isBuilding()},
                      {[style.disabledAgent]: agent.agentConfigState === AgentConfigState.Disabled});
  }
}
