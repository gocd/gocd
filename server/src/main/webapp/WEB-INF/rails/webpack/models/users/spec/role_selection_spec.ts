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

import {GoCDRole, Roles} from "models/roles/roles";
import {computeBulkUpdateRolesJSON, computeRolesSelection} from "models/users/role_selection";
import {User, Users} from "models/users/users";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("computeRolesSelection for user role dropdown", () => {

  it("should return roles selection map with state set correctly", () => {
    const roles          = allRoles();
    const rolesSelection = computeRolesSelection(roles, allUsers());

    expect(rolesSelection.size).toEqual(3);
    expect(rolesSelection.get(roles[0] as GoCDRole)!.isChecked()).toEqual(false);
    expect(rolesSelection.get(roles[0] as GoCDRole)!.isIndeterminate()).toEqual(true);

    expect(rolesSelection.get(roles[1] as GoCDRole)!.isChecked()).toEqual(false);
    expect(rolesSelection.get(roles[1] as GoCDRole)!.isIndeterminate()).toEqual(true);

    expect(rolesSelection.get(roles[2] as GoCDRole)!.isChecked()).toEqual(false);
    expect(rolesSelection.get(roles[2] as GoCDRole)!.isIndeterminate()).toEqual(false);
  });

});

describe("computeBulkUpdateRolesJSON for user role dropdown", () => {

  it("should return json required for bulk update of user roles", () => {
    const roles = allRoles();
    const users = allUsers();

    const rolesSelection = computeRolesSelection(roles, users);
    // On state
    rolesSelection.get(roles[0] as GoCDRole)!.click();

    //Off state
    rolesSelection.get(roles[1] as GoCDRole)!.click();
    rolesSelection.get(roles[1] as GoCDRole)!.click();

    const bulkUpdateJSON = computeBulkUpdateRolesJSON(rolesSelection, users);
    expect(bulkUpdateJSON).toEqual({
                                     operations: [
                                       {
                                         role: "gocd-admins",
                                         users: {
                                           add: ["bob", "alice", "joe"]
                                         }
                                       },
                                       {
                                         role: "nobody",
                                         users: {
                                           remove: ["bob", "alice", "joe"]
                                         }
                                       }
                                     ]
                                   });
  });
});

function allUsers() {
  const bob   = new User({
                           login_name: "bob",
                           display_name: "",
                           is_admin: true,
                           roles: [{name: "gocd-admins", type: "gocd"}],
                         });
  const joe   = new User({
                           login_name: "joe",
                           display_name: "",
                           is_admin: true,
                           roles: [{name: "nobody", type: "gocd"}]
                         });
  const alice = new User({
                           login_name: "alice",
                           display_name: "",
                           is_admin: true,
                           roles: [{name: "gocd-admins", type: "gocd"}]
                         });
  const users = new Users(bob, alice, joe);

  users.map((user) => {
    user.checked(true);
  });

  return users;
}

function allRoles() {
  return Roles.fromJSON({
                          _embedded: {
                            roles: [
                              RolesTestData.GoCDRoleJSON("gocd-admins"),
                              RolesTestData.GoCDRoleJSON("nobody"),
                              RolesTestData.GoCDRoleJSON("unused-role")
                            ]
                          }
                        });
}
