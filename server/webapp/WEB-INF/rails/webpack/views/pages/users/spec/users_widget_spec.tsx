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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {UserJSON} from "models/users/users";
import {UsersTableWidget, UsersWidget} from "views/pages/users/users_widget";

describe("UsersWidget", () => {
  let $root: any, root: any;
  const users: Stream<UserJSON[]> = stream();

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  beforeEach(mount);

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function mount() {
    m.mount(root, {
      view() {
        return (<UsersWidget users={users}/>);
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  it("should render a message if list of users is empty", () => {
    users([]);
    m.redraw();
    expect($root.find("table")).not.toBeInDOM();
    expect($root).toContainText("No users found!");
  });

  function bob() {
    return {
      email: "bob@example.com",
      display_name: "Bob",
      login_name: "bob",
      email_me: true,
      checkin_aliases: ["bob@gmail.com"],
      enabled: true
    };
  }

  function alice() {
    return {
      email: "alice@example.com",
      display_name: "Alice",
      login_name: "alice",
      email_me: true,
      checkin_aliases: ["alice@gmail.com", "alice@acme.com"],
      enabled: false
    };
  }

  function john() {
    return {
      display_name: "Jon Doe",
      login_name: "jdoe",
      email_me: true,
      enabled: false,
      checkin_aliases: []
    };
  }

  it("should render a list of user attributes", () => {
    users([bob(), alice()]);
    m.redraw();
    expect($root.find("table")).toBeInDOM();
    expect(UsersTableWidget.headers())
      .toEqual(["Username", "Display name", "Roles", "Aliases", "Admin", "Email", "Enabled"]);

    expect(UsersTableWidget.userData(users())).toHaveLength(2);

    expect(UsersTableWidget.userData(users())[0])
      .toEqual(["bob", "Bob", undefined, "bob@gmail.com", undefined, "bob@example.com", true]);

    expect(UsersTableWidget.userData(users())[1])
      .toEqual(["alice", "Alice", undefined, "alice@gmail.com, alice@acme.com", undefined, "alice@example.com", false]);
  });

  it("should display the number of enabled and disabled users", () => {
    users([bob(), alice(), john()]);
    m.redraw();

    expect(find("enabled-user-count")).toHaveText("1");
    expect(find("disabled-user-count")).toHaveText("2");
  });
});
