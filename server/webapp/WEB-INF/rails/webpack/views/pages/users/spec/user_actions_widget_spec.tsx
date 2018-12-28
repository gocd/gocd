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

import * as $ from "jquery";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {User, Users} from "models/users/users";
import {FlashMessageModel} from "views/components/flash_message";
import {UsersActionsWidget} from "views/pages/users/user_actions_widget";

const simulateEvent = require("simulate-event");

describe("User Actions Widget", () => {
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
        return (<UsersActionsWidget users={users} message={flashMessageModel}/>);
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
    users(new Users([bob(), alice(), john()]));
    m.redraw();

    expect(find("all-user-count")).toHaveText("3");
    expect(find("enabled-user-count")).toHaveText("1");
    expect(find("disabled-user-count")).toHaveText("2");
  });

  describe("search", () => {
    it("should show filtered users based on the search query", () => {
      users(new Users([bob(), alice(), john()]));
      m.redraw();

      expect(find("all-user-count")).toHaveText("3");
      expect(find("enabled-user-count")).toHaveText("1");
      expect(find("disabled-user-count")).toHaveText("2");

      const searchBy = "Jon Doe";

      const searchField = find("search-box").get(0);
      $(searchField).val(searchBy);
      simulateEvent.simulate(searchField, "input");

      m.redraw();

      expect(find("all-user-count")).toHaveText("1");
      expect(find("enabled-user-count")).toHaveText("0");
      expect(find("disabled-user-count")).toHaveText("1");
    });
  });

  describe("Filters", () => {
    it("it should show filters buttons", () => {
      users(new Users([bob(), alice(), john()]));
      expect(find("filters-btn")).toBeInDOM();
    });

    it("should not show filters view by default", () => {
      users(new Users([bob(), alice(), john()]));

      expect(find("filters-view")).toBeInDOM();
      expect(find("filters-view").get(0).getAttribute("data-test-visible")).toBe("false");
    });

    it("should toggle visibility of filters view on filters-btn click", () => {
      users(new Users([bob(), alice(), john()]));
      expect(find("filters-view").get(0).getAttribute("data-test-visible")).toBe("false");

      find("filters-btn").click();
      m.redraw();

      expect(find("filters-view").get(0).getAttribute("data-test-visible")).toBe("true");
    });

    it("should filter users based on admin privileges", () => {
      users(new Users([bob(), alice(), john()]));
      m.redraw();

      expect(find("all-user-count")).toHaveText("3");
      expect(find("enabled-user-count")).toHaveText("1");
      expect(find("disabled-user-count")).toHaveText("2");

      find("filters-btn").click();
      m.redraw();

      find("form-field-input-super-administrators").click();
      m.redraw();

      expect(find("all-user-count")).toHaveText("2");
      expect(find("enabled-user-count")).toHaveText("1");
      expect(find("disabled-user-count")).toHaveText("1");
    });

    it("should filter users based on normal user privileges", () => {
      users(new Users([bob(), alice(), john()]));
      m.redraw();

      expect(find("all-user-count")).toHaveText("3");
      expect(find("enabled-user-count")).toHaveText("1");
      expect(find("disabled-user-count")).toHaveText("2");

      find("filters-btn").click();
      m.redraw();

      find("form-field-input-normal-users").click();
      m.redraw();

      expect(find("all-user-count")).toHaveText("1");
      expect(find("enabled-user-count")).toHaveText("0");
      expect(find("disabled-user-count")).toHaveText("1");
    });

    it("should filter enabled users", () => {
      users(new Users([bob(), alice(), john()]));
      m.redraw();

      expect(find("all-user-count")).toHaveText("3");
      expect(find("enabled-user-count")).toHaveText("1");
      expect(find("disabled-user-count")).toHaveText("2");

      find("filters-btn").click();
      m.redraw();

      find("form-field-input-enabled").click();
      m.redraw();

      expect(find("all-user-count")).toHaveText("1");
      expect(find("enabled-user-count")).toHaveText("1");
      expect(find("disabled-user-count")).toHaveText("0");
    });

    it("should filter disabled users", () => {
      users(new Users([bob(), alice(), john()]));
      m.redraw();

      expect(find("all-user-count")).toHaveText("3");
      expect(find("enabled-user-count")).toHaveText("1");
      expect(find("disabled-user-count")).toHaveText("2");

      find("filters-btn").click();
      m.redraw();

      find("form-field-input-disabled").click();
      m.redraw();

      expect(find("all-user-count")).toHaveText("2");
      expect(find("enabled-user-count")).toHaveText("0");
      expect(find("disabled-user-count")).toHaveText("2");
    });

    it("should reset all filters", () => {
      users(new Users([bob(), alice(), john()]));
      m.redraw();

      //expand fliters
      find("filters-btn").click();
      m.redraw();

      //apply all filters
      find("form-field-input-super-administrators").click();
      find("form-field-input-normal-users").click();
      find("form-field-input-enabled").click();
      find("form-field-input-disabled").click();
      m.redraw();

      //verify all filters are applied
      expect(find("form-field-input-super-administrators").get(0).checked).toBe(true);
      expect(find("form-field-input-normal-users").get(0).checked).toBe(true);
      expect(find("form-field-input-enabled").get(0).checked).toBe(true);
      expect(find("form-field-input-disabled").get(0).checked).toBe(true);

      //reset filters
      find("reset-filter-btn").click();
      m.redraw();

      //verify all filters are reset
      expect(find("form-field-input-super-administrators").get(0).checked).toBe(false);
      expect(find("form-field-input-normal-users").get(0).checked).toBe(false);
      expect(find("form-field-input-enabled").get(0).checked).toBe(false);
      expect(find("form-field-input-disabled").get(0).checked).toBe(false);
    });
  });
});
