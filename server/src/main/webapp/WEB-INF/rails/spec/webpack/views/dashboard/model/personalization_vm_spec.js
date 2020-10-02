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
import Stream from "mithril/stream";
import {SparkRoutes} from "helpers/spark_routes";
import {PersonalizationVM} from "views/dashboard/models/personalization_vm";
import {DashboardFilters} from "models/dashboard/dashboard_filters";

describe("Personalization View Model", () => {
  it("active() tests if a view name matches the current view case-insensitively", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    expect(vm.active("Bar")).toBe(false);

    expect(vm.active("Foo")).toBe(true);
    expect(vm.active("foo")).toBe(true);
    expect(vm.active("FOO")).toBe(true);
  });

  it("activate() marks a view as the current view", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    expect(vm.active("Bar")).toBe(false);
    expect(vm.active("Foo")).toBe(true);

    vm.activate("Bar");
    expect(vm.active("Bar")).toBe(true);
    expect(vm.active("Foo")).toBe(false);

    vm.activate("bAz");
    expect(vm.active("Baz")).toBe(true);
    expect(vm.active("Bar")).toBe(false);
  });

  it("activate() should trigger onchange()", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.onchange = jasmine.createSpy();
    vm.names(["Foo", "Bar", "Baz"]);

    vm.activate("Bar");
    expect(vm.active("Bar")).toBe(true);
    expect(vm.onchange).toHaveBeenCalled();
  });

  it("hideAllDropdowns() hides all dropdowns", () => {
    const currentView = Stream();
    const vm = new PersonalizationVM(currentView);

    // baseline
    expect(vm.tabSettingsDropdownVisible()).toBe(false);
    expect(vm.tabsListDropdownVisible()).toBe(false);

    vm.toggleTabSettingsDropdown();
    expect(vm.tabSettingsDropdownVisible()).toBe(true);

    vm.hideAllDropdowns();
    expect(vm.tabSettingsDropdownVisible()).toBe(false);
    expect(vm.tabsListDropdownVisible()).toBe(false);

    vm.toggleTabsListDropdown();
    expect(vm.tabsListDropdownVisible()).toBe(true);

    vm.hideAllDropdowns();
    expect(vm.tabSettingsDropdownVisible()).toBe(false);
    expect(vm.tabsListDropdownVisible()).toBe(false);
  });

  it("toggleTabsListDropdown() toggles the tabs list dropdown visibility", () => {
    const currentView = Stream();
    const vm = new PersonalizationVM(currentView);

    expect(vm.tabsListDropdownVisible()).toBe(false);

    vm.toggleTabsListDropdown();
    expect(vm.tabsListDropdownVisible()).toBe(true);

    vm.toggleTabsListDropdown();
    expect(vm.tabsListDropdownVisible()).toBe(false);
  });

  it("toggleTabSettingsDropdown() toggles the tab settings dropdown visibility", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    expect(vm.tabSettingsDropdownVisible()).toBe(false);

    vm.toggleTabSettingsDropdown();
    expect(vm.tabSettingsDropdownVisible()).toBe(true);

    vm.toggleTabSettingsDropdown();
    expect(vm.tabSettingsDropdownVisible()).toBe(false);
  });

  it("actionHandler() ensures dropdown is hidden when firing the wrapped handler", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    const fn = jasmine.createSpy();

    vm.toggleTabSettingsDropdown();
    expect(vm.tabSettingsDropdownVisible()).toBe(true);

    vm.actionHandler(fn)({stopPropagation: () => {}});
    expect(vm.tabSettingsDropdownVisible()).toBe(false);
    expect(fn).toHaveBeenCalled();
  });

  it("isDefault() tests if a given tab is the default, case-insensitively", () => {
    const vm = new PersonalizationVM(Stream());

    expect(vm.isDefault("deFaUlT")).toBe(true);
    expect(vm.isDefault("default")).toBe(true);
    expect(vm.isDefault("DEFAULT")).toBe(true);
    expect(vm.isDefault("nope")).toBe(false);
  });

  it("canonicalCurrentName() gets the canonical name of the filter regardless of what currentView() returns", () => {
    const currentView = Stream("BAZ");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    expect(vm.canonicalCurrentName()).toBe("Baz");
  });

  it("etag() returns the local copy of the content hash", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    vm.checksum("abcdefg");
    expect(vm.etag()).toBe("abcdefg");
  });

  it("etag(updatedHash) fetches new personalization data when the new hash differs from the local copy", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    vm.checksum("abcdefg");
    expect(vm.etag()).toBe("abcdefg");

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), undefined, 'GET').andReturn({
        responseText:    JSON.stringify({filters: [{ name:"New", type: "whitelist", pipelines: ["a"] }]}),
        responseHeaders: {
          ETag:           `"1234567"`,
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          200
      });

      vm.etag("abcdefg"); // should cause no change
      expect(vm.names()).toEqual(["Foo", "Bar", "Baz"]);
      expect(vm.checksum()).toBe("abcdefg");
      expect(vm.etag()).toBe("abcdefg");

      vm.etag("foo");
      expect(vm.names()).toEqual(["New"]);
      expect(vm.checksum()).toBe("1234567");
      expect(vm.etag()).toBe("1234567");
    });
  });

  it("onchange() allows registration of multiple handlers", () => {
    const vm = new PersonalizationVM(Stream());
    const a = jasmine.createSpy();
    const b = jasmine.createSpy();

    vm.onchange(a);
    vm.onchange(b);

    expect(a).toHaveBeenCalledTimes(0);
    expect(b).toHaveBeenCalledTimes(0);

    vm.onchange();

    expect(a).toHaveBeenCalled();
    expect(b).toHaveBeenCalled();
  });

  it("tabs() output depends on whether or not a sort is in progress", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar"]);

    expect(vm.tabs()).toEqual([{id: "Foo", name: "Foo"}, {id: "Bar", name: "Bar"}]);

    vm.stagedSort(new DashboardFilters([{ name:"New", type: "whitelist", pipelines: ["a"] }]));
    expect(vm.tabs()).toEqual([{id: "New", name: "New"}]);

    vm.stagedSort(null);
    expect(vm.tabs()).toEqual([{id: "Foo", name: "Foo"}, {id: "Bar", name: "Bar"}]);
  });

  it('selectFirstView() should select first view', () => {
    const currentView = Stream("Bar");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);
    expect(vm.currentView()).toBe("Bar");

    vm.selectFirstView();
    expect(vm.currentView()).toBe("Foo");
  });
});
