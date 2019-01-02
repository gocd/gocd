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

import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {UserFilters} from "models/users/user_filters";
import {User, Users} from "models/users/users";
describe("UsersWidget", () => {
  let $root: any, root: any;
  const users: Stream<Users> = stream(new Users());
  const usersFilter          = stream(new UserFilters());
  let onEnable: (users: Users, e: MouseEvent) => void;
  let onDisable: (users: Users, e: MouseEvent) => void;
  let onDelete: (users: Users, e: MouseEvent) => void;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
    onEnable      = _.noop;
    onDisable     = _.noop;
    onDelete     = _.noop;
  });

  beforeEach(mount);

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function mount() {
    m.mount(root, {
      view() {
        return (<UsersWidget
          onEnable={onEnable}
          onDisable={onDisable}
          onDelete={onDelete}
          users={users}
          userFilter={usersFilter}
        />);
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function bob() {
    return User.fromJSON({
                           email: "bob@example.com",
                           display_name: "Bob",
                           login_name: "bob",
                           is_admin: true,
                           email_me: true,
                           checkin_aliases: ["bob@gmail.com"],
                           enabled: true
                         });
  }

  function alice() {
    return User.fromJSON({
                           email: "alice@example.com",
                           display_name: "Alice",
                           login_name: "alice",
                           is_admin: false,
                           email_me: true,
                           checkin_aliases: ["alice@gmail.com", "alice@acme.com"],
                           enabled: false
                         });
  }

  it("should render a list of user attributes", () => {
    const allUsers = new Users(bob(), alice());
    users(allUsers);
    const userData = UsersTableWidget.userData(users());
    m.redraw();

    const bobUserData = userData[0];
    const aliceUserData = userData[1];

    expect($root.find("table")).toBeInDOM();

    expect(UsersTableWidget.headers(allUsers).slice(1, 7))
      .toEqual(["Username", "Display name", "Roles", "Admin", "Email", "Enabled"]);

    expect(UsersTableWidget.userData(users())).toHaveLength(2);

    expect(bobUserData[1]).toEqual("bob");
    expect(bobUserData[2]).toEqual("Bob");
    expect(bobUserData[4]).toEqual("Yes");
    expect(bobUserData[5]).toEqual("bob@example.com");
    expect(bobUserData[6]).toEqual("Yes");

    expect(aliceUserData[1]).toEqual("alice");
    expect(aliceUserData[2]).toEqual("Alice");
    expect(aliceUserData[4]).toEqual("No");
    expect(aliceUserData[5]).toEqual("alice@example.com");
    expect(aliceUserData[6]).toEqual("No");
  });

});

import {UsersTableWidget, UsersWidget} from "views/pages/users/users_widget";
