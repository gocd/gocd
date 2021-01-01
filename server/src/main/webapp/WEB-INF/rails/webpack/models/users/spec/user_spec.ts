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

import {User, UserJSON} from "models/users/users";

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

  function userJSON(): UserJSON {
    return {
      login_name: "Administrator",
      is_admin: true,
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
