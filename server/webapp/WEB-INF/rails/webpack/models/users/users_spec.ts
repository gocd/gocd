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

import {User, Users, UsersJSON} from "models/users/users";

describe("Users Model", () => {
  it("should deserialize from JSON", () => {
    const users = Users.fromJSON(usersJSON());

    expect(users.list()).toHaveLength(7);
    expect(users.list()[0]).toEqual(User.fromJSON(usersJSON()._embedded.users[0]));
    expect(users.list()[1]).toEqual(User.fromJSON(usersJSON()._embedded.users[1]));
    expect(users.list()[2]).toEqual(User.fromJSON(usersJSON()._embedded.users[2]));
    expect(users.list()[3]).toEqual(User.fromJSON(usersJSON()._embedded.users[3]));
    expect(users.list()[4]).toEqual(User.fromJSON(usersJSON()._embedded.users[4]));
    expect(users.list()[5]).toEqual(User.fromJSON(usersJSON()._embedded.users[5]));
    expect(users.list()[6]).toEqual(User.fromJSON(usersJSON()._embedded.users[6]));
  });

  it("should deserialize User JSON", () => {
    const users = Users.fromJSON(usersJSON());

    expect(users.list()).toHaveLength(7);
    expect(users.list()[0]).toEqual(User.fromJSON(usersJSON()._embedded.users[0]));

    const rootUser     = users.list()[0];
    const rootUserJSON = usersJSON()._embedded.users[0];

    expect(rootUser.loginName).toEqual(rootUserJSON.login_name);
    expect(rootUser.displayName).toEqual(rootUserJSON.display_name);
    expect(rootUser.enabled).toEqual(rootUserJSON.enabled);
    expect(rootUser.email).toEqual(rootUserJSON.email);
    expect(rootUser.emailMe).toEqual(rootUserJSON.email_me);
    expect(rootUser.checkinAliases).toEqual(rootUserJSON.checkin_aliases);
  });

  it("should return enabled users count", () => {
    const users = Users.fromJSON(usersJSON());
    expect(users.enabledUsersCount()).toBe(5);
  });

  it("should return disabled users count", () => {
    const users = Users.fromJSON(usersJSON());

    expect(users.disabledUsersCount()).toBe(2);
  });

  function usersJSON(): UsersJSON {
    return {
      _embedded: {
        users: [
          {
            login_name: "root",
            display_name: "root",
            enabled: false,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "jez",
            display_name: "jez",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "jigsaw",
            display_name: "jigsaw",
            enabled: false,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "admin",
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
            display_name: "cruise_admin",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "operate",
            display_name: "operate",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          },
          {
            login_name: "view",
            display_name: "view",
            enabled: true,
            email: undefined,
            email_me: false,
            checkin_aliases: []
          }
        ]
      }
    }
      ;
  }
});
