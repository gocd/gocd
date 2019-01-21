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
import {GoCDRole, Roles} from "models/roles/roles_new";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import {UserFilters} from "models/users/user_filters";
import {User, Users} from "models/users/users";
import {State as UserActionsState} from "views/pages/users/user_actions_widget";

describe("UsersWidget", () => {
  let $root: any, root: any;

  let attrs: UserActionsState;

  beforeEach(() => {
    attrs         = {
      users: stream(new Users()),
      roles: stream(new Roles()),
      userFilters: stream(new UserFilters()),
      initializeRolesDropdownAttrs: _.noop,
      showRoles: stream(false),
      showFilters: stream(false),
      rolesSelection: stream(new Map<GoCDRole, TriStateCheckbox>()),
      onEnable: _.noop,
      onDisable: _.noop,
      onDelete: _.noop,
      onRolesUpdate: _.noop,
      roleNameToAdd: stream(),
      onRolesAdd: _.noop,
      onMakeAdmin: _.noop,
      onRemoveAdmin: _.noop
    };
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
        return (<UsersWidget {...attrs}/>);
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
    const userData = UsersTableWidget.userData(allUsers);
    m.redraw();

    const bobUserData   = userData[0];
    const aliceUserData = userData[1];
    const headers       = UsersTableWidget.headers(allUsers);

    expect($root.find("table")).toBeInDOM();

    expect(headers[1]).toEqual("Username");
    expect(headers[2]).toEqual("Display name");
    expect(headers[4]).toEqual("System Admin");
    expect(headers[5]).toEqual("Email");
    expect(headers[6]).toEqual("Enabled");

    expect(UsersTableWidget.userData(allUsers)).toHaveLength(2);
    expect(bobUserData[1].text).toEqual("bob");
    expect(bobUserData[2].text).toEqual("Bob");
    expect(bobUserData[4].text).toEqual("Yes");
    expect(bobUserData[5].text).toEqual("bob@example.com");
    expect(bobUserData[6].text).toEqual("Yes");
    expect(bobUserData[1].attrs.className).not.toContain("disabled");

    expect(aliceUserData[1].text).toEqual("alice");
    expect(aliceUserData[2].text).toEqual("Alice");
    expect(aliceUserData[4].text).toEqual("No");
    expect(aliceUserData[5].text).toEqual("alice@example.com");
    expect(aliceUserData[6].text).toEqual("No");
    expect(aliceUserData[1].attrs.className).toContain("disabled");
  });

});

import {UsersTableWidget, UsersWidget} from "views/pages/users/users_widget";
