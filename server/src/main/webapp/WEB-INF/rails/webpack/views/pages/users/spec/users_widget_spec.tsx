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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {GoCDRole, Roles} from "models/roles/roles";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import {User, Users} from "models/users/users";
import {UserFilters} from "models/users/user_filters";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";
import {Attrs, UsersWidget} from "views/pages/users/users_widget";
import {UserViewHelper} from "views/pages/users/user_view_helper";

const flag: (val?: boolean) => Stream<boolean> = Stream;

describe("UsersWidget", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  let attrs: Attrs;

  beforeEach(() => {
    attrs = {
      users: Stream(new Users(bob(), alice())),
      user: bob(),
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
      onToggleAdmin: _.noop,
      userViewHelper: Stream(new UserViewHelper()),
      operationState: Stream<OperationState>(OperationState.UNKNOWN)
    };

    attrs.userViewHelper().systemAdmins().users([bob().loginName()]);

    helper.mount(() => <UsersWidget {...attrs}/>);
  });

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
    expect(helper.byTestId("users-table")).toBeInDOM();
    expect(helper.byTestId("users-header").children[1]).toContainText("Username");
    expect(helper.byTestId("users-header").children[2]).toContainText("Display name");
    expect(helper.byTestId("users-header").children[4]).toContainText("System Admin");
    expect(helper.byTestId("users-header").children[5]).toContainText("Email");
    expect(helper.byTestId("users-header").children[6]).toContainText("Enabled");

    expect(helper.allByTestId("user-row")).toHaveLength(2);

    expect(helper.textByTestId("user-username")).toContain("bob");
    expect(helper.textByTestId("user-display-name")).toContain("Bob");
    expect(helper.textByTestId("user-email")).toContain("bob@example.com");
    expect(helper.textByTestId("user-enabled")).toContain("Yes");

    expect(helper.allByTestId("user-username")[1].textContent).toContain("alice");
    expect(helper.allByTestId("user-display-name")[1].textContent).toContain("Alice");
    expect(helper.allByTestId("user-email")[1].textContent).toContain("alice@example.com");
    expect(helper.allByTestId("user-enabled")[1].textContent).toContain("No");
  });

  it("should render in progress icon if update operation is in progress", () => {
    const user = attrs.users()[0];
    attrs.userViewHelper().userUpdateInProgress(user);

    m.redraw.sync();

    expect(helper.byTestId("Spinner-icon", helper.byTestId("user-super-admin-switch"))).toBeInDOM();
  });

  it("should render success icon is update operation is successful", () => {
    const user = attrs.users()[0];
    attrs.userViewHelper().userUpdateSuccessful(user);

    m.redraw.sync();

    expect(helper.byTestId("update-successful", helper.byTestId("user-super-admin-switch"))).toBeInDOM();
  });

  it("should render error icon with message if update operation is unsuccessful", () => {
    const user = attrs.users()[0];
    attrs.userViewHelper().userUpdateFailure(user, "boom!");

    m.redraw.sync();

    expect(helper.byTestId("update-unsuccessful", helper.byTestId("user-super-admin-switch"))).toBeInDOM();
    expect(helper.textByTestId("user-update-error-message")).toContain("boom!");
  });
});
