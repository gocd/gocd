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

const Stream            = require("mithril/stream");
const SparkRoutes       = require("helpers/spark_routes");
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
});
