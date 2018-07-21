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

const Stream = require("mithril/stream");
const PersonalizationVM = require("views/dashboard/models/personalization_vm");

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

  it("toggleDropdown() toggles the dropdown visibility flag", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    vm.hideDropdown();
    expect(vm.dropdownVisible()).toBe(false);

    vm.toggleDropdown();
    expect(vm.dropdownVisible()).toBe(true);

    vm.toggleDropdown();
    expect(vm.dropdownVisible()).toBe(false);
  });

  it("actionHandler() ensures dropdown is hidden when firing the wrapped handler", () => {
    const currentView = Stream("Foo");
    const vm = new PersonalizationVM(currentView);
    vm.names(["Foo", "Bar", "Baz"]);

    const fn = jasmine.createSpy();

    vm.toggleDropdown();
    expect(vm.dropdownVisible()).toBe(true);

    vm.actionHandler(fn)({stopPropagation: () => {}});
    expect(vm.dropdownVisible()).toBe(false);
    expect(fn).toHaveBeenCalled();
  });

  it("isDefault() tests if a given tab is the default, case-insensitively", () => {
    const vm = new PersonalizationVM(Stream());

    expect(vm.isDefault("deFaUlT")).toBe(true);
    expect(vm.isDefault("default")).toBe(true);
    expect(vm.isDefault("DEFAULT")).toBe(true);
    expect(vm.isDefault("nope")).toBe(false);
  });
});
