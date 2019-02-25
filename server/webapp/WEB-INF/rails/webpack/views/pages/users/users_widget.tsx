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
import * as _ from "lodash";
import * as m from "mithril";
import {User, Users} from "models/users/users";
import * as Icons from "views/components/icons";
import {SuperAdminPrivilegeSwitch, SuperAdminPrivilegeSwitchAttrs} from "views/pages/users/super_admin_toggle_widget";
import {State as UserActionsState, UsersActionsWidget} from "views/pages/users/user_actions_widget";
import * as styles from "./index.scss";

const classnames = bind(styles);

export enum UpdateOperationStatus {
  IN_PROGRESS, SUCCESS, ERROR
}

interface UserViewModel {
  updateOperationStatus: UpdateOperationStatus;
  updateOperationErrorMessage?: string | null;
}

export interface State extends UserActionsState, SuperAdminPrivilegeSwitchAttrs {
  userViewStates: {
    [key: string]: UserViewModel;
  };
}

export class UsersTableWidget extends MithrilViewComponent<State> {
  static headers(users: Users) {
    return [
      <input type="checkbox"
             className={styles.formCheck}
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

  view(vnode: m.Vnode<State>) {
    return (<div className={styles.flexTable} data-test-id="users-table">
      <div className={styles.flTblHeader} data-test-id="users-header">
        {
          _.map(UsersTableWidget.headers(vnode.attrs.users() as Users), (header) => {
            return <div className={styles.flHead}>{header}</div>;
          })
        }
      </div>
      <div className="table-body">
        {
          vnode.attrs.users().map((user: User) => {
            const className   = (user.enabled() ? "" : styles.disabled);
            const selectedRow = (user.checked() ? styles.selected : "");

            return [<div className={classnames(styles.flRow, selectedRow, className)} data-test-id="user-row">
              <div className={styles.flCell}>
                <input type="checkbox" className={styles.formCheck}
                       checked={user.checked()}
                       onclick={m.withAttr("checked", user.checked)}/>
              </div>
              <div className={styles.flCell} data-test-id="user-username">
                <span>{user.loginName()}</span>
              </div>
              <div className={styles.flCell} data-test-id="user-display-name">
                <span>{user.displayName()}</span>
              </div>
              <div className={styles.flCell} data-test-id="user-roles">
                <span>{UsersTableWidget.roles(user)}</span>
              </div>
              <div className={classnames(styles.flCell, styles.systemAdminCell)} data-test-id="user-super-admin-switch">
                <SuperAdminPrivilegeSwitch user={user}
                                           noAdminsConfigured={vnode.attrs.noAdminsConfigured}
                                           onRemoveAdmin={vnode.attrs.onRemoveAdmin}
                                           onMakeAdmin={vnode.attrs.onMakeAdmin}
                                           systemAdminUsers={vnode.attrs.systemAdminUsers}/>
                {this.toggleOperationStatusIcon(vnode, user)}
              </div>
              <div className={styles.flCell} data-test-id="user-email">
                <span>{user.email()}</span>
              </div>
              <div className={styles.flCell} data-test-id="user-enabled">
                <span>{user.enabled() ? "Yes" : "No"}</span>
              </div>
            </div>,
              this.updateOperationErrorMessage(vnode, user)
            ];
          })
        }
      </div>
    </div>);
  }

  updateOperationErrorMessage(vnode: m.Vnode<State>, user: User) {
    const userViewState = vnode.attrs.userViewStates[user.loginName()];
    if (userViewState && userViewState.updateOperationErrorMessage) {
      return <div className={classnames(styles.flRow)}>
        <div className={styles.alertError} data-test-id="user-update-error-message">
          <p>{userViewState.updateOperationErrorMessage}</p>
        </div>
      </div>;
    }
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

  private toggleOperationStatusIcon(vnode: m.Vnode<State>, user: User) {
    let icon;
    const userViewState = vnode.attrs.userViewStates[user.loginName()];
    if (userViewState) {
      switch (userViewState.updateOperationStatus) {
        case UpdateOperationStatus.IN_PROGRESS:
          icon = <Icons.Spinner iconOnly={true} title={"Update in progress"}/>;
          break;
        case UpdateOperationStatus.SUCCESS:
          icon = <span class={styles.iconCheck} data-test-id="update-successful"/>;
          break;
        case UpdateOperationStatus.ERROR:
          icon = <span className={styles.iconError} data-test-id="update-unsuccessful"/>;
          break;
      }
    }
    return icon;
  }
}

export class UsersWidget extends MithrilViewComponent<State> {
  view(vnode: m.Vnode<State>) {
    return [
      <UsersActionsWidget {...vnode.attrs} />,
      <UsersTableWidget {...vnode.attrs}/>
    ];
  }
}
