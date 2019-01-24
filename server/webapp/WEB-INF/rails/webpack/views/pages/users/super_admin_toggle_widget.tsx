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
import {User, Users} from "models/users/users";
import {SwitchBtn} from "views/components/switch";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import * as styles from "./index.scss";

interface SuperAdminPrivilegeSwitchAttrs {
  user: User;
  onRemoveAdmin: (users: Users, e: MouseEvent) => void;
  onMakeAdmin: (users: Users, e: MouseEvent) => void;
  noAdminsConfigured: Stream<boolean>;
}

interface State {
  userToOperate: User;
  onToggleClick: (e: MouseEvent) => any;
  populateUserToOperate: (user: User) => void;
}

export class SuperAdminPrivilegeSwitch extends MithrilComponent<SuperAdminPrivilegeSwitchAttrs, State> {
  oninit(vnode: m.Vnode<SuperAdminPrivilegeSwitchAttrs, State>) {
    vnode.state.onToggleClick = (e: MouseEvent) => {
      (vnode.state.userToOperate.isAdmin())
        ? vnode.attrs.onRemoveAdmin(new Users(vnode.state.userToOperate), e)
        : vnode.attrs.onMakeAdmin(new Users(vnode.state.userToOperate), e);
    };

    vnode.state.populateUserToOperate = (user: User) => {
      vnode.state.userToOperate = User.clone(user);

      //if no super admins are configured.. the admin is normal user and can be elevated to system admin.
      if (vnode.attrs.noAdminsConfigured()) {
        vnode.state.userToOperate.isAdmin(false);
      }

      vnode.state.userToOperate.checked(true);
    };
  }

  view(vnode: m.Vnode<SuperAdminPrivilegeSwitchAttrs, State>) {
    vnode.state.populateUserToOperate(vnode.attrs.user);

    let optionalTooltip;
    let isAdminText = vnode.state.userToOperate.isAdmin() ? "YES" : "NO";

    if (vnode.attrs.noAdminsConfigured()) {
      const loginName = vnode.attrs.user.loginName;
      optionalTooltip = (<Tooltip.Info size={TooltipSize.small}
                                       content={`Explicitly making '${loginName}' user a system administrator will result into other users not having system administrator privileges.`}/>);
      isAdminText     = "Not Specified";
    }

    return (
      <div class={styles.adminSwitchWrapper} data-test-id="admin-switch-wrapper">
        <SwitchBtn field={vnode.state.userToOperate.isAdmin}
                   small={true}
                   onclick={vnode.state.onToggleClick.bind(vnode.state)}/>
        <span class={styles.isAdminText} data-test-id="is-admin-text">{isAdminText}</span>
        {optionalTooltip}
      </div>
    );
  }
}
