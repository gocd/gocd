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
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {AuthConfig, AuthConfigs} from "models/auth_configs/auth_configs";
import {Directive, GoCDRole, PluginRole, RoleType} from "models/roles/roles";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Spinner} from "views/components/spinner";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";
import {GoCDRoleModalBodyWidget, PluginRoleModalBodyWidget} from "views/pages/roles/role_modal_body_widget";
import styles from "./index.scss";
import {CreatePolicyWidget} from "./policy_widget";

const foundationClassNames = bind(foundationStyles);

export enum Action {
  NEW, CLONE, EDIT
}

interface RoleModalAttrs {
  role: Stream<GoCDRole | PluginRole>;
  pluginInfos: PluginInfos;
  authConfigs: AuthConfigs;
  action: Action;
  message?: string;
  isStale?: Stream<boolean>;
  changeRoleType?: (roleType: RoleType) => void;
  resourceAutocompleteHelper: Map<string, string[]>;
}

export class RoleModalBody extends MithrilViewComponent<RoleModalAttrs> {
  view(vnode: m.Vnode<RoleModalAttrs>): m.Children | void | null {
    if (vnode.attrs.message) {
      return (<FlashMessage type={MessageType.alert} message={vnode.attrs.message}/>);
    }

    if (!vnode.attrs.role || (vnode.attrs.isStale && vnode.attrs.isStale())) {
      return <div class={styles.spinnerWrapper}><Spinner/></div>;
    }

    let mayBeTypeSelector: any;
    if (vnode.attrs.action === Action.NEW) {
      const canAddPluginRole = RoleModalBody.hasAuthConfigs(vnode);
      let icon;
      if (!canAddPluginRole) {
        const title = "Either no plugin has authorization capability or there are no authorization configs defined for the same.";
        icon        = <Tooltip.Help size={TooltipSize.small} content={title}/>;
      }
      mayBeTypeSelector = (
        <div data-test-id="role-type-selector">
          <label class="inline">Select type of role:&nbsp;&nbsp;&nbsp;</label>
          <input
            class="core-role"
            id="core-role"
            name="role-type-selector"
            type="radio"
            checked={!vnode.attrs.role().isPluginRole()}
            onclick={vnode.attrs.changeRoleType && vnode.attrs.changeRoleType.bind(this, RoleType.gocd)}/>
          <label class="inline" for="core-role">GoCD Role</label>

          <input
            class="plugin-role"
            name="role-type-selector"
            id="plugin-role"
            type="radio"
            disabled={!canAddPluginRole}
            checked={vnode.attrs.role().isPluginRole()}
            onclick={vnode.attrs.changeRoleType && vnode.attrs.changeRoleType.bind(this, RoleType.plugin)}/>
          <label class={!canAddPluginRole ? `${styles.disabled} inline` : "inline"}
                 disabled={!canAddPluginRole} for="plugin-role">
            Plugin Role
            {icon}
          </label>
        </div>
      );
    }

    let roleWidget: any;
    if (vnode.attrs.role().isPluginRole()) {
      roleWidget = (
        <PluginRoleModalBodyWidget isNameDisabled={vnode.attrs.action === Action.EDIT}
                                   role={vnode.attrs.role()}
                                   pluginInfos={vnode.attrs.pluginInfos}
                                   authConfigs={vnode.attrs.authConfigs}/>);
    } else {
      roleWidget = (
        <GoCDRoleModalBodyWidget isNameDisabled={vnode.attrs.action === Action.EDIT}
                                 role={vnode.attrs.role()}/>);
    }

    const addNewPermission = () => {
      vnode.attrs.role().policy().push(Stream(new Directive("", "", "", "")));
    };

    return (<div class={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        {mayBeTypeSelector}
        {roleWidget}
        <CreatePolicyWidget policy={vnode.attrs.role().policy}
                            resourceAutocompleteHelper={vnode.attrs.resourceAutocompleteHelper}/>
        <div class={styles.addPermission}>
          <Buttons.Secondary data-test-id="add-permission-button" onclick={addNewPermission}>
            + New Permission
          </Buttons.Secondary>
        </div>
      </div>
    );
  }

  private static hasAuthConfigs(vnode: m.Vnode<RoleModalAttrs>) {
    const authConfigsWithInstalledPlugin = _.filter(vnode.attrs.authConfigs, (authConfig: AuthConfig) => {
      const authConfigWithPlugin = _.find(vnode.attrs.pluginInfos,
                                          (pluginInfo) => {
                                            return authConfig.pluginId() === pluginInfo.id;
                                          });
      if (authConfigWithPlugin) {
        return true;
      }
    });
    return authConfigsWithInstalledPlugin.length !== 0;
  }
}
