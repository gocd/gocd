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
import {AgentConfigState, Agents} from "models/new_agent/agents";
import {EnvironmentsService, ResourcesService} from "models/new_agent/agents_crud";
import {ButtonGroup} from "views/components/buttons";
import * as Buttons from "views/components/buttons";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {KeyValuePair} from "views/components/key_value_pair";
import {EnvironmentsDropdownButton, ResourcesDropdownButton} from "./environments_and_resources_dropdown_button";
import style from "./index.scss";
import m from "mithril";

const classnames = bind(style);

interface AgentHeaderPanelAttrs {
  agents: Agents;
  onEnable: (e: MouseEvent) => void;
  onDisable: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  flashMessage: FlashMessageModelWithTimeout;
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<any>;
  updateResources: (resourcesToAdd: string[], resourcesToRemove: string[]) => Promise<any>;
}

export class AgentHeaderPanel extends MithrilViewComponent<AgentHeaderPanelAttrs> {
  view(vnode: m.Vnode<AgentHeaderPanelAttrs, this>) {
    const agents = vnode.attrs.agents;
    return (<div class={style.headerPanel}>
      <div class={style.leftContainer}>
        <ButtonGroup>
          <Buttons.Primary data-test-id="delete-agents"
                           disabled={agents.isNoneSelected()}
                           onclick={vnode.attrs.onDelete}>DELETE</Buttons.Primary>
          <Buttons.Primary data-test-id="enable-agents"
                           disabled={agents.isNoneSelected()}
                           onclick={vnode.attrs.onEnable}>ENABLE</Buttons.Primary>
          <Buttons.Primary data-test-id="disable-agents"
                           disabled={agents.isNoneSelected()}
                           onclick={vnode.attrs.onDisable}>DISABLE</Buttons.Primary>
          <EnvironmentsDropdownButton show={vnode.attrs.agents.showEnvironments}
                                      agents={agents}
                                      updateEnvironments={vnode.attrs.updateEnvironments}
                                      flashMessage={vnode.attrs.flashMessage}
                                      service={new EnvironmentsService()}/>
          <ResourcesDropdownButton show={vnode.attrs.agents.showResources}
                                   agents={agents}
                                   updateResources={vnode.attrs.updateResources}
                                   flashMessage={vnode.attrs.flashMessage}
                                   service={new ResourcesService()}/>
        </ButtonGroup>

        <KeyValuePair inline={true} data={new Map(
          [
            ["Total", this.span(agents.count())],
            ["Pending", this.span(agents.filterBy(AgentConfigState.Pending).length)],
            ["Enabled", this.span(agents.filterBy(AgentConfigState.Enabled).length, style.enabled)],
            ["Disabled", this.span(agents.filterBy(AgentConfigState.Disabled).length, style.disabled)]
          ])
        }/>
      </div>

      <SearchField placeholder="Filter Agents" label="Search for agents" property={agents.filterText}/>
    </div>);
  }

  private span(count: number, className: string = ""): m.Children {
    return <span class={classnames(style.count, className)}>{count}</span>;
  }
}