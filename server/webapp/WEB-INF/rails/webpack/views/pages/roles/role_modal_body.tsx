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

import classnames from "classnames";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {AuthConfig, AuthConfigs} from "models/auth_configs/auth_configs";
import {GoCDRole, PluginRole, RoleType} from "models/roles/roles";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Spinner} from "views/components/spinner";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";
import {GoCDRoleModalBodyWidget, PluginRoleModalBodyWidget} from "views/pages/roles/role_modal_body_widget";
import * as styles from "./index.scss";

export enum Action {
  NEW, CLONE, EDIT
}

interface RoleModalAttrs {
  role: Stream<GoCDRole | PluginRole>;
  pluginInfos: Array<PluginInfo<Extension>>;
  authConfigs: AuthConfigs;
  action: Action;
  message?: string;
  isStale?: Stream<boolean>;
  changeRoleType?: (roleType: RoleType) => void;
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
            disabled={!RoleModalBody.hasAuthConfigs(vnode)}
            checked={vnode.attrs.role().isPluginRole()}
            onclick={vnode.attrs.changeRoleType && vnode.attrs.changeRoleType.bind(this, RoleType.plugin)}/>
          <label class={!RoleModalBody.hasAuthConfigs(vnode) ? `${styles.disabled} inline` : "inline"}
                 disabled={!RoleModalBody.hasAuthConfigs(vnode)} for="plugin-role">Plugin Role</label>
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

    return (<div class={classnames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        {mayBeTypeSelector}
        {roleWidget}
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
