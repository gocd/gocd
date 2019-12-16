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
import {Agent} from "models/agents/agents";
import {GetAllService} from "models/agents/agents_crud";
import {StaticAgentsVM} from "models/agents/agents_vm";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";
import * as Buttons from "views/components/buttons";
import {Dropdown, DropdownAttrs, Primary} from "views/components/buttons";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {QuickAddField, TriStateCheckboxField} from "views/components/forms/input_fields";
import {Spinner} from "views/components/spinner";
import Style from "./index.scss";
import spinnerCss from "./spinner.scss";

export interface MyDropdownAttrs extends DropdownAttrs {
  service: GetAllService;
  agentsVM: StaticAgentsVM;
  flashMessage: FlashMessageModelWithTimeout;
}

export abstract class AbstractDropdownButton<V extends DropdownAttrs> extends Dropdown<V> {
  protected readonly data: Stream<string[]>                             = Stream();
  protected readonly operationInProgress: Stream<boolean>               = Stream();
  protected readonly triStateCheckboxMap: Map<string, TriStateCheckbox> = new Map<string, TriStateCheckbox>();

  toggleDropdown(vnode: m.Vnode<DropdownAttrs & V>, e: MouseEvent) {
    super.toggleDropdown(vnode, e);
    if (vnode.attrs.show()) {
      this.operationInProgress(true);
      vnode.attrs.service.all((data: string) => {
        this.operationInProgress(false);
        this.data(JSON.parse(data));
        this.buildTriStateCheckBox(vnode);
        m.redraw.sync();
      }, () => "");
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
        <Spinner small={true} css={spinnerCss}/>
      </div>;
    }

    return <div data-test-id="association" class={Style.dropdownContent}>{this.body(vnode)}</div>;
  }

  protected apply(vnode: m.Vnode<DropdownAttrs & V>) {
    this.operationInProgress(true);
    this.updatePromise(vnode).finally(() => vnode.attrs.show(false));
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
      const numberOfAgentsWithSpecifiedEnv = vnode.attrs.agentsVM.list().reduce((count: number, agent: Agent) => {
        if (vnode.attrs.agentsVM.isAgentSelected(agent.uuid) && this.hasAssociationWith(agent, item)) {
          count += 1;
        }
        return count;
      }, 0);

      switch (numberOfAgentsWithSpecifiedEnv) {
        case 0:
          this.triStateCheckboxMap.set(item, new TriStateCheckbox(TristateState.off));
          break;
        case vnode.attrs.agentsVM.selectedAgentsUUID().length:
          this.triStateCheckboxMap.set(item, new TriStateCheckbox(TristateState.on));
          break;
        default:
          this.triStateCheckboxMap.set(item, new TriStateCheckbox(TristateState.indeterminate));
          break;
      }
    });
  }

  protected abstract hasAssociationWith(agent: Agent, item: string): boolean;
}

interface ResourcesAttrs extends MyDropdownAttrs {
  updateResources: (resourcesToAdd: string[], resourcesToRemove: string[]) => Promise<ApiResult<string>>;
}

export class ResourcesDropdownButton extends AbstractDropdownButton<ResourcesAttrs> {
  private newResource: Stream<string> = Stream();

  protected updatePromise(vnode: m.Vnode<DropdownAttrs & ResourcesAttrs>) {
    return vnode.attrs.updateResources(this.getKeysOfSelectedCheckBoxes(), this.getKeysOfUnselectedCheckBoxes());
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & ResourcesAttrs>) {
    return <Buttons.Primary data-test-id="modify-resources-association"
                            dropdown={true}
                            disabled={vnode.attrs.agentsVM.selectedAgentsUUID().length === 0}
                            onclick={(e: MouseEvent) => {
                              vnode.attrs.agentsVM.showEnvironments(false);
                              this.toggleDropdown(vnode, e);
                            }}>RESOURCES</Buttons.Primary>;
  }

  protected body(vnode: m.Vnode<DropdownAttrs & ResourcesAttrs>) {
    return [
      Array.from(this.triStateCheckboxMap).map(([resource, triStateCheckbox]) => {
        return <TriStateCheckboxField label={resource} property={Stream(triStateCheckbox)}/>;
      }),
      <div class={Style.quickAdd}>
        <QuickAddField dataTestId={"resource-to-add"} property={this.newResource}
                       buttonDisableReason=""
                       onclick={this.addNewResource.bind(this)}/>
      </div>,
      <div class={Style.dropdownContentFooter}>
        <Primary data-test-id="resources-to-apply" onclick={this.apply.bind(this, vnode)}>Apply</Primary>
      </div>
    ];
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
