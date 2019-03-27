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
import {FlashMessage, FlashMessageModelWithTimeout} from "views/components/flash_message";
import * as Icons from "views/components/icons";
import {
  RequiresUserViewHelper,
  SuperAdminPrivilegeSwitch,
  SuperAdminPrivilegeSwitchAttrs
} from "views/pages/users/super_admin_toggle_widget";
import {State as UserActionsState, UsersActionsWidget} from "views/pages/users/user_actions_widget";
import * as styles from "./index.scss";

const classnames = bind(styles);

export interface Attrs extends UserActionsState, SuperAdminPrivilegeSwitchAttrs, RequiresUserViewHelper {
  flashMessage?: FlashMessageModelWithTimeout;
}

export class UsersTableWidget extends MithrilViewComponent<Attrs> {
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

  view(vnode: m.Vnode<Attrs>) {
    const flashMessage = vnode.attrs.flashMessage ?
      <FlashMessage message={vnode.attrs.flashMessage.message} type={vnode.attrs.flashMessage.type}/> : null;
    return (
      <div className={styles.flexTable} data-test-id="users-table">
        {flashMessage}
        <div className={styles.tableHeader} data-test-id="users-header">
          {
            _.map(UsersTableWidget.headers(vnode.attrs.users() as Users), (header) => {
              return <div className={styles.tableHead}>{header}</div>;
            })
          }
        </div>
        <div className="table-body">
          {
            vnode.attrs.users().map((user: User) => {
              const className   = (user.enabled() ? "" : styles.disabled);
              const selectedRow = (user.checked() ? styles.selected : "");

              return [
                <div className={classnames(styles.tableRow, selectedRow, className)} data-test-id="user-row">
                  <div className={styles.tableCell}>
                    <input type="checkbox" className={styles.formCheck}
                           checked={user.checked()}
                           onclick={m.withAttr("checked", user.checked)}/>
                  </div>
                  <div className={styles.tableCell} data-test-id="user-username">
                    <span>{user.loginName()}</span>
                  </div>
                  <div className={styles.tableCell} data-test-id="user-display-name">
                    <span>{user.displayName()}</span>
                  </div>
                  <div className={styles.tableCell} data-test-id="user-roles">
                    <span>{UsersTableWidget.roles(user)}</span>
                  </div>
                  <div className={classnames(styles.tableCell, styles.systemAdminCell)}
                       data-test-id="user-super-admin-switch">
                    <SuperAdminPrivilegeSwitch user={user}
                                               userViewHelper={vnode.attrs.userViewHelper}
                                               onToggleAdmin={vnode.attrs.onToggleAdmin}/>
                    {this.maybeShowOperationStatusIcon(vnode, user)}
                  </div>
                  <div className={styles.tableCell} data-test-id="user-email">
                    <span>{user.email()}</span>
                  </div>
                  <div className={styles.tableCell} data-test-id="user-enabled">
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
      return <div className={classnames(styles.tableRow)}>
        <div className={styles.alertError} data-test-id="user-update-error-message">
          <p>{vnode.attrs.userViewHelper().errorMessageFor(user)}</p>
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

  private maybeShowOperationStatusIcon(vnode: m.Vnode<Attrs>, user: User) {
    if (vnode.attrs.userViewHelper().knowsAbout(user)) {
      if (vnode.attrs.userViewHelper().isInProgress(user)) {
        return <Icons.Spinner iconOnly={true} title={"Update in progress"}/>;
      }
      if (vnode.attrs.userViewHelper().hasError(user)) {
        return <span className={styles.iconError} data-test-id="update-unsuccessful"/>;
      }

      if (vnode.attrs.userViewHelper().isUpdatedSuccessfully(user)) {
        return <span className={styles.iconCheck} data-test-id="update-successful"/>;
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
