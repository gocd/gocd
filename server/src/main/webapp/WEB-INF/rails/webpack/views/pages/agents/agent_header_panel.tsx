/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import m from "mithril";
import {AgentConfigState} from "models/agents/agents";
import {EnvironmentsService, ResourcesService} from "models/agents/agents_crud";
import {StaticAgentsVM} from "models/agents/agents_vm";
import {ButtonGroup} from "views/components/buttons";
import * as Buttons from "views/components/buttons";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {KeyValuePair} from "views/components/key_value_pair";
import {EnvironmentsDropdownButton} from "views/pages/agents/environment_dropdown_button";
import {ResourcesDropdownButton} from "views/pages/agents/resources_dropdown_button";
import style from "./index.scss";

const classnames = bind(style);

interface AgentHeaderPanelAttrs {
  agentsVM: StaticAgentsVM;
  onEnable: (e: MouseEvent) => void;
  onDisable: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  flashMessage: FlashMessageModelWithTimeout;
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<ApiResult<string>>;
  updateResources: (resourcesToAdd: string[], resourcesToRemove: string[]) => Promise<ApiResult<string>>;
}

export class AgentHeaderPanel extends MithrilViewComponent<AgentHeaderPanelAttrs> {
  view(vnode: m.Vnode<AgentHeaderPanelAttrs, this>) {
    const agentsVM = vnode.attrs.agentsVM;
    return (<div class={style.headerPanel}>
      <div class={style.leftContainer}>
        <ButtonGroup>
          <Buttons.Primary data-test-id="delete-agents"
                           disabled={AgentHeaderPanel.isNoneSelected(agentsVM)}
                           onclick={vnode.attrs.onDelete}>DELETE</Buttons.Primary>
          <Buttons.Primary data-test-id="enable-agents"
                           disabled={AgentHeaderPanel.isNoneSelected(agentsVM)}
                           onclick={vnode.attrs.onEnable}>ENABLE</Buttons.Primary>
          <Buttons.Primary data-test-id="disable-agents"
                           disabled={AgentHeaderPanel.isNoneSelected(agentsVM)}
                           onclick={vnode.attrs.onDisable}>DISABLE</Buttons.Primary>
          <EnvironmentsDropdownButton show={agentsVM.showEnvironments}
                                      agentsVM={agentsVM}
                                      updateEnvironments={vnode.attrs.updateEnvironments}
                                      flashMessage={vnode.attrs.flashMessage}
                                      service={new EnvironmentsService()}/>
          <ResourcesDropdownButton show={agentsVM.showResources}
                                   agentsVM={agentsVM}
                                   updateResources={vnode.attrs.updateResources}
                                   flashMessage={vnode.attrs.flashMessage}
                                   service={new ResourcesService()}/>
        </ButtonGroup>

        <KeyValuePair inline={true} data={new Map(
          [
            ["Total", this.span(agentsVM.list().length)],
            ["Pending", this.span(agentsVM.filterBy(AgentConfigState.Pending).length)],
            ["Enabled", this.span(agentsVM.filterBy(AgentConfigState.Enabled).length, style.enabled)],
            ["Disabled", this.span(agentsVM.filterBy(AgentConfigState.Disabled).length, style.disabled)]
          ])
        }/>
      </div>

      <SearchField placeholder="Filter Agents" label="Search for agents" property={agentsVM.filterText}/>
    </div>);
  }

  private static isNoneSelected(agentsVM: StaticAgentsVM) {
    return agentsVM.selectedAgentsUUID().length === 0;
  }

  private span(count: number, className: string = ""): m.Children {
    return <span class={classnames(style.count, className)}>{count}</span>;
  }
}
