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

import {GoCDRole, Roles, UserRoleUpdateJSON} from "models/roles/roles";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";
import {Users} from "models/users/users";

export function computeRolesSelection(allRoles: Roles, selectedUsers: Users) {
  const rolesSelection: Map<GoCDRole, TriStateCheckbox> = new Map<GoCDRole, TriStateCheckbox>();

  allRoles.map((role) => {
    const everyUserHasCurrentRole = selectedUsers.every((user) => {
      return user.gocdRoles().includes(role.name());
    });

    const noUserHasCurrentRole = selectedUsers.every((user) => {
      return !user.gocdRoles().includes(role.name());
    });

    let triStateState: TristateState = TristateState.indeterminate;

    if (everyUserHasCurrentRole) {
      triStateState = TristateState.on;
    } else if (noUserHasCurrentRole) {
      triStateState = TristateState.off;
    }

    rolesSelection.set(role as GoCDRole, new TriStateCheckbox(triStateState));
  });
  return rolesSelection;
}

export function computeBulkUpdateRolesJSON(rolesSelection: Map<GoCDRole, TriStateCheckbox>, users: Users) {
  const operations: UserRoleUpdateJSON[] = [];
  rolesSelection.forEach((value, key) => {
    if (value.ischanged() && value.isUnchecked()) {
      operations.push({
                        role: key.name(),
                        users: {
                          remove: users.userNamesOfSelectedUsers()
                        }
                      });
    } else if (value.ischanged() && value.isChecked()) {
      operations.push({
                        role: key.name(),
                        users: {
                          add: users.userNamesOfSelectedUsers()
                        }
                      });
    }
  });
  return {operations};
}
