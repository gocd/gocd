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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Agent} from "models/agents/agents";
import {StaticAgentsVM} from "models/agents/agents_vm";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";
import {DropdownAttrs, Primary} from "views/components/buttons";
import {TriStateCheckboxField} from "views/components/forms/input_fields";
import {Info} from "views/components/tooltip";
import Style from "views/pages/agents/index.scss";
import {AbstractDropdownButton} from "./resources_dropdown_button";

interface EnvAttrs extends DropdownAttrs {
  updateEnvironments: (environmentsToAdd: string[], environmentsToRemove: string[]) => Promise<ApiResult<string>>;
  agentsVM: StaticAgentsVM;
}

export class EnvironmentsDropdownButton extends AbstractDropdownButton<EnvAttrs> {
  protected updatePromise(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    return vnode.attrs.updateEnvironments(this.getKeysOfSelectedCheckBoxes(), this.getKeysOfUnselectedCheckBoxes());
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    return <Primary data-test-id="modify-environments-association"
                    dropdown={true}
                    disabled={vnode.attrs.agentsVM.selectedAgentsUUID().length === 0}
                    onclick={(e: MouseEvent) => {
                      vnode.attrs.agentsVM.showResources(false);
                      this.toggleDropdown(vnode, e);
                    }}>ENVIRONMENTS</Primary>;
  }

  protected body(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    if (this.triStateCheckboxMap.size === 0) {
      return <strong>No environments are defined.</strong>;
    }

    return [
      Array.from(this.triStateCheckboxMap).map(([environment, triStateCheckbox]) => {
        const tooltipMsg = (triStateCheckbox as EnvironmentInfo).getToolTipMsg();
        const tooltip = (tooltipMsg.length > 0) ? <Info content={tooltipMsg}/> : null;
        return <div class={Style.environmentInfo}>
          <TriStateCheckboxField label={environment}
                                 property={Stream(triStateCheckbox)}
                                 readonly={triStateCheckbox.isDisabled()}/>
          {tooltip}
        </div>;
      }),
      <div class={Style.dropdownContentFooter}>
        <Primary data-test-id="environment-to-apply"
                 onclick={this.apply.bind(this, vnode)} disabled={this.areAllEnvsDisabled()}>Apply</Primary>
      </div>
    ];
  }

  protected hasAssociationWith(agent: Agent, environment: string) {
    return agent.environmentNames().includes(environment);
  }

  protected buildTriStateCheckBox(vnode: m.Vnode<DropdownAttrs & EnvAttrs>) {
    this.triStateCheckboxMap.clear();

    const selectedAgentsUUID = vnode.attrs.agentsVM.selectedAgentsUUID();
    const allAgents = vnode.attrs.agentsVM.all();

    const allSelectedAgents = allAgents.filter((agent) => selectedAgentsUUID.includes(agent.uuid));
    const allSelectedAgentsEnvs = allSelectedAgents.map((agent) => agent.environments);
    const allAgentsEnvs = _.flatMap(allSelectedAgentsEnvs).map((env) => env.name);

    const allUniqueEnvs = _.uniq(allAgentsEnvs.concat(this.data())).sort();

    allUniqueEnvs.map((item) => {
      let isAssociatedViaConfigRepo = false;
      let unknownEnvCount = 0;
      const numberOfAgentsWithSpecifiedEnv = vnode.attrs.agentsVM.list().reduce((count: number, agent: Agent) => {
        if (vnode.attrs.agentsVM.isAgentSelected(agent.uuid) && this.hasAssociationWith(agent, item)) {
          count += 1;
          const agentsEnvironment = agent.environments.filter((agentEnv) => agentEnv.name === item);

          isAssociatedViaConfigRepo = isAssociatedViaConfigRepo || agentsEnvironment
            .some((agentEnv) => agentEnv.isAssociatedFromConfigRepo());
          unknownEnvCount += agentsEnvironment.filter((agentEnv) => agentEnv.isUnknown()).length;
        }
        return count;
      }, 0);

      const isUnknown = unknownEnvCount === 0 ? false : unknownEnvCount !== allSelectedAgents.length;

      const disabled = isAssociatedViaConfigRepo || isUnknown;
      const msg = EnvironmentsDropdownButton.getTooltipContent(isAssociatedViaConfigRepo, isUnknown);

      switch (numberOfAgentsWithSpecifiedEnv) {
        case 0:
          this.triStateCheckboxMap.set(item, new EnvironmentInfo(TristateState.off, disabled, msg));
          break;
        case vnode.attrs.agentsVM.selectedAgentsUUID().length:
          this.triStateCheckboxMap.set(item, new EnvironmentInfo(TristateState.on, disabled, msg));
          break;
        default:
          this.triStateCheckboxMap.set(item, new EnvironmentInfo(TristateState.indeterminate, disabled, msg));
          break;
      }
    });
  }

  private static getTooltipContent(disabled: boolean, unknown: boolean) {
    if (disabled) {
      return "Cannot edit Environment associated from Config Repo";
    } else if (unknown) {
      return "Environment is not defined in config XML";
    }
    return "";
  }

  private areAllEnvsDisabled() {
    return _.every(Array.from(this.triStateCheckboxMap.values()), (checkbox: TriStateCheckbox) => checkbox.isDisabled());
  }
}

class EnvironmentInfo extends TriStateCheckbox {
  private toolTipMsg: string;

  constructor(initialState: TristateState = TristateState.indeterminate, disabled = false, toolTipMsg: string = "") {
    super(initialState, disabled);
    this.toolTipMsg = toolTipMsg;
  }

  getToolTipMsg(): string {
    return this.toolTipMsg;
  }
}
