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

const DashboardFilters = require("models/dashboard/dashboard_filters");

describe("DashboardFilters", () => {
  it("names() returns an array of filter names", () => {
    const filters = new DashboardFilters([def, a, b, c]);
    expect(filters.names()).toEqual(["Default", "a", "b", "c"]);
  });

  it("defaultFilter() returns a filter matching the default name", () => {
    const filters = new DashboardFilters([def, a, b, c]);
    expect(filters.defaultFilter().name).toBe("Default");
    expect(filters.defaultFilter()).toEqual(def);
    expect(filters.defaultFilter() === def).toBe(true);
  });

  it("defaultFilter() creates a new default wildcard filter when a default does not exist", () => {
    const filters = new DashboardFilters([a]);
    expect(filters.names()).toEqual(["a"]);

    const newDefault = filters.defaultFilter();
    expect(filters.names()).toEqual(["Default", "a"]);

    expect(newDefault === def).toBe(false);
    expect(JSON.stringify(newDefault) !== JSON.stringify(def)).toBe(true); // not structurally equal

    expect(newDefault.name).toBe("Default");
    expect(newDefault.type).toBe("blacklist");
    expect(newDefault.pipelines).toEqual([]);
  });

  it("findFilter() finds a filter by name, case-insensitively", () => {
    const filters = new DashboardFilters([def, a, b, c]);
    expect(filters.findFilter("a")).toEqual(a);
    expect(filters.findFilter("A")).toEqual(a);
  });

  it("findFilter() falls back to defaultFilter when it cannot resolve a filter by name", () => {
    expect(new DashboardFilters([def, a]).findFilter("c")).toEqual(def);

    const filters = new DashboardFilters([a]);
    const found = filters.findFilter("c");

    expect(found.name).toBe("Default");
    expect(filters.names()).toEqual(["Default", "a"]);
    expect(found).toEqual({name: "Default", state: [], type: "blacklist", pipelines: []});
  });

  it("addFilter() appends a filter", () => {
    const filters = new DashboardFilters([def, a]);
    filters.addFilter(b);
    expect(filters.names()).toEqual(["Default", "a", "b"]);
    expect(filters.findFilter("b")).toEqual(b);
  });

  it("removeFilter() removes a filter by name", () => {
    const filters = new DashboardFilters([def, a, b, c]);
    filters.removeFilter("b");
    expect(filters.names()).toEqual(["Default", "a", "c"]);
  });

  it("replaceFilter() replaces a filter by name", () => {
    const filters = new DashboardFilters([def, a, b]);
    expect(filters.names()).toEqual(["Default", "a", "b"]);

    filters.replaceFilter("a", c);
    expect(filters.names()).toEqual(["Default", "c", "b"]);
    expect(filters.findFilter("c")).toEqual(c);
  });

  it("replaceFilter() falls back to addFilter() when the existing filter cannot be resolved", () => {
    const filters = new DashboardFilters([def, a, b]);
    expect(filters.names()).toEqual(["Default", "a", "b"]);

    filters.replaceFilter("d", c);
    expect(filters.names()).toEqual(["Default", "a", "b", "c"]);
    expect(filters.findFilter("c")).toEqual(c);
  });

  it("replaceFilter() falls back to addFilter() when the existing filter name is null", () => {
    const filters = new DashboardFilters([def, a, b]);
    expect(filters.names()).toEqual(["Default", "a", "b"]);

    filters.replaceFilter(null, c);
    expect(filters.names()).toEqual(["Default", "a", "b", "c"]);
    expect(filters.findFilter("c")).toEqual(c);
  });

  it("moveFilterByIndex() moves source filter to destination position", () => {
    const filters = new DashboardFilters([a, b, c, d, e]);
    const idx = (f) => filters.names().indexOf(f);

    expect(filters.names()).toEqual(["a", "b", "c", "d", "e"]);

    filters.moveFilterByIndex(idx("e"), idx("c")); // test move backward
    expect(filters.names()).toEqual(["a", "b", "e", "c", "d"]);

    filters.moveFilterByIndex(idx("b"), idx("b")); // no-op, should not change order
    expect(filters.names()).toEqual(["a", "b", "e", "c", "d"]);

    filters.moveFilterByIndex(idx("b"), idx("c")); // test move forward
    expect(filters.names()).toEqual(["a", "e", "c", "b", "d"]);

    filters.moveFilterByIndex(idx("d"), idx("a")); // test move to front
    expect(filters.names()).toEqual(["d", "a", "e", "c", "b"]);
  });

  it("moveFilterByIndex() throws errors when source or destination is out of bounds", () => {
    const filters = new DashboardFilters([a, b, c, d, e]);
    expect(filters.names()).toEqual(["a", "b", "c", "d", "e"]);

    expect(() => filters.moveFilterByIndex(0, 6)).toThrow(new RangeError("Cannot resolve filter at index 6; out of bounds"));
    expect(filters.names()).toEqual(["a", "b", "c", "d", "e"]);

    expect(() => filters.moveFilterByIndex(0, -3)).toThrow(new RangeError("Cannot resolve filter at index -3; out of bounds"));
    expect(filters.names()).toEqual(["a", "b", "c", "d", "e"]);

    expect(() => filters.moveFilterByIndex(-3, 1)).toThrow(new RangeError("Cannot resolve filter at index -3; out of bounds"));
    expect(filters.names()).toEqual(["a", "b", "c", "d", "e"]);

    expect(() => filters.moveFilterByIndex(6, 1)).toThrow(new RangeError("Cannot resolve filter at index 6; out of bounds"));
    expect(filters.names()).toEqual(["a", "b", "c", "d", "e"]);
  });

  it("clone() produces a structurally equivalent copy (deep copy)", () => {
    const original = new DashboardFilters([def, a, b]);
    const dupe = original.clone();

    expect(original !== dupe).toBe(true);
    expect(JSON.stringify(original)).toEqual(JSON.stringify(dupe));

    expect(original.filters).toEqual(dupe.filters);

    for (let i = 0, len = original.filters.length; i < len; ++i) {
      const x = original.filters[i], y = dupe.filters[i];

      expect(x).toEqual(y);
      expect(x !== y).toBe(true); //structually equal, but not referentially
    }
  });
});

const def = {name: "Default", state:[], type: "whitelist", pipelines: ["a", "c"]};
const a = {name: "a", state: [], type: "blacklist", pipelines: ["a"]};
const b = {name: "b", state: [], type: "blacklist", pipelines: ["b"]};
const c = {name: "c", state: [], type: "whitelist", pipelines: ["c"]};
const d = {name: "d", state: [], type: "whitelist", pipelines: ["d"]};
const e = {name: "e", state: [], type: "blacklist", pipelines: ["e"]};
