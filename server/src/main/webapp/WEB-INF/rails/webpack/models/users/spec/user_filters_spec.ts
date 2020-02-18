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

import {usersJSON} from "models/users/spec/fixtures";
import {UserFilters} from "models/users/user_filters";
import {Users} from "models/users/users";

describe("UsersFilter", () => {

  describe("Filters", () => {
    it("should turn off all the filters by default", () => {
      const userFilters = new UserFilters();
      expect(userFilters.anyFiltersApplied()).toBe(false);

      expect(userFilters.superAdmins()).toBe(false);
      expect(userFilters.normalUsers()).toBe(false);
      expect(userFilters.enabledUsers()).toBe(false);
      expect(userFilters.disabledUsers()).toBe(false);
      expect(userFilters.selectedRoles().length).toBe(0);
    });

    it("should reset all filters", () => {
      const userFilters = new UserFilters();
      expect(userFilters.anyFiltersApplied()).toBe(false);
      expect(userFilters.anyFiltersApplied()).toBe(false);

      userFilters.superAdmins(true);

      expect(userFilters.anyFiltersApplied()).toBe(true);

      userFilters.resetFilters();

      expect(userFilters.anyFiltersApplied()).toBe(false);
      expect(userFilters.superAdmins()).toBe(false);
      expect(userFilters.normalUsers()).toBe(false);
      expect(userFilters.enabledUsers()).toBe(false);
      expect(userFilters.disabledUsers()).toBe(false);
    });

    describe("Privileges Section", () => {
      it("should return true if any privileges filter is applied", () => {
        const userFilters = new UserFilters();

        expect(userFilters.isAnyPrivilegesFilterApplied()).toBe(false);

        userFilters.superAdmins(true);
        expect(userFilters.isAnyPrivilegesFilterApplied()).toBe(true);
      });

      it("should filter users based by admin privileges", () => {
        const users = Users.fromJSON(usersJSON());

        const userFilters = new UserFilters();
        userFilters.superAdmins(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(5);
        expect(filteredUsers[0].displayName()).toBe("root");
        expect(filteredUsers[1].displayName()).toBe("jez");
        expect(filteredUsers[2].displayName()).toBe("jigsaw");
        expect(filteredUsers[3].displayName()).toBe("admin");
        expect(filteredUsers[4].displayName()).toBe("cruise_admin");
      });

      it("should filter users based by normal user privileges", () => {
        const users = Users.fromJSON(usersJSON());

        const userFilters = new UserFilters();
        userFilters.normalUsers(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(2);
        expect(filteredUsers[0].displayName()).toBe("operate");
        expect(filteredUsers[1].displayName()).toBe("view");
      });

      it("should apply multiple filters", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();

        userFilters.superAdmins(true);
        userFilters.normalUsers(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(7);
      });

      it("should apply multiple filters along with search query", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();

        userFilters.superAdmins(true);
        userFilters.searchText("admin");

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers).toHaveLength(2);
        expect(filteredUsers[0].displayName()).toBe("admin");
        expect(filteredUsers[1].displayName()).toBe("cruise_admin");
      });
    });

    describe("User state section", () => {

      it("should return true if any state filter is applied", () => {
        const userFilters = new UserFilters();

        expect(userFilters.isAnyUserStateFilterApplied()).toBe(false);

        userFilters.enabledUsers(true);
        expect(userFilters.isAnyUserStateFilterApplied()).toBe(true);
      });

      it("should filter enabled users", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();

        userFilters.enabledUsers(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(5);
        expect(filteredUsers[0].displayName()).toBe("jez");
        expect(filteredUsers[1].displayName()).toBe("admin");
        expect(filteredUsers[2].displayName()).toBe("cruise_admin");
        expect(filteredUsers[3].displayName()).toBe("operate");
        expect(filteredUsers[4].displayName()).toBe("view");
      });

      it("should filter disabled users", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();

        userFilters.disabledUsers(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(2);
        expect(filteredUsers[0].displayName()).toBe("root");
        expect(filteredUsers[1].displayName()).toBe("jigsaw");
      });

      it("should apply multiple filters", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();
        userFilters.enabledUsers(true);
        userFilters.disabledUsers(true);
        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(7);
      });

      it("should apply multiple filters along with search query", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();
        userFilters.enabledUsers(true);
        userFilters.searchText("admin");
        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers).toHaveLength(2);
        expect(filteredUsers[0].displayName()).toBe("admin");
        expect(filteredUsers[1].displayName()).toBe("cruise_admin");
      });
    });

    describe("Role section", () => {
      it("should answer if any roles are selected", () => {
        // initial state
        const filters = new UserFilters();
        expect(filters.isAnyRoleFilterApplied()).toBe(false);
        expect(filters.selectedRoles()).toEqual([]);

        // initialize a selection
        const selection = filters.roleSelectionFor("foo");
        expect(selection()).toBe(false);
        expect(filters.selectedRoles()).toEqual([]);
        expect(filters.isAnyRoleFilterApplied()).toBe(false);

        // set a selection
        selection(true);
        expect(filters.selectedRoles()).toEqual(["foo"]);
        expect(filters.isAnyRoleFilterApplied()).toBe(true);
      });

      it("should filter users based by roles", () => {
        const users = Users.fromJSON(usersJSON());

        const userFilters = new UserFilters();
        const selectedRole = userFilters.roleSelectionFor("gocd-admin");
        selectedRole(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(2);
        expect(filteredUsers[0].displayName()).toBe("root");
        expect(filteredUsers[1].displayName()).toBe("admin");
      });

      it("should apply multiple filters along with search query", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();

        const selectedRole = userFilters.roleSelectionFor("gocd-admin");
        selectedRole(true);
        userFilters.searchText("some random");

        let filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers).toHaveLength(0);

        userFilters.searchText("admin");
        filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers).toHaveLength(1);
        expect(filteredUsers[0].displayName()).toBe("admin");
      });

    });

    describe("Privileges, User State and role section", () => {
      it("should select all the enabled admin users", () => {
        const users = Users.fromJSON(usersJSON());

        const userFilters = new UserFilters();
        userFilters.superAdmins(true);
        userFilters.enabledUsers(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(3);
        expect(filteredUsers[0].displayName()).toBe("jez");
        expect(filteredUsers[1].displayName()).toBe("admin");
        expect(filteredUsers[2].displayName()).toBe("cruise_admin");
      });

      it("should select all the disabled normal users", () => {
        const users       = Users.fromJSON(usersJSON());
        const userFilters = new UserFilters();
        userFilters.normalUsers(true);
        userFilters.disabledUsers(true);
        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(0);
      });

      it("should select all the enabled admin users with gocd role", () => {
        const users = Users.fromJSON(usersJSON());

        const userFilters = new UserFilters();
        userFilters.superAdmins(true);
        userFilters.enabledUsers(true);
        const selectedRole = userFilters.roleSelectionFor("gocd-admin");
        selectedRole(true);

        const filteredUsers = userFilters.performFilteringOn(users);

        expect(filteredUsers.totalUsersCount()).toBe(1);
        expect(filteredUsers[0].displayName()).toBe("admin");
      });
    });
  });

  describe("Search", () => {
    it("should return all users when no search query is provided", () => {
      const users         = Users.fromJSON(usersJSON());
      const userFilters   = new UserFilters();
      const filteredUsers = userFilters.performFilteringOn(users);
      expect(filteredUsers.totalUsersCount()).toBe(7);
      expect(userFilters.searchText()).toBe("");
    });

    it("should return searched users when search query is provided", () => {
      const users = Users.fromJSON(usersJSON());

      const userFilters = new UserFilters();
      userFilters.searchText("admin");

      const filteredUsers = userFilters.performFilteringOn(users);

      expect(filteredUsers).toHaveLength(2);
      expect(filteredUsers[0].displayName()).toBe("admin");
      expect(filteredUsers[1].displayName()).toBe("cruise_admin");
    });

    it("should return no users when search query is provided which doesnt match any user", () => {
      const users = Users.fromJSON(usersJSON());

      const userFilters = new UserFilters();
      userFilters.searchText("some-stupid-query-which-doesn't-match-any-user");

      const filteredUsers = userFilters.performFilteringOn(users);

      expect(filteredUsers).toHaveLength(0);
    });
  });

});
