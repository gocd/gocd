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

import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {GoCDRole, Roles} from "models/roles/roles";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import {UserFilters} from "models/users/user_filters";
import {User, Users} from "models/users/users";
import {UserViewHelper} from "views/pages/users/user_view_helper";
import {Attrs, UsersWidget} from "views/pages/users/users_widget";

describe("UsersWidget", () => {
  let $root: any, root: any;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  let attrs: Attrs;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();

    attrs = {
      users: stream(new Users(bob(), alice())),
      user: bob(),
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
      onToggleAdmin: _.noop,
      userViewHelper: stream(new UserViewHelper())
    };

    attrs.userViewHelper().systemAdmins().users([bob().loginName()]);

    m.mount(root, {
      view() {
        return (<UsersWidget {...attrs}/>);
      }
    });

    m.redraw();

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
    expect(find("users-table")).toBeInDOM();
    expect(find("users-header")[0].children[1]).toContainText("Username");
    expect(find("users-header")[0].children[2]).toContainText("Display name");
    expect(find("users-header")[0].children[4]).toContainText("System Admin");
    expect(find("users-header")[0].children[5]).toContainText("Email");
    expect(find("users-header")[0].children[6]).toContainText("Enabled");

    expect(find("user-row")).toHaveLength(2);

    expect(find("user-username")[0]).toContainText("bob");
    expect(find("user-display-name")[0]).toContainText("Bob");
    expect(find("user-email")[0]).toContainText("bob@example.com");
    expect(find("user-enabled")[0]).toContainText("Yes");

    expect(find("user-username")[1]).toContainText("alice");
    expect(find("user-display-name")[1]).toContainText("Alice");
    expect(find("user-email")[1]).toContainText("alice@example.com");
    expect(find("user-enabled")[1]).toContainText("No");
  });

  it("should render in progress icon if update operation is in progress", () => {
    const user = attrs.users()[0];
    attrs.userViewHelper().userUpdateInProgress(user);

    m.redraw();

    expect(findIn(find("user-super-admin-switch")[0], "Spinner-icon")).toBeInDOM();
  });

  it("should render success icon is update operation is successful", () => {
    const user = attrs.users()[0];
    attrs.userViewHelper().userUpdateSuccessful(user);

    m.redraw();

    expect(find("user-super-admin-switch"));
    expect(findIn(find("user-super-admin-switch")[0], "update-successful")).toBeInDOM();
  });

  it("should render error icon with message if update operation is unsuccessful", () => {
    const user = attrs.users()[0];
    attrs.userViewHelper().userUpdateFailure(user, "boom!");

    m.redraw();

    expect(findIn(find("user-super-admin-switch")[0], "update-unsuccessful")).toBeInDOM();
    expect(find("user-update-error-message")).toContainText("boom!");

  });

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  function findIn(elem: any, id: string) {
    return $(elem).find(`[data-test-id='${id}']`);
  }

});
