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

import {bind} from "classnames/bind";
import {ApiResult} from "helpers/api_request_builder";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {Agent, AgentConfigState} from "models/agents/agents";
import {StaticAgentsVM} from "models/agents/agents_vm";
import {AnalyticsCapability} from "models/shared/plugin_infos_new/analytics_plugin_capabilities";
import {AnalyticsExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, FlashMessageModelWithTimeout} from "views/components/flash_message";
import {Table} from "views/components/table";
import {AgentAnalyticsWidget} from "views/pages/agents/agent_analytics_widget";
import {AgentHeaderPanel} from "views/pages/agents/agent_header_panel";
import {AgentStatusWidget} from "views/pages/agents/agent_status_widget";
import {RequiresPluginInfos} from "views/pages/page_operations";
import style from "./index.scss";

const classnames = bind(style);

interface AgentsWidgetAttrs extends RequiresPluginInfos {
  agentsVM: StaticAgentsVM;
  onEnable: (e: MouseEvent) => void;
  onDisable: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  flashMessage: FlashMessageModelWithTimeout;
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<ApiResult<string>>;
  updateResources: (resourcesToAdd: string[], resourcesToRemove: string[]) => Promise<ApiResult<string>>;
  isUserAdmin: boolean;
  showAnalyticsIcon: boolean;
}

export class StaticAgentsWidget extends MithrilViewComponent<AgentsWidgetAttrs> {

  static joinOrNoneSpecified(array: string[]): m.Children {
    if (array && array.length > 0) {
      return array.join(", ");
    } else {
      return (<em>none specified</em>);
    }
  }

  view(vnode: m.Vnode<AgentsWidgetAttrs>) {
    let flashMessage;

    if (vnode.attrs.flashMessage) {
      flashMessage = <FlashMessage message={vnode.attrs.flashMessage.message} type={vnode.attrs.flashMessage.type}/>;
    }

    const tableData = vnode.attrs.agentsVM.list().map((agent: Agent) => {
      const tableCellClasses = StaticAgentsWidget.tableCellClasses(agent);
      return [
        <div key={agent.uuid} class={classnames(tableCellClasses, style.agentCheckbox)}>
          {StaticAgentsWidget.checkBoxFor(agent, vnode)}
        </div>,
        <div class={classnames(tableCellClasses, style.hostname)}
             data-test-id={`agent-hostname-of-${agent.uuid}`}>{StaticAgentsWidget.getHostnameLink(vnode.attrs.isUserAdmin,
                                                                                                  agent)}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-sandbox-of-${agent.uuid}`}>{agent.sandbox}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-operating-system-of-${agent.uuid}`}>{agent.operatingSystem}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-ip-address-of-${agent.uuid}`}>{agent.ipAddress}</div>,
        <AgentStatusWidget agent={agent} buildDetailsForAgent={vnode.attrs.agentsVM.showBuildDetailsForAgent}
                           cssClasses={tableCellClasses}/>,
        <div class={tableCellClasses}
             data-test-id={`agent-free-space-of-${agent.uuid}`}>{agent.readableFreeSpace()}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-resources-of-${agent.uuid}`}>{StaticAgentsWidget.joinOrNoneSpecified(agent.resources)}</div>,
        <div class={tableCellClasses}
             data-test-id={`agent-environments-of-${agent.uuid}`}>{StaticAgentsWidget.joinOrNoneSpecified(agent.environmentNames())}</div>,
        <div class={classnames(tableCellClasses, style.agentAnalytics)}
             data-test-id={`agent-analytics-of-${agent.uuid}`}>{StaticAgentsWidget.analyticsIcon(agent, vnode)}</div>
      ];
    });

    return <div class={style.agentsTable}
                onclick={StaticAgentsWidget.hideBuildDetails.bind(this, vnode.attrs.agentsVM)}>
      <AgentHeaderPanel {...vnode.attrs}/>
      {flashMessage}
      <Table data={tableData}
             headers={[
               StaticAgentsWidget.globalCheckBox(vnode),
               "Agent Name", "Sandbox", "OS", "IP Address", "Status", "Free Space", "Resources", "Environments", ""]}
             sortHandler={vnode.attrs.agentsVM.agentsSortHandler}/>

    </div>;
  }

  private static hideBuildDetails(agentsVM: StaticAgentsVM) {
    agentsVM.showBuildDetailsForAgent("");
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

  private static globalCheckBox(vnode: m.Vnode<AgentsWidgetAttrs>) {
    if (vnode.attrs.isUserAdmin) {
      return <input type="checkbox"
                    data-test-id={"select-all-agents"}
                    checked={vnode.attrs.agentsVM.isAllStaticAgentSelected()}
                    onclick={() => vnode.attrs.agentsVM.toggleAgentsSelection()}/>;
    }

    return null;
  }

  private static checkBoxFor(agent: Agent, vnode: m.Vnode<AgentsWidgetAttrs>) {
    if (vnode.attrs.isUserAdmin) {
      return <input type="checkbox"
                    data-test-id={`agent-checkbox-of-${agent.uuid}`}
                    checked={vnode.attrs.agentsVM.isAgentSelected(agent.uuid)}
                    onclick={() => vnode.attrs.agentsVM.toggleAgentSelection(agent.uuid)}/>;
    }
    return null;
  }

  private static analyticsIcon(agent: Agent, vnode: m.Vnode<AgentsWidgetAttrs>) {
    if (!vnode.attrs.showAnalyticsIcon) {
      return;
    }

    const agentAnalytics = this.getAgentAnalyticsByPlugin(vnode.attrs.pluginInfos());

    if (_.isEmpty(agentAnalytics)) {
      return;
    }

    return <AgentAnalyticsWidget agent={agent} supportedAnalytics={agentAnalytics}/>;
  }

  private static getAgentAnalyticsByPlugin(pluginInfos: PluginInfos): { [key: string]: AnalyticsCapability[] } {
    const reduceFn = (filteredPluginInfos: { [key: string]: AnalyticsCapability[] }, pluginInfo: PluginInfo) => {
      const analyticsExtension = pluginInfo.extensionOfType(ExtensionTypeString.ANALYTICS) as AnalyticsExtension;
      if (analyticsExtension) {
        const agentAnalytics = analyticsExtension.capabilities.agentSupport();
        if (analyticsExtension && agentAnalytics.length > 0) {
          filteredPluginInfos[pluginInfo.id] = agentAnalytics;
        }
      }
      return filteredPluginInfos;
    };

    return pluginInfos.reduce(reduceFn, {});
  }
}
