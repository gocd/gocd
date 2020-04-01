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
import {TestHelper} from  "views/pages/spec/test_helper";
import {KeyValEditor} from "../editor";
import {EntriesVM} from "../vms";

const helper = new TestHelper();

// selectors
const NAME_FIELD    = `input[type="text"][placeholder="Name"]`;
const VALUE_FIELD   = `input[type="text"][placeholder="Value"]`;
const SECRET_FIELD  = `input[type="password"][placeholder="Secret Value"]`;
const DELETE_BUTTON = `button[title="Delete"]`;
const ADD_BUTTON    = `button[title="Add another property"]`;

describe("<KeyValEditor/>", () => {
  afterEach(() => helper.unmount());

  it("creates basic structure", () => {
    const vm = new EntriesVM([plain("a", "b")]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(helper.qa("th").length).toBe(3);
    expect(helper.textAll("th")).toEqual(["Encrypt", "Name", "Value"]);

    expect(helper.byTestId("switch-checkbox")).toBeInDOM();
    assertExistsWithValue(NAME_FIELD, "a");
    assertExistsWithValue(VALUE_FIELD, "b");
    expect(helper.q(DELETE_BUTTON)).toBeInDOM();
    expect(helper.q(ADD_BUTTON)).toBeInDOM();
  });

  it("renders a password field for a secret value", () => {
    const vm = new EntriesVM([encr("a", "b")]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(helper.qa("th").length).toBe(3);
    expect(helper.textAll("th")).toEqual(["Encrypt", "Name", "Value"]);

    expect(helper.byTestId("switch-checkbox")).toBeInDOM();
    assertExistsWithValue(NAME_FIELD, "a");
    assertExistsMatching(SECRET_FIELD, /^\*+$/);
    expect(helper.q(DELETE_BUTTON)).toBeInDOM();
    expect(helper.q(ADD_BUTTON)).toBeInDOM();
  });

  it("ensures there is a blank entry if no initial properties", () => {
    const vm = new EntriesVM([]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(helper.byTestId("switch-checkbox")).toBeInDOM();
    assertExistsWithValue(NAME_FIELD, "");
    assertExistsWithValue(VALUE_FIELD, "");
    expect(helper.q(DELETE_BUTTON)).toBeInDOM();
    expect(helper.q(ADD_BUTTON)).toBeInDOM();
  });

  it("creates a row per entry", () => {
    const vm = new EntriesVM([plain("foo", "bar"), plain("baz", "quu"), plain("beavis", "butthead")]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    // still one set of headers
    expect(helper.qa("th").length).toBe(3);
    expect(helper.textAll("th")).toEqual(["Encrypt", "Name", "Value"]);

    // each row has the following
    expect(helper.allByTestId("switch-checkbox").length).toBe(3);
    expect(helper.qa(NAME_FIELD).length).toBe(3);
    expect(helper.valueAll(NAME_FIELD)).toEqual(["foo", "baz", "beavis"]);
    expect(helper.qa(VALUE_FIELD).length).toBe(3);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["bar", "quu", "butthead"]);
    expect(helper.qa(DELETE_BUTTON).length).toBe(3);

    expect(helper.q(ADD_BUTTON)).toBeInDOM(); // just one of these
  });

  it("allows headers to be overridden", () => {
    helper.mount(() => <KeyValEditor model={new EntriesVM()} headers={[<th>llama</th>, <th>obama</th>, <th>mamma</th>]}/>);
    expect(helper.qa("th").length).toBe(3);
    expect(helper.textAll("th")).toEqual(["llama", "obama", "mamma"]);
  });

  it("updating the fields will update the model", () => {
    const vm = new EntriesVM([plain("a", "b")]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(vm.toJSON()).toEqual([{ key: "a", value: "b" }]);

    helper.oninput(NAME_FIELD, "hello");
    helper.oninput(VALUE_FIELD, "world");
    helper.clickByTestId("switch-checkbox");

    expect(vm.toJSON()).toEqual([{ key: "hello", value: "world", secure: true }]);
  });

  it("clicking delete will remove the row", () => {
    const vm = new EntriesVM([plain("a", "b"), plain("c", "d")]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(helper.qa(NAME_FIELD).length).toBe(2);
    expect(helper.valueAll(NAME_FIELD)).toEqual(["a", "c"]);

    expect(helper.qa(VALUE_FIELD).length).toBe(2);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["b", "d"]);

    helper.click(helper.qa(DELETE_BUTTON)[0]);

    expect(helper.qa(NAME_FIELD).length).toBe(1);
    expect(helper.valueAll(NAME_FIELD)).toEqual(["c"]);

    expect(helper.qa(VALUE_FIELD).length).toBe(1);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["d"]);
  });

  it("clicking add will add a blank row to the bottom", () => {
    const vm = new EntriesVM([plain("a", "b")]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(helper.valueAll(NAME_FIELD)).toEqual(["a"]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["b"]);

    helper.click(ADD_BUTTON);
    expect(helper.valueAll(NAME_FIELD)).toEqual(["a", ""]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["b", ""]);

    // can keep clicking...
    helper.click(ADD_BUTTON);
    expect(helper.valueAll(NAME_FIELD)).toEqual(["a", "", ""]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["b", "", ""]);
  });

  it("all new blank rows honor namespaces", () => {
    const vm = new EntriesVM([plain("ns.a", "b")], "ns");
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(helper.valueAll(NAME_FIELD)).toEqual(["a"]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["b"]);

    helper.click(ADD_BUTTON);
    helper.click(ADD_BUTTON);
    helper.click(ADD_BUTTON);
    expect(helper.valueAll(NAME_FIELD)).toEqual(["a", "", "", ""]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["b", "", "", ""]);

    const nameFields = helper.qa(NAME_FIELD);

    [1, 2, 3].forEach((i) => {
      helper.oninput(nameFields[i], "key" + i);
    });

    expect(helper.valueAll(NAME_FIELD)).toEqual(["a", "key1", "key2", "key3"]);
    expect(vm.toJSON()).toEqual([
      { key: "ns.a", value: "b" },
      { key: "ns.key1", value: "" },
      { key: "ns.key2", value: "" },
      { key: "ns.key3", value: "" },
    ]);
  });

  it("deleting the last row will replace it with a blank one", () => {
    const vm = new EntriesVM([plain("a", "b")]);
    helper.mount(() => <KeyValEditor model={vm}/>);

    expect(helper.valueAll(NAME_FIELD)).toEqual(["a"]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual(["b"]);

    helper.click(DELETE_BUTTON);

    expect(helper.valueAll(NAME_FIELD)).toEqual([""]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual([""]);
    expect(vm.toJSON()).toEqual([]);

    // doing it again, even on a blank row, will just replace it with another blank row
    helper.click(DELETE_BUTTON);

    expect(helper.valueAll(NAME_FIELD)).toEqual([""]);
    expect(helper.valueAll(VALUE_FIELD)).toEqual([""]);
    expect(vm.toJSON()).toEqual([]);
  });
});

function assertExistsWithValue(selector: string, value: string) {
  const input = helper.q(selector);
  expect(input).toBeInDOM();
  expect(helper.value(input)).toBe(value);
}

function assertExistsMatching(selector: string, pattern: RegExp) {
  const input = helper.q(selector);
  expect(input).toBeInDOM();
  expect(helper.value(input)).toMatch(pattern);
}

function plain(key: string, value: string) {
  return { key, value, encrypted: false };
}

function encr(key: string, value: string) {
  return { key, value, encrypted: true };
}
