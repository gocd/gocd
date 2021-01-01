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
import {usersJSON} from "models/users/spec/fixtures";
import {Users} from "models/users/users";

describe("Users Model", () => {
  it("should deserialize from JSON", () => {
    const users = Users.fromJSON(usersJSON());

    expect(users).toHaveLength(7);
  });

  it("should deserialize User JSON", () => {
    const users = Users.fromJSON(usersJSON());

    expect(users).toHaveLength(7);

    const rootUser     = users[0];
    const rootUserJSON = usersJSON()._embedded.users[0];

    expect(rootUser.loginName()).toEqual(rootUserJSON.login_name);
    expect(rootUser.displayName()).toEqual(rootUserJSON.display_name as string);
    expect(rootUser.enabled()).toEqual(rootUserJSON.enabled as boolean);
    expect(rootUser.email()).toEqual(rootUserJSON.email as string);
    expect(rootUser.emailMe()).toEqual(rootUserJSON.email_me as boolean);
    expect(rootUser.checkinAliases()).toEqual(rootUserJSON.checkin_aliases as string[]);
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

  it("should return all users sorted by usernames", () => {
    const users       = Users.fromJSON(usersJSON());
    const sortedUsers = users.sortedByUsername();
    expect(sortedUsers[0].loginName()).toBe("admin");
    expect(sortedUsers[1].loginName()).toBe("cruise_admin");
    expect(sortedUsers[2].loginName()).toBe("jez");
    expect(sortedUsers[3].loginName()).toBe("jigsaw");
    expect(sortedUsers[4].loginName()).toBe("operate");
    expect(sortedUsers[5].loginName()).toBe("root");
    expect(sortedUsers[6].loginName()).toBe("view");
  });

  it("should remove user by username", () => {
    const users = Users.fromJSON(usersJSON());
    const user  = Users.fromJSON(usersJSON())[2];
    user.displayName("Testing replace");

    const originalUser = users[2];
    users.replace(user);
    // not the same object
    expect(originalUser).not.toEqual(users[2]);
    expect(user).toEqual(users[2]);
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

    it("should return all selected users", () => {
      const users = Users.fromJSON(usersJSON());

      expect(users.selectedUsers().length).toBe(0);
      users[0].checked(true);

      expect(users.selectedUsers().length).toBe(1);
      expect(users.selectedUsers()[0].loginName()).toBe("root");
    });

    it("should return usernames of all selected users", () => {
      const users = Users.fromJSON(usersJSON());

      expect(users.userNamesOfSelectedUsers().length).toBe(0);
      users[0].checked(true);

      expect(users.userNamesOfSelectedUsers()).toEqual(["root"]);
    });

    it("should tell if any user is selected", () => {
      const users = Users.fromJSON(usersJSON());

      expect(users.anyUserSelected()).toBe(false);
      users[0].checked(true);

      expect(users.anyUserSelected()).toBe(true);
    });
  });

});
