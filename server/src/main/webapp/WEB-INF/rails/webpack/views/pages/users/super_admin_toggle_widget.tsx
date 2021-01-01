/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {User} from "models/users/users";
import {SwitchBtn} from "views/components/switch";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {UserViewHelper} from "views/pages/users/user_view_helper";
import styles from "./index.scss";

interface ToggleAdminOperation<T> {
  onToggleAdmin: (e: MouseEvent, obj: T) => void;
}

export interface RequiresUserViewHelper {
  userViewHelper: Stream<UserViewHelper>;
}

export interface SuperAdminPrivilegeSwitchAttrs extends ToggleAdminOperation<User>, RequiresUserViewHelper {
  user: User;
}

export class  SuperAdminPrivilegeSwitch extends MithrilComponent<SuperAdminPrivilegeSwitchAttrs> {
  view(vnode: m.Vnode<SuperAdminPrivilegeSwitchAttrs>): m.Children {
    let optionalTooltip;
    let isAdminText          = vnode.attrs.user.isAdmin() ? "YES" : "NO";
    const loginName          = vnode.attrs.user.loginName();
    const userViewHelper     = vnode.attrs.userViewHelper();
    const isUserActualAdmin  = (!userViewHelper.noAdminsConfigured() && vnode.attrs.user.isAdmin());
    const isUserGroupedAdmin = isUserActualAdmin && !userViewHelper.isIndividualAdmin(vnode.attrs.user);

    if (userViewHelper.noAdminsConfigured()) {
      optionalTooltip = (
        <Tooltip.Info size={TooltipSize.small}
                      content={`Explicitly making '${loginName}' user a system administrator will result into other users not having system administrator privileges.`}/>
      );
      isAdminText     = "Not Specified";
    } else if (isUserGroupedAdmin) {
      optionalTooltip = (
        <Tooltip.Info size={TooltipSize.small}
                      content={`'${loginName}' user has the system administrator privileges because the user is assigned the group administrative role. To remove this user from system administrators, assigned role needs to be removed.`}/>
      );
    }

    return <div class={styles.adminSwitchWrapper} data-test-id="admin-switch-wrapper">
      <SwitchBtn field={Stream((vnode.attrs.user.isAdmin() && !userViewHelper.noAdminsConfigured()))}
                 small={true}
                 disabled={isUserGroupedAdmin || !vnode.attrs.user.enabled() || userViewHelper
                   .isInProgress(vnode.attrs.user)}
                 onclick={this.toggle.bind(this, vnode)}/>
      <span class={styles.isAdminText} data-test-id="is-admin-text">{isAdminText}</span>
      {optionalTooltip}
    </div>;
  }

  private toggle(vnode: m.Vnode<SuperAdminPrivilegeSwitchAttrs>, e: MouseEvent) {
    vnode.attrs.onToggleAdmin(e, vnode.attrs.user);
  }
}
