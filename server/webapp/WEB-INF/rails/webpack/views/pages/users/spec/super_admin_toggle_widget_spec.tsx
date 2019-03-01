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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {User} from "models/users/users";
import "views/components/table/spec/table_matchers";
import {SuperAdminPrivilegeSwitch} from "views/pages/users/super_admin_toggle_widget";
import {UserViewHelper} from "views/pages/users/user_view_helper";

describe("Super Admin Toggle", () => {
  const simulateEvent = require("simulate-event");

  let $root: any, root: any;
  let userViewHelper: Stream<UserViewHelper>,
      user: User,
      onToggleAdmin: (e: MouseEvent, user: User) => void;

  beforeEach(() => {
    userViewHelper = stream(new UserViewHelper());
    userViewHelper().systemAdmins().users(["bob"]);
    user          = bob();
    onToggleAdmin = jasmine.createSpy("onRemoveAdmin");

    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  beforeEach(mount);

  afterEach(unmount);

  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render YES when the user is an admin", () => {
    expect(find("is-admin-text")).toContainText("YES");
  });

  it("should render NO when the user is not an admin", () => {
    user.isAdmin(false);
    m.redraw();

    expect(find("is-admin-text")).toContainText("NO");
  });

  it("should render Not Specified when system administrators are not configured", () => {
    userViewHelper().systemAdmins().users([]);
    m.redraw();

    expect(find("is-admin-text")).toContainText("Not Specified");
  });

  it("should render enabled toggle button when the user is an admin", () => {
    expect(find("switch-checkbox").get(0).checked).toBe(true);
  });

  it("should render disabled toggle button when the user is NOT an admin", () => {
    user.isAdmin(false);
    m.redraw();

    expect(find("switch-checkbox").get(0).checked).toBe(false);
  });

  it("should render make current user admin tooltip when system administrators are not configured", () => {
    userViewHelper().systemAdmins().users([]);
    m.redraw();

    const expectedTooltipContent = "Explicitly making 'bob' user a system administrator will result into other users not having system administrator privileges.";

    expect(find("tooltip-wrapper")).toBeInDOM();
    expect(find("tooltip-content")).toContainText(expectedTooltipContent);
  });

  it("should NOT render make current user admin tooltip when system administrators are configured", () => {
    expect(find("tooltip-wrapper")).not.toBeInDOM();
  });

  it("should make a request to make admin on toggling non admin user privilege", () => {
    user.isAdmin(false);
    m.redraw();

    expect(onToggleAdmin).not.toHaveBeenCalled();
    simulateEvent.simulate(find("switch-paddle").get(0), "click");
    expect(onToggleAdmin).toHaveBeenCalled();
  });

  it("should render tooltip if user is admin because of the role", () => {
    userViewHelper().systemAdmins().users([]);
    userViewHelper().systemAdmins().roles(["admin"]);

    m.redraw();

    const expectedTooltipContent = "'bob' user has the system administrator privileges because the user is assigned the group administrative role. To remove this user from system administrators, assigned role needs to be removed.";

    expect(find("tooltip-wrapper")).toBeInDOM();
    expect(find("tooltip-content")).toContainText(expectedTooltipContent);
  });

  it("should disable switch if user is admin because of the role", () => {
    userViewHelper().systemAdmins().users([]);
    userViewHelper().systemAdmins().roles(["admin"]);

    m.redraw();

    expect(find("switch-checkbox").prop("disabled")).toBe(true);
  });

  function mount() {
    m.mount(root, {
      view() {
        return (
          <SuperAdminPrivilegeSwitch user={user}
                                     onToggleAdmin={onToggleAdmin}
                                     userViewHelper={userViewHelper}
          />
        );
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
});
