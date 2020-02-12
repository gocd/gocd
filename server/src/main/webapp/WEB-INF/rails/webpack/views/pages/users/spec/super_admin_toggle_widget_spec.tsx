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

import m from "mithril";
import Stream from "mithril/stream";
import {User} from "models/users/users";
import "views/components/table/spec/table_matchers";
import {TestHelper} from "views/pages/spec/test_helper";
import {SuperAdminPrivilegeSwitch} from "views/pages/users/super_admin_toggle_widget";
import {UserViewHelper} from "views/pages/users/user_view_helper";

describe("Super Admin Toggle", () => {
  const helper        = new TestHelper();

  let userViewHelper: Stream<UserViewHelper>,
      user: User,
      onToggleAdmin: (e: MouseEvent, user: User) => void;

  beforeEach(() => {
    userViewHelper = Stream(new UserViewHelper());
    userViewHelper().systemAdmins().users(["bob"]);
    user          = bob();
    onToggleAdmin = jasmine.createSpy("onRemoveAdmin");
  });

  beforeEach(mount);

  afterEach(helper.unmount.bind(helper));

  it("should render YES when the user is an admin", () => {
    expect(helper.textByTestId("is-admin-text")).toContain("YES");
  });

  it("should render NO when the user is not an admin", () => {
    user.isAdmin(false);
    helper.redraw();

    expect(helper.textByTestId("is-admin-text")).toContain("NO");
  });

  it("should render Not Specified when system administrators are not configured", () => {
    userViewHelper().systemAdmins().users([]);
    helper.redraw();

    expect(helper.textByTestId("is-admin-text")).toContain("Not Specified");
  });

  it("should render enabled toggle button when the user is an admin", () => {
    expect(helper.byTestId("switch-checkbox")).toBeChecked();
  });

  it("should render disabled toggle button when the user is NOT an admin", () => {
    user.isAdmin(false);
    helper.redraw();

    expect(helper.byTestId("switch-checkbox")).not.toBeChecked();
  });

  it("should render disabled toggle system administrators are not configured", () => {
    userViewHelper().systemAdmins().users([]);
    helper.redraw();

    expect(helper.byTestId("switch-checkbox")).not.toBeChecked();
  });

  it("should render make current user admin tooltip when system administrators are not configured", () => {
    userViewHelper().systemAdmins().users([]);
    helper.redraw();

    const expectedTooltipContent = "Explicitly making 'bob' user a system administrator will result into other users not having system administrator privileges.";

    expect(helper.byTestId("tooltip-wrapper")).toBeInDOM();
    expect(helper.textByTestId("tooltip-content")).toContain(expectedTooltipContent);
  });

  it("should NOT render make current user admin tooltip when system administrators are configured", () => {
    expect(helper.byTestId("tooltip-wrapper")).toBeFalsy();
  });

  it("should make a request to make admin on toggling non admin user privilege", () => {
    user.isAdmin(false);
    helper.redraw();

    expect(onToggleAdmin).not.toHaveBeenCalled();
    helper.clickByTestId('switch-paddle');
    expect(onToggleAdmin).toHaveBeenCalled();
  });

  it("should render tooltip if user is admin because of the role", () => {
    userViewHelper().systemAdmins().users([]);
    userViewHelper().systemAdmins().roles(["admin"]);

    helper.redraw();

    const expectedTooltipContent = "'bob' user has the system administrator privileges because the user is assigned the group administrative role. To remove this user from system administrators, assigned role needs to be removed.";

    expect(helper.byTestId("tooltip-wrapper")).toBeInDOM();
    expect(helper.textByTestId("tooltip-content")).toContain(expectedTooltipContent);
  });

  it("should disable switch if user is admin because of the role", () => {
    userViewHelper().systemAdmins().users([]);

    userViewHelper().systemAdmins().roles(["admin"]);

    helper.redraw();

    expect(helper.byTestId("switch-checkbox").hasAttribute("disabled")).toBe(true);
  });

  function mount() {
    helper.mount(() => <SuperAdminPrivilegeSwitch user={user}
                                                  onToggleAdmin={onToggleAdmin}
                                                  userViewHelper={userViewHelper}
    />);
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
