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
import {Agent} from "models/new_agent/agents";
import {DropdownAttrs, Primary} from "views/components/buttons";
import {TriStateCheckboxField} from "views/components/forms/input_fields";
import Style from "views/pages/new_agents/index.scss";
import {StaticAgentsVM} from "../../../models/new_agent/agents_vm";
import {TriStateCheckbox, TristateState} from "../../../models/tri_state_checkbox";
import {Info} from "../../components/tooltip";
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
                    onclick={(e) => {
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
          <TriStateCheckboxField label={environment} property={Stream(triStateCheckbox)} readonly={triStateCheckbox.isDisabled()}/>
          {tooltip}
        </div>;
      }),
      <div class={Style.dropdownContentFooter}>
        <Primary data-test-id="environment-to-apply"
                 onclick={this.apply.bind(this, vnode)}>Apply</Primary>
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
      const numberOfAgentsWithSpecifiedEnv = vnode.attrs.agentsVM.list().reduce((count: number, agent: Agent) => {
        if (vnode.attrs.agentsVM.isAgentSelected(agent.uuid) && this.hasAssociationWith(agent, item)) {
          count += 1;
        }
        return count;
      }, 0);

      const disabledDueToConfigRepoAssociation = EnvironmentsDropdownButton.isAssociatedViaConfigRepo(item, allSelectedAgents);
      const disabledDueToUnknown = EnvironmentsDropdownButton.isUnknown(item, allSelectedAgents);

      const disabled = disabledDueToConfigRepoAssociation || disabledDueToUnknown;
      const msg = EnvironmentsDropdownButton.getTooltipContent(disabledDueToConfigRepoAssociation, disabledDueToUnknown);

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

  private static isAssociatedViaConfigRepo(envName: string, agents: Agent[]): boolean {
    if (!agents || agents.length === 0) {
      return false;
    }

    return _.flatMap(agents.map((agent) => agent.environments))
        .filter((env) => env.name === envName)
        .map((env) => env.isAssociatedFromConfigRepo())
        .reduce((accumalator, currentValue) => accumalator || currentValue, false);
  }

  private static isUnknown(envName: string, agents: Agent[]): boolean {
    if (!agents || agents.length === 0) {
      return false;
    }

    const hasUnknownEnvs = _.flatMap(agents.map((agent) => agent.environments))
        .filter((env) => env.name === envName && env.isUnknown());

    return hasUnknownEnvs.length === 0 ? false : hasUnknownEnvs.length !== agents.length;
  }

  private static getTooltipContent(disabled: boolean, unknown: boolean) {
    if (disabled) {
      return "Cannot edit Environment associated from Config Repo";
    } else if (unknown) {
      return "Environment is not defined in config XML";
    }
    return "";
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
