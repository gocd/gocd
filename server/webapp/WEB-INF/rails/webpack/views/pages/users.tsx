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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {AdminsCRUD, BulkUpdateSystemAdminJSON} from "models/admins/admin_crud";
import {BulkUserRoleUpdateJSON, GoCDAttributes, GoCDRole, Roles} from "models/roles/roles";
import {RolesCRUD} from "models/roles/roles_crud";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";
import {computeBulkUpdateRolesJSON, computeRolesSelection} from "models/users/role_selection";
import {UserFilters} from "models/users/user_filters";
import {BulkUserUpdateJSON, User, Users} from "models/users/users";
import {UsersCRUD} from "models/users/users_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {Page, PageState} from "views/pages/page";
import {AddOperation} from "views/pages/page_operations";
import {UserSearchModal} from "views/pages/users/add_user_modal";
import {DeleteUserConfirmModal} from "views/pages/users/delete_user_confirmation_modal";
import {State as UserActionsState, UsersActionsWidget} from "views/pages/users/user_actions_widget";
import {UserViewHelper} from "views/pages/users/user_view_helper";
import {Attrs as UsersWidgetState, UsersTableWidget} from "views/pages/users/users_widget";
import * as styles from "./users/index.scss";

interface State extends UserActionsState, AddOperation<Users>, UsersWidgetState {
  initialUsers: Stream<Users>;
}

export class UsersPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.initialUsers   = stream(new Users());
    vnode.state.userViewHelper = stream(new UserViewHelper());
    vnode.state.userFilters    = stream(new UserFilters());
    vnode.state.roles          = stream(new Roles());
    vnode.state.rolesSelection = stream(new Map<GoCDRole, TriStateCheckbox>());

    vnode.state.showFilters   = stream(false);
    vnode.state.showRoles     = stream(false);
    vnode.state.roleNameToAdd = stream();

    vnode.state.initializeRolesDropdownAttrs = () => {
      if (vnode.state.showRoles()) {
        vnode.state.rolesSelection(computeRolesSelection(vnode.state.roles(),
                                                         vnode.state.users().selectedUsers()));
        vnode.state.roleNameToAdd("");
      }
    };

    vnode.state.onAdd = (e) => {
      e.stopPropagation();
      new UserSearchModal(this.flashMessage, this.fetchData.bind(this, vnode)).render();
    };

    vnode.state.onEnable = (usersToEnable, e) => {
      const json = {
        operations: {
          enable: true,
        },
        users: usersToEnable.userNamesOfSelectedUsers()
      };

      this.bulkUserStateChange(vnode, json);
    };

    vnode.state.onDisable = (usersToDisable, e) => {
      const json = {
        operations: {
          enable: false,
        },
        users: usersToDisable.userNamesOfSelectedUsers()
      };

      this.bulkUserStateChange(vnode, json);
    };

    vnode.state.onDelete = (usersToDelete, e) => {
      const enabledUsers = usersToDelete.selectedUsers().enabledUsers();
      if (enabledUsers.length > 0) {
        const verbPhrase = enabledUsers.length === 1 ? `is` : `are`;
        const message    = `'${enabledUsers.userNamesOfSelectedUsers()
                                           .join(",")}' ${verbPhrase} enabled. Can't perform the requested opertaion.`;
        this.flashMessage.setMessage(MessageType.alert,
                                     message);
        return;
      }
      const json = {
        users: usersToDelete.userNamesOfSelectedUsers()
      };

      new DeleteUserConfirmModal(json,
                                 (msg) => {
                                   this.pageState = PageState.OK;
                                   this.flashMessage.setMessage(MessageType.success, msg);
                                   this.fetchData(vnode);
                                   this.scrollToTop();
                                 },
                                 (errorResponse) => {
                                   this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                                   this.fetchData(vnode);
                                   this.scrollToTop();
                                 })
        .render();
    };

    vnode.state.onToggleAdmin = (e: MouseEvent, user: User) => {
      vnode.state.userViewHelper().userUpdateInProgress(user);
      const addUserToSystemAdmins = vnode.state.userViewHelper().noAdminsConfigured() || !user.isAdmin();
      const json                  = {
        operations: {
          users: {
            [addUserToSystemAdmins ? "add" : "remove"]: [user.loginName()]
          }
        }
      };
      this.updateSystemAdminPrivilegeForUser(vnode, json, user);
    };

    vnode.state.onRolesAdd = (roleName: string, users: Users) => {
      const gocdAttributes = new GoCDAttributes(users.userNamesOfSelectedUsers());
      const role           = new GoCDRole(roleName, gocdAttributes);

      RolesCRUD.create(role)
               .then((apiResult) => {
                 apiResult.do((successResponse) => {
                   vnode.state.roleNameToAdd("");
                   vnode.state.roles().push(role);
                   vnode.state.rolesSelection().set(role, new TriStateCheckbox(TristateState.on));
                 }, (errorResponse) => {
                   this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                 });
               });
    };

    vnode.state.onRolesUpdate = (rolesSelection: Map<GoCDRole, TriStateCheckbox>, users: Users) => {
      const bulkUpdateJSON = computeBulkUpdateRolesJSON(rolesSelection, users);
      this.bulkUpdateExistingRolesOnUsers(vnode, bulkUpdateJSON);
      vnode.state.showRoles(false);
    };

    vnode.state.users = () => vnode.state.userFilters()
                                   .performFilteringOn(vnode.state.initialUsers())
                                   .sortedByUsername();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    let bannerToDisplay;
    if (vnode.state.userViewHelper().noAdminsConfigured()) {
      bannerToDisplay = (<FlashMessage type={MessageType.warning}
                                       message="There are currently no administrators defined in the configuration. This makes everyone an administrator. We recommend that you explicitly make user a system administrator."/>);
    }

    return (
      <div>
        {bannerToDisplay}
        <div className={styles.flashMessageWrapperContainer}>
          <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type} dismissible={false}/>
        </div>
        <UsersTableWidget {...vnode.state} hasMessage={this.flashMessage.hasMessage()}/>
      </div>
    );
  }

  pageName(): string {
    return "Users Management";
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [];
    headerButtons.push(<Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Import User</Buttons.Primary>);

    return <div>
      <HeaderPanel title="Users Management" buttons={headerButtons}/>
      <UsersActionsWidget {...vnode.state} />
    </div>;

  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([AdminsCRUD.all(), UsersCRUD.all(), RolesCRUD.all("gocd")]).then((args) => {
      const adminsResult = args[0];
      const userResult   = args[1];
      const rolesResult  = args[2];

      userResult.do((successResponse) => {
                      vnode.state.initialUsers(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                      this.pageState = PageState.FAILED;
                    }
      );

      rolesResult.do((successResponse) => {
                       vnode.state.roles(successResponse.body);
                       this.pageState = PageState.OK;
                     }, (errorResponse) => {
                       this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                       this.pageState = PageState.FAILED;
                     }
      );

      adminsResult.do((successResponse) => {
                        vnode.state.userViewHelper().systemAdmins(successResponse.body);
                        this.pageState = PageState.OK;
                      },
                      (errorResponse) => {
                        this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                        this.pageState = PageState.FAILED;
                      });
    });
  }

  private bulkUserStateChange(vnode: m.Vnode<null, State>, json: BulkUserUpdateJSON): void {
    UsersCRUD.bulkUserStateUpdate(json)
             .then((apiResult) => {
               apiResult.do((successResponse) => {
                              this.pageState = PageState.OK;
                              this.flashMessage.setMessage(MessageType.success,
                                                           `Users were ${json.operations.enable ? "enabled" : "disabled"} successfully!`);
                              this.fetchData(vnode);
                              this.scrollToTop();
                            },
                            (errorResponse) => {
                              this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                              this.fetchData(vnode);
                              this.scrollToTop();
                            });
             });
  }

  private updateSystemAdminPrivilegeForUser(vnode: m.Vnode<null, State>,
                                            json: BulkUpdateSystemAdminJSON,
                                            user: User): void {
    AdminsCRUD.bulkUpdate(json)
              .then((apiResult) => {
                apiResult.do((bulkUpdateSuccess) => {
                               this.fetchData(vnode).then(() => {
                                 vnode.state.userViewHelper().userUpdateSuccessful(user);
                                 m.redraw();
                               });
                             },
                             (errorResponse) => {
                               vnode.state.userViewHelper().userUpdateFailure(user, errorResponse.message);
                             });
              });
  }

  private bulkUpdateExistingRolesOnUsers(vnode: m.Vnode<null, State>, bulkUpdateJSON: BulkUserRoleUpdateJSON) {
    RolesCRUD.bulkUserRoleUpdate(bulkUpdateJSON)
             .then((apiResult) => {
               apiResult.do((successResponse) => {
                              this.pageState = PageState.OK;
                              this.flashMessage.setMessage(MessageType.success, "Roles are updated successfully!");
                              this.fetchData(vnode);
                            },
                            (errorResponse) => {
                              this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                              this.fetchData(vnode);
                            });
             });
  }

}
