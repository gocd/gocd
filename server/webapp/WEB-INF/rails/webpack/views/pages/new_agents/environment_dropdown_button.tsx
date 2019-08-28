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
import m from "mithril";
import Stream from "mithril/stream";
import {Agent, Agents} from "models/new_agent/agents";
import {DropdownAttrs, Primary} from "views/components/buttons";
import {TriStateCheckboxField} from "views/components/forms/input_fields";
import {AbstractDropdownButton} from "./resources_dropdown_button";

interface EnvAttrs extends DropdownAttrs {
  agents: Agents;
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<ApiResult<string>>;
}

export class EnvironmentsDropdownButton extends AbstractDropdownButton<EnvAttrs> {
  protected updatePromise(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    return vnode.attrs.updateEnvironments(this.getKeysOfSelectedCheckBoxes(), this.getKeysOfUnselectedCheckBoxes());
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    return <Primary data-test-id="modify-environments-association"
                    dropdown={true}
                    disabled={vnode.attrs.agents.isNoneSelected()}
                    onclick={(e) => {
                      vnode.attrs.agents.showResources(false);
                      this.toggleDropdown(vnode, e);
                    }}>ENVIRONMENTS</Primary>;
  }

  protected body(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    if (this.triStateCheckboxMap.size === 0) {
      return <strong>No environments are defined.</strong>;
    }

    return [
      Array.from(this.triStateCheckboxMap).map(([environment, triStateCheckbox]) => {
        return <TriStateCheckboxField label={environment} property={Stream(triStateCheckbox)}/>;
      }),
      <Primary data-test-id="environment-to-apply" onclick={this.apply.bind(this, vnode)}>Apply</Primary>
    ];
  }

  protected hasAssociationWith(agent: Agent, environment: string) {
    return agent.environmentNames().includes(environment);
  }
}
