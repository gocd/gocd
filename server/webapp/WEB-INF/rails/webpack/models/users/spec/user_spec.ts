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

import {UpdateOperationStatus, User, UserJSON} from "models/users/users";

describe("User Model", () => {
  it("should tell if search text is matching with any user information", () => {
    const user = User.fromJSON(userJSON());

    expect(user.matches("some random text")).toBe(false);
    expect(user.matches("aDmiN")).toBe(true);
    expect(user.matches("bOb")).toBe(true);
    expect(user.matches("EXAMPLE")).toBe(true);
    expect(user.matches("BuilDER")).toBe(true);
  });

  it("should return all GoCD roles", () => {
    const user = User.fromJSON(userJSON());
    expect(user.gocdRoles()).toEqual(["admin"]);
  });

  it("should return all plugin roles", () => {
    const user = User.fromJSON(userJSON());
    expect(user.pluginRoles()).toEqual(["plugin role"]);
  });

  it("should mark update operation status as in_progress", () => {
    const user = User.fromJSON(userJSON());

    user.markUpdateInprogress();

    expect(user.updateOperationStatus()).toEqual(UpdateOperationStatus.IN_PROGRESS);
  });

  it("should mark update operation status as success", () => {
    const user = User.fromJSON(userJSON());

    user.markUpdateSuccessful();

    expect(user.updateOperationStatus()).toEqual(UpdateOperationStatus.SUCCESS);
  });

  it("should mark update operation status as error", () => {
    const user = User.fromJSON(userJSON());

    user.markUpdateUnsuccessful();

    expect(user.updateOperationStatus()).toEqual(UpdateOperationStatus.ERROR);
  });

  it("should clear update status", () => {
    const user = User.fromJSON(userJSON());

    user.clearUpdateStatus();

    expect(user.updateOperationStatus()).toEqual(null);
  });

  it("should update user from json", () => {
    const user = User.fromJSON(userJSON());
    const json = {
      login_name: "Administrator",
      is_admin: false,
      is_individual_admin: false,
      enabled: false,
      display_name: "Testing update",
      email: "bob+1@example.com",
      email_me: true,
      roles: [],
      checkin_aliases: []
    };

    user.updateFromJSON(json as UserJSON);

    expect(user.isAdmin()).toEqual(false);
    expect(user.enabled()).toEqual(false);
    expect(user.displayName()).toEqual("Testing update");
    expect(user.email()).toEqual("bob+1@example.com");
    expect(user.emailMe()).toEqual(true);
    expect(user.checkinAliases()).toEqual([]);
    expect(user.roles()).toEqual([]);
  });

  function userJSON(): UserJSON {
    return {
      login_name: "Administrator",
      is_admin: true,
      is_individual_admin: false,
      display_name: "Bob the builder",
      enabled: true,
      email: "Bob@example.com",
      email_me: false,
      checkin_aliases: [
        "alias1@gmail.com",
        "alias2@example.com"
      ],
      roles: [
        {
          name: "admin",
          type: "gocd"
        },
        {
          name: "plugin role",
          type: "plugin"
        }
      ]
    };
  }
});
