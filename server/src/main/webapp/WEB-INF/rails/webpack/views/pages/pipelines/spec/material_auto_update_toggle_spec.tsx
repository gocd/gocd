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
import {basicAccessor} from "models/base/accessor";
import {TestHelper} from "views/pages/spec/test_helper";
import {DependencyIgnoreSchedulingToggle, MaterialAutoUpdateToggle} from "../material_auto_update_toggle";

const helper = new TestHelper();

describe("MaterialAutoUpdateToggle", () => {
  afterEach(helper.unmount.bind(helper));

  it("maps \"auto\" => true and \"manual\" => false", () => {
    const toggle = basicAccessor<boolean>(true);
    helper.mount(() => <MaterialAutoUpdateToggle toggle={toggle}/>);

    expect(toggle()).toBe(true);
    expect(helper.byTestId("radio-auto")).toBeChecked();
    expect(helper.byTestId("radio-manual")).not.toBeChecked();

    helper.clickByTestId("radio-manual");

    expect(toggle()).toBe(false);
    expect(helper.byTestId("radio-auto")).not.toBeChecked();
    expect(helper.byTestId("radio-manual")).toBeChecked();

    helper.clickByTestId("radio-auto");

    expect(toggle()).toBe(true);
    expect(helper.byTestId("radio-auto")).toBeChecked();
    expect(helper.byTestId("radio-manual")).not.toBeChecked();
  });

  it("accepts a noun to change the wording", () => {
    const toggle = basicAccessor<boolean>(true);
    helper.mount(() => <MaterialAutoUpdateToggle toggle={toggle} noun="peacock"/>);

    expect(helper.textAllByTestId("form-field-label")).toEqual([
      "Peacock polling behavior:",
      "Regularly fetch updates to this peacock",
      "Fetch updates to this peacock only on webhook or manual trigger",
    ]);
  });
});

describe("DependencyIgnoreSchedulingToggle", () => {
  afterEach(helper.unmount.bind(helper));

  it("maps \"auto\" => false and \"manual\" => true", () => {
    const toggle = basicAccessor<boolean>(false);
    helper.mount(() => <DependencyIgnoreSchedulingToggle toggle={toggle}/>);

    expect(toggle()).toBe(false);
    expect(helper.byTestId("radio-auto")).toBeChecked();
    expect(helper.byTestId("radio-manual")).not.toBeChecked();

    helper.clickByTestId("radio-manual");

    expect(toggle()).toBe(true);
    expect(helper.byTestId("radio-auto")).not.toBeChecked();
    expect(helper.byTestId("radio-manual")).toBeChecked();

    helper.clickByTestId("radio-auto");

    expect(toggle()).toBe(false);
    expect(helper.byTestId("radio-auto")).toBeChecked();
    expect(helper.byTestId("radio-manual")).not.toBeChecked();
  });
});
