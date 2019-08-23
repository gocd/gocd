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

import {ApiResult, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import {Agent, Agents} from "models/new_agent/agents";
import {GetAllService} from "models/new_agent/agents_crud";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";
import {Dropdown, DropdownAttrs, Primary} from "views/components/buttons";
import * as Buttons from "views/components/buttons";
import m from "mithril";
import Stream from "mithril/stream";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import {QuickAddField, TriStateCheckboxField} from "views/components/forms/input_fields";
import {Spinner} from "views/components/spinner";
import Style from "./index.scss";

interface Attrs {
  service: GetAllService;
  flashMessage: FlashMessageModelWithTimeout;
}

abstract class AbstractDropdownButton<V extends Attrs> extends Dropdown<V> {
  protected readonly data: Stream<string[]>                             = Stream();
  protected readonly operationInProgress: Stream<boolean>               = Stream();
  protected readonly triStateCheckboxMap: Map<string, TriStateCheckbox> = new Map<string, TriStateCheckbox>();

  toggleDropdown(vnode: m.Vnode<DropdownAttrs & V>, e: MouseEvent) {
    super.toggleDropdown(vnode, e);
    if (vnode.attrs.show()) {
      this.operationInProgress(true);
      vnode.attrs.service.all()
           .then((result: ApiResult<string>) => this.onResult(result, vnode))
           .finally(this.buildTriStateCheckBox.bind(this, vnode));
    }
  }

  protected abstract updatePromise(vnode: m.Vnode<DropdownAttrs & V>): Promise<ApiResult<string>>;

  protected abstract body(vnode: m.Vnode<DropdownAttrs & V>): m.Children;

  protected doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & V>) {
    if (!vnode.attrs.show()) {
      return;
    }

    if (this.operationInProgress()) {
      return <div class={Style.dropdownContent}>
        <Spinner small={true}/>
      </div>;
    }

    return this.body(vnode);
  }

  protected apply(vnode: m.Vnode<DropdownAttrs & V>) {
    this.operationInProgress(true);
    this.updatePromise(vnode).finally(() => vnode.attrs.show(false));
  }

  private onResult(result: ApiResult<string>, vnode: m.Vnode<DropdownAttrs & V>) {
    this.operationInProgress(false);
    result.do(this.onSuccess.bind(this), (errorResponse) => this.onFailure(errorResponse, vnode));
  }

  private onSuccess(successResponse: SuccessResponse<string>) {
    this.data(JSON.parse(successResponse.body));
  }

  private onFailure(errorResponse: ErrorResponse, vnode: m.Vnode<DropdownAttrs & V>) {
    vnode.attrs.flashMessage.setMessage(MessageType.alert, errorResponse.message);
  }

  protected getKeysOfSelectedCheckBoxes() {
    return Array.from(this.triStateCheckboxMap.keys())
                .reduce((listOfEnvs: string[], environment: string) => {
                  if (this.triStateCheckboxMap.get(environment)!.isChecked()) {
                    listOfEnvs.push(environment);
                  }
                  return listOfEnvs;
                }, []);
  }

  protected getKeysOfUnselectedCheckBoxes() {
    return Array.from(this.triStateCheckboxMap.keys())
                .reduce((listOfEnvs: string[], environment: string) => {
                  if (this.triStateCheckboxMap.get(environment)!.isUnchecked()) {
                    listOfEnvs.push(environment);
                  }
                  return listOfEnvs;
                }, []);
  }

  protected buildTriStateCheckBox(vnode: m.Vnode<DropdownAttrs & V>) {
    this.triStateCheckboxMap.clear();
    this.data().map((item) => {
      const selectedAgents                 = vnode.attrs.agents.getSelectedAgents();
      const numberOfAgentsWithSpecifiedEnv = selectedAgents.reduce((count: number, agent: Agent) => {
        if (this.hasAssociationWith(agent, item)) {
          count += 1;
        }
        return count;
      }, 0);

      const state = numberOfAgentsWithSpecifiedEnv === selectedAgents.length ? TristateState.on : numberOfAgentsWithSpecifiedEnv === 0 ? TristateState.off : TristateState.indeterminate;
      this.triStateCheckboxMap.set(item, new TriStateCheckbox(state));
    });
  }

  protected abstract hasAssociationWith(agent: Agent, item: string): boolean;
}

interface EnvAttrs extends Attrs {
  agents: Agents;
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<ApiResult<string>>;
}

export class EnvironmentsDropdownButton extends AbstractDropdownButton<EnvAttrs> {
  protected updatePromise(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    return vnode.attrs.updateEnvironments(this.getKeysOfSelectedCheckBoxes(), this.getKeysOfUnselectedCheckBoxes());
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    return <Buttons.Primary data-test-id="modify-environments-association"
                            dropdown={true}
                            disabled={vnode.attrs.agents.isNoneSelected()}
                            onclick={(e) => {
                              vnode.attrs.agents.showResources(false);
                              this.toggleDropdown(vnode, e);
                            }}>ENVIRONMENTS</Buttons.Primary>;
  }

  protected body(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    return (<div class={Style.dropdownContent}>
      {Array.from(this.triStateCheckboxMap).map(([environment, triStateCheckbox]) => {
        return <TriStateCheckboxField label={environment} property={Stream(triStateCheckbox)}/>;
      })}
      <Primary onclick={this.apply.bind(this, vnode)}>Apply</Primary>
    </div>);
  }

  protected hasAssociationWith(agent: Agent, environment: string) {
    return agent.environmentNames().includes(environment);
  }
}

interface ResourcesAttrs extends Attrs {
  agents: Agents;
  updateResources: (resourcesToAdd: string[], resourcesToRemove: string[]) => Promise<ApiResult<string>>;
}

export class ResourcesDropdownButton extends AbstractDropdownButton<ResourcesAttrs> {
  private newResource: Stream<string> = Stream();

  protected updatePromise(vnode: m.Vnode<DropdownAttrs & ResourcesAttrs>) {
    return vnode.attrs.updateResources(this.getKeysOfSelectedCheckBoxes(), this.getKeysOfUnselectedCheckBoxes());
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & ResourcesAttrs>) {
    return <Buttons.Primary data-test-id="modify-environments-association"
                            dropdown={true}
                            disabled={vnode.attrs.agents.isNoneSelected()}
                            onclick={(e) => {
                              vnode.attrs.agents.showEnvironments(false);
                              this.toggleDropdown(vnode, e);
                            }}>RESOURCES</Buttons.Primary>;
  }

  protected body(vnode: m.Vnode<DropdownAttrs & ResourcesAttrs>) {
    return (<div class={Style.dropdownContent}>
      {Array.from(this.triStateCheckboxMap).map(([resource, triStateCheckbox]) => {
        return <TriStateCheckboxField label={resource} property={Stream(triStateCheckbox)}/>;
      })}
      <QuickAddField property={this.newResource} buttonDisableReason=""
                     onclick={this.addNewResource.bind(this)}/>
      <Primary onclick={this.apply.bind(this, vnode)}>Apply</Primary>
    </div>);
  }

  protected hasAssociationWith(agent: Agent, resource: string) {
    return agent.resources.includes(resource);
  }

  private addNewResource() {
    this.data().push(this.newResource());
    this.triStateCheckboxMap.set(this.newResource(), new TriStateCheckbox(TristateState.on));
    this.newResource("");
  }
}