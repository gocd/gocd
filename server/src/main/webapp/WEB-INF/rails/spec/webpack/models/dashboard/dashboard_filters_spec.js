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

import {DashboardFilters} from "models/dashboard/dashboard_filters";

describe("DashboardFilters", () => {
  it("names() returns an array of filter names", () => {
    const filters = new DashboardFilters([def, a, b, c]);
    expect(filters.names()).toEqual(["Default", "a", "b", "c"]);
  });

  it("defaultFilter() returns the first filter", () => {
    const filters = new DashboardFilters([a, b, c]);
    expect(filters.defaultFilter().name).toBe("a");
    expect(filters.defaultFilter()).toEqual(a);
    expect(filters.defaultFilter() === a).toBe(true);
  });

  it("findFilter() finds a filter by name, case-insensitively", () => {
    const filters = new DashboardFilters([def, a, b, c]);
    expect(filters.findFilter("a")).toEqual(a);
    expect(filters.findFilter("A")).toEqual(a);
  });

  it("findFilter() falls back to defaultFilter when it cannot resolve a filter by name", () => {
    expect(new DashboardFilters([a, b]).findFilter("c")).toEqual(a);

    const filters = new DashboardFilters([a]);
    const found = filters.findFilter("c");

    expect(found.name).toBe("a");
    expect(filters.names()).toEqual(["a"]);
    expect(found).toEqual(a);
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
