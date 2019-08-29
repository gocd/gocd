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
import {
  Agent,
  AgentConfigState,
  Agents,
} from "models/new_agent/agents";
import {Table} from "views/components/table";
import {AgentStatusWidget} from "views/pages/new_agents/agent_status_widget";
import {RequiresPluginInfos} from "views/pages/page_operations";
import style from "./index.scss";

const classnames = bind(style);

interface AgentsWidgetAttrs extends RequiresPluginInfos {
  agents: Agents;
  isUserAdmin: boolean;
}

export class ElasticAgentsWidget extends MithrilViewComponent<AgentsWidgetAttrs> {
  view(vnode: m.Vnode<AgentsWidgetAttrs>) {
    const tableData = vnode.attrs.agents.list().map((agent: Agent) => {
      const tableCellClasses = ElasticAgentsWidget.tableCellClasses(agent);
      return [
        <div class={classnames(tableCellClasses, style.hostname)} data-test-id={`agent-hostname-of-${agent.uuid}`}>
          {ElasticAgentsWidget.getHostnameLink(vnode.attrs.isUserAdmin, agent)}
        </div>,
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
             data-test-id={`agent-environments-of-${agent.uuid}`}>{ElasticAgentsWidget.joinOrNoneSpecified(agent.environmentNames())}</div>
      ];
    });

    return <div class={style.agentsTable} onclick={ElasticAgentsWidget.hideBuildDetails.bind(this, vnode.attrs.agents)}>
      <Table data={tableData}
             headers={["Agent Name", "Sandbox", "OS", "IP Address", "Status", "Free Space", "Environments"]}
             sortHandler={vnode.attrs.agents.elasticAgents()}/>
    </div>;
  }

  private static joinOrNoneSpecified(array: string[]): m.Children {
    if (array && array.length > 0) {
      return array.join(", ");
    } else {
      return (<em>none specified</em>);
    }
  }

  private static hideBuildDetails(agents: Agents) {
    agents.buildDetailsForAgent("");
  }

  private static tableCellClasses(agent: Agent) {
    return classnames(style.tableCell,
                      {[style.building]: agent.isBuilding()},
                      {[style.disabledAgent]: agent.agentConfigState === AgentConfigState.Disabled});
  }

  private static getHostnameLink(isUserAdmin: boolean, agent: Agent) {
    if (!isUserAdmin) {
      return (<span>{agent.hostname}</span>);
    }

    return <a href={`/go/agents/${agent.uuid}/job_run_history`}>{agent.hostname}</a>;
  }
}
