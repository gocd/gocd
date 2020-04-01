/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {GoCDRole, Roles} from "models/roles/roles";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import {User, Users} from "models/users/users";
import {UserFilters} from "models/users/user_filters";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";
import {Attrs as UserActionsAttrs, UsersActionsWidget} from "views/pages/users/user_actions_widget";

const flag: (val?: boolean) => Stream<boolean> = Stream;

describe("User Actions Widget", () => {
  let attrs: UserActionsAttrs;
  let users: Stream<Users>;
  const helper = new TestHelper();

  beforeEach(() => {
    users = Stream(new Users());
    attrs = {
      users,
      roles: Stream(new Roles()),
      userFilters: Stream(new UserFilters()),
      initializeRolesDropdownAttrs: _.noop,
      showRoles: flag(false),
      showFilters: flag(false),
      rolesSelection: Stream(new Map<GoCDRole, TriStateCheckbox>()),
      onEnable: () => Promise.resolve(),
      onDisable: () => Promise.resolve(),
      onDelete: _.noop,
      onRolesUpdate: _.noop,
      roleNameToAdd: Stream(),
      onRolesAdd: _.noop,
      operationState: Stream<OperationState>(OperationState.UNKNOWN)
    };
  });

  beforeEach(mount);
  afterEach(helper.unmount.bind(helper));

  function mount() {
    helper.mount(() => <UsersActionsWidget {...attrs}/>);
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
    m.redraw.sync();

    expect(helper.textByTestId("users-total")).toBe("3");
    expect(helper.textByTestId("users-enabled")).toBe("1");
    expect(helper.textByTestId("users-disabled")).toBe("2");
  });

});
