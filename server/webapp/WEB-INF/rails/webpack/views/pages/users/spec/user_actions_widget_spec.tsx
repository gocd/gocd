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
import {GoCDRole, Roles} from "models/roles/roles_new";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import {UserFilters} from "models/users/user_filters";
import {User, Users} from "models/users/users";
import {State as UserActionsState, UsersActionsWidget} from "views/pages/users/user_actions_widget";

describe("User Actions Widget", () => {
  let $root: any, root: any;
  let attrs: UserActionsState;
  let users: Stream<Users>;

  beforeEach(() => {
    users         = stream(new Users());
    attrs         = {
      users,
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
        return (
          <UsersActionsWidget {...attrs}/>);
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

  function john() {
    return User.fromJSON({
                           display_name: "Jon Doe",
                           login_name: "jdoe",
                           is_admin: true,
                           email_me: true,
                           enabled: false,
                           checkin_aliases: []
                         });
  }

  it("should display the number of enabled and disabled users", () => {
    users(new Users(bob(), alice(), john()));
    m.redraw();

    expect(find("users-total")).toHaveText("3");
    expect(find("users-enabled")).toHaveText("1");
    expect(find("users-disabled")).toHaveText("2");
  });

});
