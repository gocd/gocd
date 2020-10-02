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
import _ from "lodash";
import Stream from "mithril/stream";
import {PipelineListVM} from "views/dashboard/models/pipeline_list_vm";

describe("Pipeline List View Model", () => {
  it("restores prior expand/collapse state after clearing search", () => {
    const all = { group1: "abc".split(""), group2: "ab,cd".split(","), group3: "de".split("") };
    const sel = _st({ a: true, b: true, c: true, ab: true, cd: false, d: false, e: false });
    const model = new PipelineListVM(all, sel);

    expect(collectExpandedGroups(model.displayedList())).toEqual([]); // all collapsed by default

    // expand group2
    model.displayedList().group2.expanded(true);
    expect(collectExpandedGroups(model.displayedList())).toEqual(["group2"]);

    // search term should match both groups
    model.searchTerm("a");
    expect(collectExpandedGroups(model.displayedList())).toEqual(["group1", "group2"]); // all matches expanded by default in search

    // collapse group2 while in search results
    model.displayedList().group2.expanded(false);
    expect(collectExpandedGroups(model.displayedList())).toEqual(["group1"]); // should honor expand/collapse during search

    // clear search term
    model.searchTerm("");
    expect(collectExpandedGroups(model.displayedList())).toEqual(["group2"]); // should restore expand/collapse state to that of prior to search
  });

  it("only displays pipelines that include the search term matching case insensitively", () => {
    const all = { group1: "abc".split(""), group2: "ab,cd".split(",") };
    const sel = _st({ a: true, b: true, c: true, ab: true, cd: false });
    const model = new PipelineListVM(all, sel);
    model.searchTerm("B");

    const names = getPipelineNames(model.displayedList());
    expect(names).toEqual(["ab", "b"]);
  });

  it("displays all pipelines if no search term", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: false, e: true, f: false });
    const model = new PipelineListVM(all, sel);

    const names = getPipelineNames(model.displayedList());
    expect(names).toEqual(("abcdef").split(""));
  });

  it("automatically calculates group selection state", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: false, e: true, f: false });

    const model = new PipelineListVM(all, sel);
    expect(model.displayedList().group1.selected()).toBe(true);
    expect(model.displayedList().group2.selected()).toBe(false);

    sel.b(false);
    expect(model.displayedList().group1.selected()).toBe(false);
  });

  it("hasAnySelections() reports whether at least 1 pipeline is selected", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: false, b: false, c: false, d: false, e: false, f: false });

    const model = new PipelineListVM(all, sel);
    expect(model.hasAnySelections()).toBe(false);

    sel.b(true);
    expect(model.hasAnySelections()).toBe(true);
  });

  it("hasSearch() detects presence of search term", () => {
    const model = new PipelineListVM({}, {});

    model.searchTerm("");
    expect(model.hasSearch()).toBe(false);

    model.searchTerm("   ");
    expect(model.hasSearch()).toBe(false);

    model.searchTerm("foo");
    expect(model.hasSearch()).toBe(true);
  });

  it('hasAllSelected() should report true if all pipelines are selected', () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: true, e: true, f: true });

    const model = new PipelineListVM(all, sel);
    expect(model.hasAllSelected()).toBe(true);

    sel.b(false);
    expect(model.hasAllSelected()).toBe(false);
  });

  it('hasNoneSelected() should report true if no pipelines are selected', () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: false, b: false, c: false, d: false, e: false, f: false });

    const model = new PipelineListVM(all, sel);
    expect(model.hasNoneSelected()).toBe(true);

    sel.b(true);
    expect(model.hasNoneSelected()).toBe(false);
  });

  it("selectAll() should alter all pipelines", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: true, e: false, f: false });

    const model = new PipelineListVM(all, sel);
    model.selectAll();

    expect(model.displayedList().group1.selected()).toBe(true);
    expect(model.displayedList().group2.selected()).toBe(true);

    expect(_.every(sel, (s) => s())).toBe(true);
  });

  it("selectNone() should alter all pipelines", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: true, e: false, f: false });

    const model = new PipelineListVM(all, sel);
    model.selectNone();

    expect(model.displayedList().group1.selected()).toBe(false);
    expect(model.displayedList().group2.selected()).toBe(false);
    expect(_.every(sel, (s) => !s())).toBe(true);
  });

  it("selectAll() should honor search term", () => {
    const all = { group1: "aa,ab,c".split(","), group2: "d,ae,af".split(",") };
    const sel = _st({ aa: false, ab: false, c: false, d: false, ae: false, af: false });

    const model = new PipelineListVM(all, sel);
    model.searchTerm("a");
    model.selectAll();

    expect(selectedPipelineNames(sel)).toEqual(["aa", "ab", "ae", "af"]);
  });

  it("selectNone() should honor search term", () => {
    const all = { group1: "aa,ab,c".split(","), group2: "d,ae,af".split(",") };
    const sel = _st({ aa: true, ab: true, c: true, d: true, ae: true, af: true });

    const model = new PipelineListVM(all, sel);
    model.searchTerm("a");
    model.selectNone();

    expect(selectedPipelineNames(sel)).toEqual(["c", "d"]);
  });

  it("selecting at least one pipeline should set group indeterminate state", () => {
    const all = { group1: "abc".split(""), group2: "def".split(""), group3: "g" };
    const sel = _st({ a: true, b: false, c: false, d: true, e: true, f: true, g: false });

    const model = new PipelineListVM(all, sel);
    const list = model.displayedList();

    expect(list.group1.indeterminate()).toBe(true);
    expect(list.group1.selected()).toBe(false);

    expect(list.group2.indeterminate()).toBe(false);
    expect(list.group2.selected()).toBe(true);

    expect(list.group3.indeterminate()).toBe(false);
    expect(list.group3.selected()).toBe(false);
  });

  it("toggling the selected attribute on a pipeline entry alters the internal selection map", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: true, e: false, f: false });

    const model = new PipelineListVM(all, sel);
    expect(sel.f()).toBe(false);

    const pipes = model.displayedList().group2.pipelines;
    expect(pipes[2].name).toBe("f");
    check(pipes[2].selected); // should flip the specified state, expects this to be the previous state before onchange()

    expect(sel.f()).toBe(true);
  });

  it("toggling the selected attribute on a group alters all pipelines of the group", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: true, e: false, f: false });

    const list = new PipelineListVM(all, sel).displayedList();
    check(list.group2.selected);
    expect(_.every(list.group2.pipelines, (p) => p.selected())).toBe(true);

    uncheck(list.group2.selected);
    expect(_.every(list.group2.pipelines, (p) => !p.selected())).toBe(true);

    // should not affect other groups
    expect(_.every(list.group1.pipelines, (p) => p.selected())).toBe(true);
  });

  it("PipelineListVM.calcSelectionMap() can output the initial selection state as a map", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const shown = PipelineListVM.calcSelectionMap(all, false, ["a", "c", "e"]);

    expect(JSON.stringify(shown)).toBe(JSON.stringify(_st({ a: true, b: false, c: true, d: false, e: true, f: false })));

    const hidden = PipelineListVM.calcSelectionMap(all, true, ["a", "c", "e"]);
    expect(JSON.stringify(hidden)).toBe(JSON.stringify(_st({ a: false, b: true, c: false, d: true, e: false, f: true })));
  });

  it("pipelines() can output the state of the internal selection map as an allowlist or denylist of pipelines", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: true, e: false, f: false });

    const model = new PipelineListVM(all, sel);
    expect(model.pipelines(false)).toEqual(["a", "b", "c", "d"]);
    expect(model.pipelines(true)).toEqual(["e", "f"]);
  });
});

function check(stream) {
  stream(true);
}

function uncheck(stream) {
  stream(false);
}

function _st(obj) {
  return _.reduce(obj, (m, v, k) => { m[k] = v instanceof Stream ? v : Stream(v); return m; }, {});
}

function selectedPipelineNames(sel) {
  return _.reduce(sel, (m, v, k) => { if (v()) { m.push(k); } return m; }, []).sort();
}

function collectExpandedGroups(groups) {
  return _.reduce(groups, (m, v, k) => {
    if (v.expanded()) { m.push(k); }
    return m;
  }, []).sort();
}

function getPipelineNames(list) {
  return _.reduce(list, (r, g) => r.concat(_.map(g.pipelines, "name")), []).sort();
}
