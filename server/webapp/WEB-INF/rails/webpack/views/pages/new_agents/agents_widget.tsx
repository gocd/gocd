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

import {ApiResult} from "helpers/api_request_builder";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Agents} from "models/new_agent/agents";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {Tabs} from "views/components/tab";
import {RequiresPluginInfos} from "views/pages/page_operations";
import {StaticAgentsWidget} from "./static_agents_widget";

interface AgentsWidgetAttrs extends RequiresPluginInfos {
  agents: Agents;
  onEnable: (e: MouseEvent) => void;
  onDisable: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  flashMessage: FlashMessageModelWithTimeout;
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<ApiResult<string>>;
  updateResources: (resourcesToAdd: string[], resourcesToRemove: string[]) => Promise<ApiResult<string>>;
  isUserAdmin: boolean;
  showAnalyticsIcon: boolean;
}

export class AgentsWidget extends MithrilViewComponent<AgentsWidgetAttrs> {
  view(vnode: m.Vnode<AgentsWidgetAttrs>) {
    return <Tabs tabs={["Static", "Elastic"]} contents={[
      <StaticAgentsWidget agents={vnode.attrs.agents}
                          onEnable={vnode.attrs.onEnable}
                          onDisable={vnode.attrs.onDisable}
                          onDelete={vnode.attrs.onDelete}
                          flashMessage={vnode.attrs.flashMessage}
                          updateEnvironments={vnode.attrs.updateEnvironments.bind(vnode.attrs)}
                          updateResources={vnode.attrs.updateResources}
                          showAnalyticsIcon={vnode.attrs.showAnalyticsIcon}
                          pluginInfos={vnode.attrs.pluginInfos}
                          isUserAdmin={vnode.attrs.isUserAdmin}/>,
      "Foo"
    ]}/>;
  }
}
