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
import {User, Users} from "models/users/users";
import {FlashMessageModel, MessageType} from "views/components/flash_message";
import {UsersTableWidget, UsersWidget} from "views/pages/users/users_widget";

describe("UsersWidget", () => {
  let $root: any, root: any;
  const users: Stream<Users> = stream(new Users([]));

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  beforeEach(mount);

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  const flashMessageModel = new FlashMessageModel();

  function mount() {
    m.mount(root, {
      view() {
        return (<UsersWidget users={users} message={flashMessageModel}/>);
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
    const allUsers = new Users([bob(), alice()]);
    users(allUsers);
    m.redraw();
    expect($root.find("table")).toBeInDOM();

    expect(UsersTableWidget.headers(allUsers).slice(1, 7))
      .toEqual(["Username", "Display name", "Roles", "Admin", "Email", "Enabled"]);
    expect(UsersTableWidget.userData(users())).toHaveLength(2);
    expect(UsersTableWidget.userData(users())[0].slice(1, 7))
      .toEqual(["bob", "Bob", undefined, "Yes", "bob@example.com", "Yes"]);
    expect(UsersTableWidget.userData(users())[1].slice(1, 7))
      .toEqual(["alice", "Alice", undefined, "No", "alice@example.com", "No"]);
  });

  it("should render flash message if exists", () => {
    const errorMsg = "Boom!";
    flashMessageModel.setMessage(MessageType.alert, errorMsg);
    m.redraw();

    expect(find("flash-message-alert")).toBeInDOM();
    expect(find("flash-message-alert")).toContainText(errorMsg);
  });

});
