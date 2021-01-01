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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Agent} from "models/agents/agents";
import {AnalyticsCapability} from "models/shared/plugin_infos_new/analytics_plugin_capabilities";
import {AnalyticsInteractionManager} from "views/pages/analytics/analytics_interaction_manager";
import {AnalyticsModal} from "views/pages/analytics/analytics_modal";
import style from "./index.scss";

const PluginEndpointRequestHandler = require("rails-shared/plugin-endpoint-request-handler");
const PluginEndpoint               = require("rails-shared/plugin-endpoint");

interface WidgetAttrs {
  agent: Agent;
  supportedAnalytics: { [key: string]: AnalyticsCapability[] };
}

class AgentAnalyticsModal extends AnalyticsModal<Agent> {
  title(): string {
    return `Analytics for agent: ${this.entity.hostname}`;
  }

  protected getUrlParams(): { [p: string]: string | number } {
    return {
      agent_uuid: this.entity.uuid,
      agent_hostname: this.entity.hostname,
      key: "analytics.agent-chart"
    };
  }
}

export class AgentAnalyticsWidget extends MithrilViewComponent<WidgetAttrs> {
  private namespace = AnalyticsInteractionManager.ensure().ns("AgentMetrics");

  view(vnode: m.Vnode<WidgetAttrs>): m.Children {
    return <a class={style.agentAnalytics}
              data-test-id={`analytics-icon-${vnode.attrs.agent.uuid}`}
              onclick={() => this.onClick(vnode)}/>;
  }

  private onClick(vnode: m.Vnode<WidgetAttrs>) {
    PluginEndpointRequestHandler.defineLinkHandler();
    PluginEndpoint.ensure("v1");
    new AgentAnalyticsModal(vnode.attrs.agent, vnode.attrs.supportedAnalytics, this.namespace).render();
  }
}
