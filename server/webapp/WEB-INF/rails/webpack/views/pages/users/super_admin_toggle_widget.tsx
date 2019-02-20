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

import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {User} from "models/users/users";
import {SwitchBtn} from "views/components/switch";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import * as styles from "./index.scss";

interface MakeAdminOperation<T> {
  onMakeAdmin: (obj: T, e: MouseEvent) => void;
}

interface RemoveAdminOperation<T> {
  onRemoveAdmin: (obj: T, e: MouseEvent) => void;
}

export interface SuperAdminPrivilegeSwitchAttrs extends MakeAdminOperation<User>, RemoveAdminOperation<User> {
  user: User;
  noAdminsConfigured: Stream<boolean>;
}

export interface State {
  isAdmin: Stream<boolean>;
  onToggleClick: (e: MouseEvent) => any;
  populateUserToOperate: (user: User) => void;
}

export class SuperAdminPrivilegeSwitch extends MithrilComponent<SuperAdminPrivilegeSwitchAttrs, State> {
  oninit(vnode: m.Vnode<SuperAdminPrivilegeSwitchAttrs, State>) {
    vnode.state.isAdmin       = stream((vnode.attrs.user.isAdmin() && !vnode.attrs.noAdminsConfigured()) || false);
    vnode.state.onToggleClick = (e: MouseEvent) => {
      (vnode.state.isAdmin())
        ? vnode.attrs.onRemoveAdmin(vnode.attrs.user, e)
        : vnode.attrs.onMakeAdmin(vnode.attrs.user, e);
    };

  }

  onbeforeupdate(vnode: m.Vnode<SuperAdminPrivilegeSwitchAttrs, State>,
                 old: m.VnodeDOM<SuperAdminPrivilegeSwitchAttrs, State>): boolean | void {
    vnode.state.isAdmin((vnode.attrs.user.isAdmin() && !vnode.attrs.noAdminsConfigured()));
  }

  view(vnode: m.Vnode<SuperAdminPrivilegeSwitchAttrs, State>) {

    let optionalTooltip;
    let isAdminText = vnode.attrs.user.isAdmin() ? "YES" : "NO";
    const loginName = vnode.attrs.user.loginName;

    if (vnode.attrs.noAdminsConfigured()) {
      optionalTooltip = (<Tooltip.Info size={TooltipSize.small}
                                       content={`Explicitly making '${loginName}' user a system administrator will result into other users not having system administrator privileges.`}/>);
      isAdminText     = "Not Specified";
    } else if (vnode.attrs.user.isAdmin() && !vnode.attrs.user.isIndividualAdmin()) {
      optionalTooltip = (<Tooltip.Info size={TooltipSize.small}
                                       content={`'${loginName}' user has the system administrator privileges because the user is assigned the group administrative role. To remove this user from system administrators, assigned role needs to be removed.`}/>);
    }

    return <div class={styles.adminSwitchWrapper} data-test-id="admin-switch-wrapper">
      <SwitchBtn field={vnode.state.isAdmin}
                 small={true}
                 disabled={vnode.attrs.user.isAdmin() && !vnode.attrs.user.isIndividualAdmin()}
                 onclick={vnode.state.onToggleClick.bind(vnode.state)}/>
      <span class={styles.isAdminText} data-test-id="is-admin-text">{isAdminText}</span>
      {optionalTooltip}
    </div>;
  }
}
