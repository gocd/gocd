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

import {Users, UsersJSON} from "models/users/users";

describe("Users Model", () => {
  it("should deserialize from JSON", () => {
    const users = Users.fromJSON(usersJSON());

    expect(users.list()).toHaveLength(7);
  });

  it("should deserialize User JSON", () => {
    const users = Users.fromJSON(usersJSON());

    expect(users.list()).toHaveLength(7);

    const rootUser     = users.list()[0];
    const rootUserJSON = usersJSON()._embedded.users[0];

    expect(rootUser.loginName).toEqual(rootUserJSON.login_name);
    expect(rootUser.displayName).toEqual(rootUserJSON.display_name);
    expect(rootUser.enabled).toEqual(rootUserJSON.enabled);
    expect(rootUser.email).toEqual(rootUserJSON.email);
    expect(rootUser.emailMe).toEqual(rootUserJSON.email_me);
    expect(rootUser.checkinAliases).toEqual(rootUserJSON.checkin_aliases);
  });

  it("should return total users count", () => {
    const users = Users.fromJSON(usersJSON());
    expect(users.totalUsersCount()).toBe(7);
  });

  it("should return enabled users count", () => {
    const users = Users.fromJSON(usersJSON());
    expect(users.enabledUsersCount()).toBe(5);
  });

  it("should return disabled users count", () => {
    const users = Users.fromJSON(usersJSON());
    expect(users.disabledUsersCount()).toBe(2);
  });

  describe("Users Selection", () => {
    it("should tell if all users are selected", () => {
      const users = Users.fromJSON(usersJSON());
      expect(users.areAllUsersSelected()).toBe(false);
    });

    it("should toggle all users selection", () => {
      const users = Users.fromJSON(usersJSON());
      expect(users.areAllUsersSelected()).toBe(false);
      users.toggleSelection();
      expect(users.areAllUsersSelected()).toBe(true);
    });
  });

  describe("Search", () => {
    it("should return all users when no search query is provided", () => {
      const users = Users.fromJSON(usersJSON());
      expect(users.totalUsersCount()).toBe(7);
      expect(users.search()).toBe("");
    });

    it("should return searched users when search query is provided", () => {
      const users = Users.fromJSON(usersJSON());

      expect(users.totalUsersCount()).toBe(7);
      expect(users.search()).toBe("");

      const searchQuery = "admin";
      users.search(searchQuery);

      expect(users.list()).toHaveLength(2);
      expect(users.list()[0].displayName).toBe("admin");
      expect(users.list()[1].displayName).toBe("cruise_admin");

      expect(users.search()).toBe(searchQuery);
    });

    it("should return no users when search query is provided which doesnt match any user", () => {
      const users = Users.fromJSON(usersJSON());

      expect(users.totalUsersCount()).toBe(7);
      expect(users.search()).toBe("");

      const searchQuery = "some-stupid-query-which-doesn't-match-any-user";
      users.search(searchQuery);

      expect(users.list()).toHaveLength(0);
      expect(users.search()).toBe(searchQuery);
    });
  });

  describe("Filters", () => {
    it("should turn off all the filters by default", () => {
      const users = Users.fromJSON(usersJSON());
      expect(users.filters.anyFiltersApplied()).toBe(false);

      expect(users.filters.superAdmins()).toBe(false);
      expect(users.filters.normalUsers()).toBe(false);
      expect(users.filters.enabledUsers()).toBe(false);
      expect(users.filters.disabledUsers()).toBe(false);
    });

    it("should reset all filters", () => {
      const users = Users.fromJSON(usersJSON());
      expect(users.filters.anyFiltersApplied()).toBe(false);

      users.filters.superAdmins(true);

      expect(users.filters.anyFiltersApplied()).toBe(true);

      users.filters.resetFilters();

      expect(users.filters.anyFiltersApplied()).toBe(false);
      expect(users.filters.superAdmins()).toBe(false);
      expect(users.filters.normalUsers()).toBe(false);
      expect(users.filters.enabledUsers()).toBe(false);
      expect(users.filters.disabledUsers()).toBe(false);
    });

    describe("Privileges Section", () => {
      it("should filter users based by admin privileges", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);
        users.filters.superAdmins(true);

        expect(users.totalUsersCount()).toBe(5);
        expect(users.list()[0].displayName).toBe("root");
        expect(users.list()[1].displayName).toBe("jez");
        expect(users.list()[2].displayName).toBe("jigsaw");
        expect(users.list()[3].displayName).toBe("admin");
        expect(users.list()[4].displayName).toBe("cruise_admin");
      });

      it("should filter users based by normal user privileges", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);
        users.filters.normalUsers(true);
        expect(users.totalUsersCount()).toBe(2);
        expect(users.list()[0].displayName).toBe("operate");
        expect(users.list()[1].displayName).toBe("view");
      });

      it("should apply multiple filters", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);
        users.filters.superAdmins(true);
        users.filters.normalUsers(true);

        expect(users.totalUsersCount()).toBe(7);
      });

      it("should apply multiple filters along with search query", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);

        users.filters.superAdmins(true);
        expect(users.totalUsersCount()).toBe(5);

        expect(users.list()[0].displayName).toBe("root");
        expect(users.list()[1].displayName).toBe("jez");
        expect(users.list()[2].displayName).toBe("jigsaw");
        expect(users.list()[3].displayName).toBe("admin");
        expect(users.list()[4].displayName).toBe("cruise_admin");

        const searchQuery = "admin";
        users.search(searchQuery);

        expect(users.list()).toHaveLength(2);
        expect(users.list()[0].displayName).toBe("admin");
        expect(users.list()[1].displayName).toBe("cruise_admin");

        expect(users.search()).toBe(searchQuery);
      });
    });

    describe("User state section", () => {
      it("should filter enabled users", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);

        users.filters.enabledUsers(true);

        expect(users.totalUsersCount()).toBe(5);
        expect(users.list()[0].displayName).toBe("jez");
        expect(users.list()[1].displayName).toBe("admin");
        expect(users.list()[2].displayName).toBe("cruise_admin");
        expect(users.list()[3].displayName).toBe("operate");
        expect(users.list()[4].displayName).toBe("view");
      });

      it("should filter disabled users", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);

        users.filters.disabledUsers(true);

        expect(users.totalUsersCount()).toBe(2);
        expect(users.list()[0].displayName).toBe("root");
        expect(users.list()[1].displayName).toBe("jigsaw");
      });

      it("should apply multiple filters", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);
        users.filters.enabledUsers(true);
        users.filters.disabledUsers(true);

        expect(users.totalUsersCount()).toBe(7);
      });

      it("should apply multiple filters along with search query", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);

        users.filters.enabledUsers(true);
        expect(users.totalUsersCount()).toBe(5);

        expect(users.list()[0].displayName).toBe("jez");
        expect(users.list()[1].displayName).toBe("admin");
        expect(users.list()[2].displayName).toBe("cruise_admin");
        expect(users.list()[3].displayName).toBe("operate");
        expect(users.list()[4].displayName).toBe("view");

        const searchQuery = "admin";
        users.search(searchQuery);

        expect(users.list()).toHaveLength(2);
        expect(users.list()[0].displayName).toBe("admin");
        expect(users.list()[1].displayName).toBe("cruise_admin");

        expect(users.search()).toBe(searchQuery);
      });
    });

    describe("Privileges and User State Section", () => {
      it("should select all the enabled admin users", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);

        users.filters.superAdmins(true);
        users.filters.enabledUsers(true);

        expect(users.totalUsersCount()).toBe(3);
        expect(users.list()[0].displayName).toBe("jez");
        expect(users.list()[1].displayName).toBe("admin");
        expect(users.list()[2].displayName).toBe("cruise_admin");
      });

      it("should select all the disabled normal users", () => {
        const users = Users.fromJSON(usersJSON());

        expect(users.totalUsersCount()).toBe(7);

        users.filters.normalUsers(true);
        users.filters.disabledUsers(true);

        expect(users.totalUsersCount()).toBe(0);
      });
    });
  });

  function usersJSON(): UsersJSON {
    return {
      _embedded: {
        users: [
          {
            login_name: "root",
            is_admin: true,
            display_name: "root",
            enabled: false,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "jez",
            is_admin: true,
            display_name: "jez",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "jigsaw",
            is_admin: true,
            display_name: "jigsaw",
            enabled: false,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "admin",
            is_admin: true,
            display_name: "admin",
            enabled: true,
            email: "admin@example.com",
            email_me: false,
            checkin_aliases: [
              "alias1@gmail.com",
              "alias2@example.com"
            ]
          },
          {
            login_name: "cruise_admin",
            is_admin: true,
            display_name: "cruise_admin",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "operate",
            is_admin: false,
            display_name: "operate",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "view",
            is_admin: false,
            display_name: "view",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          }
        ]
      }
    };
  }
});
