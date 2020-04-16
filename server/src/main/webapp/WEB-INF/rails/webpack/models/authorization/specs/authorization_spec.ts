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

import Stream from "mithril/stream";
import {Errors} from "../../mixins/errors";
import {
  Authorization,
  AuthorizationJSON,
  AuthorizedUsersAndRoles,
  PermissionForEntity,
  PermissionsForUsersAndRoles
} from "../authorization";

describe("AuthorizationViewModel", () => {
  it("should initialize authorization roles and users", () => {
    const admins                 = new AuthorizedUsersAndRoles([Stream("user_foo_admin")], [Stream("role_foo_admin")]);
    const view                   = new AuthorizedUsersAndRoles([Stream("user_foo_view")], [Stream("role_foo_view")]);
    const operate                = new AuthorizedUsersAndRoles([Stream("user_foo_operate")],
                                                               [Stream("role_foo_operate")]);
    const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization(view, admins, operate));

    const userWithViewPermission    = new PermissionForEntity("user_foo_view", true, false, false);
    const userWithAdminPermission   = new PermissionForEntity("user_foo_admin", false, false, true);
    const userWithOperatePermission = new PermissionForEntity("user_foo_operate", false, true, false);

    const roleWithViewPermission    = new PermissionForEntity("role_foo_view", true, false, false);
    const roleWithAdminPermission   = new PermissionForEntity("role_foo_admin", false, false, true);
    const roleWithOperatePermission = new PermissionForEntity("role_foo_operate", false, true, false);

    expect(arePermissionForEntitiesEqual(authorizationViewModel.authorizedUsers()[0], userWithViewPermission))
      .toBe(true);
    expect(arePermissionForEntitiesEqual(authorizationViewModel.authorizedUsers()[1], userWithOperatePermission))
      .toBe(true);
    expect(arePermissionForEntitiesEqual(authorizationViewModel.authorizedUsers()[2], userWithAdminPermission))
      .toBe(true);

    expect(arePermissionForEntitiesEqual(authorizationViewModel.authorizedRoles()[0], roleWithViewPermission))
      .toBe(true);
    expect(arePermissionForEntitiesEqual(authorizationViewModel.authorizedRoles()[1], roleWithOperatePermission))
      .toBe(true);
    expect(arePermissionForEntitiesEqual(authorizationViewModel.authorizedRoles()[2], roleWithAdminPermission))
      .toBe(true);
  });

  it("should validate that one of the permission is present", () => {
    const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization());

    authorizationViewModel.addAuthorizedUser(new PermissionForEntity("new-user", false, false, false));
    authorizationViewModel.addAuthorizedRole(new PermissionForEntity("new-role", false, false, false));

    expect(authorizationViewModel.isValid()).toEqual(false);
    expect(authorizationViewModel.authorizedUsers()[0].errors().errors("name"))
      .toEqual(["At least one permission should be enabled."]);
    expect(authorizationViewModel.authorizedRoles()[0].errors().errors("name"))
      .toEqual(["At least one permission should be enabled."]);
  });

  describe("User", () => {
    it("should add authorized user", () => {
      const admins                 = new AuthorizedUsersAndRoles([Stream("user_foo_admin")],
                                                                 [Stream("role_foo_admin")]);
      const view                   = new AuthorizedUsersAndRoles([Stream("user_foo_view")], [Stream("role_foo_view")]);
      const operate                = new AuthorizedUsersAndRoles([Stream("user_foo_operate")],
                                                                 [Stream("role_foo_operate")]);
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization(view, admins, operate));

      const newUser = new PermissionForEntity("newUser", false, false, true);
      authorizationViewModel.addAuthorizedUser(newUser);

      expect(authorizationViewModel.authorizedUsers()[3]).toBe(newUser);

    });

    it("should remove authorized user", () => {
      const admins                 = new AuthorizedUsersAndRoles([Stream("user_foo_admin")],
                                                                 [Stream("role_foo_admin")]);
      const view                   = new AuthorizedUsersAndRoles([Stream("user_foo_view")], [Stream("role_foo_view")]);
      const operate                = new AuthorizedUsersAndRoles([Stream("user_foo_operate")],
                                                                 [Stream("role_foo_operate")]);
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization(view, admins, operate));

      const removedUser = authorizationViewModel.authorizedUsers()[1];

      expect(authorizationViewModel.authorizedUsers().includes(removedUser)).toBe(true);

      authorizationViewModel.removeUser(removedUser);

      expect(authorizationViewModel.authorizedUsers().includes(removedUser)).toBe(false);
      expect(authorizationViewModel.authorizedUsers().length).toBe(2);
    });

    it("should set true for view and operate if admin privilege is present", () => {
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization());

      authorizationViewModel.addAuthorizedUser(new PermissionForEntity("new-user", false, false, true));

      expect(authorizationViewModel.authorizedUsers()[0].view()).toBeTruthy();
      expect(authorizationViewModel.authorizedUsers()[0].operate()).toBeTruthy();
      expect(authorizationViewModel.authorizedUsers()[0].admin()).toBeTruthy();
    });

    it("should set true for view if operate privilege is present", () => {
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization());

      authorizationViewModel.addAuthorizedUser(new PermissionForEntity("new-user", false, true, false));

      expect(authorizationViewModel.authorizedUsers()[0].view()).toBeTruthy();
      expect(authorizationViewModel.authorizedUsers()[0].operate()).toBeTruthy();
      expect(authorizationViewModel.authorizedUsers()[0].admin()).toBeFalsy();
    });
  });

  describe("Role", () => {
    it("should add authorized role", () => {
      const admins                 = new AuthorizedUsersAndRoles([Stream("user_foo_admin")],
                                                                 [Stream("role_foo_admin")]);
      const view                   = new AuthorizedUsersAndRoles([Stream("user_foo_view")], [Stream("role_foo_view")]);
      const operate                = new AuthorizedUsersAndRoles([Stream("user_foo_operate")],
                                                                 [Stream("role_foo_operate")]);
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization(view, admins, operate));

      const newRole = new PermissionForEntity("newRole", false, false, true);
      authorizationViewModel.addAuthorizedRole(newRole);

      expect(authorizationViewModel.authorizedRoles()[3]).toBe(newRole);
    });

    it("should remove authorized role", () => {
      const admins                 = new AuthorizedUsersAndRoles([Stream("user_foo_admin")],
                                                                 [Stream("role_foo_admin")]);
      const view                   = new AuthorizedUsersAndRoles([Stream("user_foo_view")], [Stream("role_foo_view")]);
      const operate                = new AuthorizedUsersAndRoles([Stream("user_foo_operate")],
                                                                 [Stream("role_foo_operate")]);
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization(view, admins, operate));

      const removedRole = authorizationViewModel.authorizedRoles()[1];

      authorizationViewModel.removeRole(removedRole);

      expect(authorizationViewModel.authorizedRoles().includes(removedRole)).toBe(false);
      expect(authorizationViewModel.authorizedRoles().length).toBe(2);
    });

    it("should set true for view and operate if admin privilege is present", () => {
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization());

      authorizationViewModel.addAuthorizedRole(new PermissionForEntity("new-role", false, false, true));

      expect(authorizationViewModel.authorizedRoles()[0].view()).toBeTruthy();
      expect(authorizationViewModel.authorizedRoles()[0].operate()).toBeTruthy();
      expect(authorizationViewModel.authorizedRoles()[0].admin()).toBeTruthy();
    });

    it("should set true for view if operate privilege is present", () => {
      const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization());

      authorizationViewModel.addAuthorizedRole(new PermissionForEntity("new-role", false, true, false));

      expect(authorizationViewModel.authorizedRoles()[0].view()).toBeTruthy();
      expect(authorizationViewModel.authorizedRoles()[0].operate()).toBeTruthy();
      expect(authorizationViewModel.authorizedRoles()[0].admin()).toBeFalsy();
    });
  });

  it("should initialize authorization with errors", () => {
    const errors = new Errors();
    errors.add("roles", "Some error msg");
    const admins                 = new AuthorizedUsersAndRoles([Stream("user_foo_admin")],
                                                               [Stream("role_foo_admin")],
                                                               errors);
    const view                   = new AuthorizedUsersAndRoles([Stream("user_foo_view")], [Stream("role_foo_view")]);
    const operate                = new AuthorizedUsersAndRoles([Stream("user_foo_operate")],
                                                               [Stream("role_foo_operate")]);
    const authorizationViewModel = new PermissionsForUsersAndRoles(new Authorization(view, admins, operate));

    expect(authorizationViewModel.errors().hasErrors()).toBeTruthy();
    expect(authorizationViewModel.errors().errorsForDisplay("roles")).toBe("Some error msg.");
  });
});

describe("AuthorizedUsersAndRoles", () => {

  it("should deserialize JSON", () => {
    const usersAndRoles                                    = {users: ["foo_user"], roles: ["foo_role"]};
    const authorizedUsersAndRoles: AuthorizedUsersAndRoles = AuthorizedUsersAndRoles.fromJSON(usersAndRoles);

    expect(authorizedUsersAndRoles.users()[0]).toEqual("foo_user");
    expect(authorizedUsersAndRoles.roles()[0]).toEqual("foo_role");

  });

  it("should serialize JSON", () => {
    const usersAndRoles = new AuthorizedUsersAndRoles([Stream("foo_user")], [Stream("foo_role")]);

    const actualJSON = usersAndRoles.toJSON();

    const expectedJSON = {users: ["foo_user"], roles: ["foo_role"]};
    expect(actualJSON).toEqual(expectedJSON);
  });

  it("should not serialize empty values to JSON", () => {
    const usersAndRoles = new AuthorizedUsersAndRoles(
      [
        Stream("foo_user"), Stream("")
      ],
      [
        Stream("foo_role"), Stream("")
      ]);

    const actualJSON = usersAndRoles.toJSON();

    const expectedJSON = {users: ["foo_user"], roles: ["foo_role"]};
    expect(actualJSON).toEqual(expectedJSON);
  });

  describe("isEmpty", () => {
    it("should return true if both users and roles are empty", () => {
      const authorizedUsersAndRoles = new AuthorizedUsersAndRoles([], []);
      expect(authorizedUsersAndRoles.isEmpty()).toBe(true);
    });

    it("should return false if only users empty", () => {
      const authorizedUsersAndRoles = new AuthorizedUsersAndRoles([], [Stream("role1")]);
      expect(authorizedUsersAndRoles.isEmpty()).toBe(false);
    });

  });
});

describe("Authorization", () => {
  describe("deserialize JSON", () => {
    it("when all data is present", () => {
      const viewJSON    = {users: ["user1"], roles: ["foo"]};
      const operateJSON = {users: ["user2"], roles: ["bar"]};
      const adminsJSON  = {users: ["user3"], roles: ["bazz"]};

      const authorizationJSON: AuthorizationJSON = {
        view: viewJSON,
        admins: adminsJSON,
        operate: operateJSON
      };

      const authorization: Authorization = Authorization.fromJSON(authorizationJSON);

      expect(authorization.view().roles()).toEqual(["foo"]);
      expect(authorization.admin().roles()).toEqual(["bazz"]);
      expect(authorization.operate().roles()).toEqual(["bar"]);

      expect(authorization.view().users()).toEqual(["user1"]);
      expect(authorization.admin().users()).toEqual(["user3"]);
      expect(authorization.operate().users()).toEqual(["user2"]);
    });

    it("should deserialize JSON if no data is present", () => {
      const authorization     = new Authorization();
      const authorizationJSON = authorization.toJSON();

      expect(authorizationJSON).toEqual({});
    });

    it("when partial data is present", () => {
      const viewJSON    = {users: [], roles: ["foo"]};
      const operateJSON = {users: ["user2"], roles: []};

      const authorizationJSON: AuthorizationJSON = {
        view: viewJSON,
        operate: operateJSON
      };
      const authorization: Authorization         = Authorization.fromJSON(authorizationJSON);

      expect(authorization.view().roles()).toEqual(["foo"]);
      expect(authorization.admin().roles()).toEqual([]);
      expect(authorization.operate().roles()).toEqual([]);

      expect(authorization.view().users()).toEqual([]);
      expect(authorization.admin().users()).toEqual([]);
      expect(authorization.operate().users()).toEqual(["user2"]);
    });

    it("with errors", () => {
      const viewJSON    = {users: [], roles: ["foo"], errors: {roles: ["some error msg"]}};
      const operateJSON = {users: ["user2"], roles: []};

      const authorizationJSON: AuthorizationJSON = {
        view: viewJSON,
        operate: operateJSON
      };
      const authorization: Authorization         = Authorization.fromJSON(authorizationJSON);

      expect(authorization.view().errors().hasErrors()).toBeTruthy();
      expect(authorization.view().errors().errorsForDisplay("roles")).toBe("some error msg.");
    });
  });

  it("should serialize JSON", () => {
    const viewAccess    = new AuthorizedUsersAndRoles([Stream("view_user")], [Stream("view_role")]);
    const adminAccess   = new AuthorizedUsersAndRoles([Stream("admin_user")], [Stream("admin_role")]);
    const operateAccess = new AuthorizedUsersAndRoles([Stream("operate_user")], [Stream("operate_role")]);

    const authorization     = new Authorization(viewAccess, adminAccess, operateAccess);
    const authorizationJSON = authorization.toJSON();

    const expectedJSON: AuthorizationJSON = {
      view: {
        users: ["view_user"],
        roles: ["view_role"]
      },
      operate: {
        users: ["operate_user"],
        roles: ["operate_role"]
      },
      admins: {
        users: ["admin_user"],
        roles: ["admin_role"]
      }
    };

    expect(authorizationJSON).toEqual(expectedJSON);
  });

  it("should serialize to empty JSON when permissions are inherited", () => {
    const viewAccess = new AuthorizedUsersAndRoles([Stream("view_user")], [Stream("view_role")]);

    const expectedJSON = {
      users: ["view_user"],
      roles: ["view_role"]
    };

    const emptyJSON = {
      users: ["view_user"],
      roles: ["view_role"]
    };

    expect(expectedJSON).toEqual(viewAccess.toJSON());
    viewAccess.isInherited(false);
    expect(emptyJSON).toEqual(viewAccess.toJSON());
  });

});

function arePermissionForEntitiesEqual(entity: PermissionForEntity, otherEntity: PermissionForEntity) {
  return entity.name() === otherEntity.name()
    && entity.admin() === otherEntity.admin()
    && entity.operate() === otherEntity.operate()
    && entity.view() === otherEntity.view();
}
