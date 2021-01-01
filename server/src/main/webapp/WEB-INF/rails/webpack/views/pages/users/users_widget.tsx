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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {User, Users} from "models/users/users";
import * as Icons from "views/components/icons";
import {
  RequiresUserViewHelper,
  SuperAdminPrivilegeSwitch,
  SuperAdminPrivilegeSwitchAttrs
} from "views/pages/users/super_admin_toggle_widget";
import {Attrs as UserActionsState, UsersActionsWidget} from "views/pages/users/user_actions_widget";
import styles from "./index.scss";

const classnames = bind(styles);

export interface Attrs extends UserActionsState, SuperAdminPrivilegeSwitchAttrs, RequiresUserViewHelper {

}

export class UsersTableWidget extends MithrilViewComponent<Attrs> {
  static headers(users: Users) {
    return [
      <input type="checkbox"
             class={styles.formCheck}
             checked={users.areAllUsersSelected()}
             onclick={users.toggleSelection.bind(users)}/>,
      "Username",
      "Display name",
      <span>
        Roles
        <div class={classnames(styles.roleLegends)}>
          <div class={classnames(styles.roleGocd)}>GoCD</div>
          <div class={classnames(styles.rolePlugin)}>Plugin</div>
        </div>
      </span>,
      "System Admin",
      "Email",
      "Enabled"
    ];
  }

  view(vnode: m.Vnode<Attrs>) {
    return (
      <div class={styles.flexTable} data-test-id="users-table">
        <div class={styles.tableHeader} data-test-id="users-header">
          {
            _.map(UsersTableWidget.headers(vnode.attrs.users() as Users), (header) => {
              return <div class={styles.tableHead}>{header}</div>;
            })
          }
        </div>
        <div class={styles.tableBody}>
          {
            vnode.attrs.users().map((user: User) => {
              const className   = (user.enabled() ? "" : styles.disabled);
              const selectedRow = (user.checked() ? styles.selected : "");

              return [
                <div class={classnames(styles.tableRow, selectedRow, className)} data-test-id="user-row">
                  <div class={styles.tableCell}>
                    <input type="checkbox" class={styles.formCheck}
                           checked={user.checked()}
                           onclick={() => {
                             user.checked(!user.checked());
                           }}/>
                  </div>
                  <div class={styles.tableCell} data-test-id="user-username">
                    <span>{user.loginName()}</span>
                  </div>
                  <div class={styles.tableCell} data-test-id="user-display-name">
                    <span>{user.displayName()}</span>
                  </div>
                  <div class={styles.tableCell} data-test-id="user-roles">
                    <span>{UsersTableWidget.roles(user)}</span>
                  </div>
                  <div class={classnames(styles.tableCell, styles.systemAdminCell)}
                       data-test-id="user-super-admin-switch">
                    <SuperAdminPrivilegeSwitch user={user}
                                               userViewHelper={vnode.attrs.userViewHelper}
                                               onToggleAdmin={vnode.attrs.onToggleAdmin}/>
                    {this.maybeShowOperationStatusIcon(vnode, user)}
                  </div>
                  <div class={styles.tableCell} data-test-id="user-email">
                    <span>{user.email()}</span>
                  </div>
                  <div class={styles.tableCell} data-test-id="user-enabled">
                    <span>{user.enabled() ? "Yes" : "No"}</span>
                  </div>
                </div>,
                this.maybeShowOperationErrorMessage(vnode, user)
              ];
            })
          }
        </div>
      </div>);
  }

  maybeShowOperationErrorMessage(vnode: m.Vnode<Attrs>, user: User) {
    if (vnode.attrs.userViewHelper().hasError(user)) {
      return <div class={classnames(styles.tableRow)}>
        <div class={styles.alertError} data-test-id="user-update-error-message">
          <p>{vnode.attrs.userViewHelper().errorMessageFor(user)}</p>
        </div>
      </div>;
    }
  }

  private static roles(user: User) {
    return (
      <span>
        {user.gocdRoles().map((roleName) => {
          return <span class={classnames(styles.gocdRole)}>{roleName}</span>;
        })}
        {user.pluginRoles().map((roleName) => {
          return <span class={classnames(styles.pluginRole)}>{roleName}</span>;
        })}
      </span>
    );
  }

  private maybeShowOperationStatusIcon(vnode: m.Vnode<Attrs>, user: User) {
    if (vnode.attrs.userViewHelper().knowsAbout(user)) {
      if (vnode.attrs.userViewHelper().isInProgress(user)) {
        return <Icons.Spinner iconOnly={true} title={"Update in progress"}/>;
      }
      if (vnode.attrs.userViewHelper().hasError(user)) {
        return <span class={styles.iconError} data-test-id="update-unsuccessful"/>;
      }

      if (vnode.attrs.userViewHelper().isUpdatedSuccessfully(user)) {
        return <span class={styles.iconCheck} data-test-id="update-successful"/>;
      }
    }
  }
}

export class UsersWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return [
      <UsersActionsWidget {...vnode.attrs} />,
      <UsersTableWidget {...vnode.attrs}/>
    ];
  }
}
