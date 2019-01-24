/*
* Copyright 2018 ThoughtWorks, Inc.
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
import * as m from "mithril";
import {User, Users} from "models/users/users";
import {Table} from "views/components/table";
import {SuperAdminPrivilegeSwitch} from "views/pages/users/super_admin_toggle_widget";
import {State as UserActionsState, UsersActionsWidget} from "views/pages/users/user_actions_widget";
import * as styles from "./index.scss";

const classnames = bind(styles);

export class UsersTableWidget extends MithrilViewComponent<UserActionsState> {
  static headers(users: Users) {
    return [
      <input type="checkbox"
             checked={users.areAllUsersSelected()}
             onclick={users.toggleSelection.bind(users)}/>,
      "Username",
      "Display name",
      <span>
        Roles
        <div className={classnames(styles.roleLegends)}>
          <div className={classnames(styles.roleGocd)}>GoCD</div>
          <div className={classnames(styles.rolePlugin)}>Plugin</div>
        </div>
      </span>,
      "System Admin",
      "Email",
      "Enabled"
    ];
  }

  static userData(users: Users, vnode: m.Vnode<UserActionsState>): any[][] {
    return users.map((user: User) => {
      const className = (user.enabled() ? "" : styles.disabled);
      return [
        <input type="checkbox" checked={user.checked()} onclick={m.withAttr("checked", user.checked)}/>,
        <span className={className}>{user.loginName()}</span>,
        <span className={className}>{user.displayName()}</span>,
        <span className={className}>{this.roles(user)}</span>,
        <SuperAdminPrivilegeSwitch user={user}
                                   noAdminsConfigured={vnode.attrs.noAdminsConfigured}
                                   onRemoveAdmin={vnode.attrs.onRemoveAdmin}
                                   onMakeAdmin={vnode.attrs.onMakeAdmin}/>,
        <span className={className}>{user.email()}</span>,
        <span className={className}>{user.enabled() ? "Yes" : "No"}</span>
      ];
    });
  }

  view(vnode: m.Vnode<UserActionsState>) {
    return <Table headers={UsersTableWidget.headers(vnode.attrs.users() as Users)}
                  data={UsersTableWidget.userData(vnode.attrs.users(), vnode)}/>;
  }

  private static roles(user: User) {
    return (
      <span>
        {user.gocdRoles().map((roleName) => {
          return <span className={classnames(styles.gocdRole)}>{roleName}</span>;
        })}
        {user.pluginRoles().map((roleName) => {
          return <span className={classnames(styles.pluginRole)}>{roleName}</span>;
        })}
      </span>
    );
  }
}

export class UsersWidget extends MithrilViewComponent<UserActionsState> {
  view(vnode: m.Vnode<UserActionsState>) {
    return [
      <UsersActionsWidget {...vnode.attrs} />,
      <UsersTableWidget {...vnode.attrs}/>
    ];
  }
}
