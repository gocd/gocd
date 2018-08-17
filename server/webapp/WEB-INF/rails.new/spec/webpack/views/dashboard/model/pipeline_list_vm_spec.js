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

const _                 = require("lodash");
const Stream            = require("mithril/stream");
const PipelineListVM = require("views/dashboard/models/pipeline_list_vm");

describe("Pipeline List View Model", () => {
  it("automatically calculates group selection state", () => {
    const all = { group1: "abc".split(""), group2: "def".split("") };
    const sel = _st({ a: true, b: true, c: true, d: false, e: true, f: false });

    const model = new PipelineListVM(all, sel);
    expect(model.displayedList().group1.selected()).toBe(true);
    expect(model.displayedList().group2.selected()).toBe(false);

    sel.b(false);
    expect(model.displayedList().group1.selected()).toBe(false);
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
    const whitelisted = PipelineListVM.calcSelectionMap(all, false, ["a", "c", "e"]);

    expect(JSON.stringify(whitelisted)).toBe(JSON.stringify(_st({ a: true, b: false, c: true, d: false, e: true, f: false })));

    const blacklisted = PipelineListVM.calcSelectionMap(all, true, ["a", "c", "e"]);
    expect(JSON.stringify(blacklisted)).toBe(JSON.stringify(_st({ a: false, b: true, c: false, d: true, e: false, f: true })));
  });

  it("pipelines() can output the state of the internal selection map as a whitelist or blacklist of pipelines", () => {
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
