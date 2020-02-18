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

import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox/index";

describe("TriStateCheckbox Model", () => {
  it("should return current state", () => {
    const checkbox = new TriStateCheckbox(TristateState.on);
    expect(checkbox.state()).toBe("on");
  });

  it("should answer for on state check", () => {
    const checkbox = new TriStateCheckbox(TristateState.on);
    expect(checkbox.isChecked()).toBe(true);
    expect(checkbox.isUnchecked()).toBe(false);
    expect(checkbox.isIndeterminate()).toBe(false);
  });

  it("should answer for off state check", () => {
    const checkbox = new TriStateCheckbox(TristateState.off);
    expect(checkbox.isUnchecked()).toBe(true);
    expect(checkbox.isChecked()).toBe(false);
    expect(checkbox.isIndeterminate()).toBe(false);
  });

  it("should answer for indeterminate state check", () => {
    const checkbox = new TriStateCheckbox(TristateState.indeterminate);
    expect(checkbox.isIndeterminate()).toBe(true);
    expect(checkbox.isChecked()).toBe(false);
    expect(checkbox.isUnchecked()).toBe(false);
  });

  it("should toggle correctly through stages if initially in indeterminate state", () => {
    const checkbox = new TriStateCheckbox(TristateState.indeterminate);

    checkbox.click();
    expect(checkbox.isChecked()).toBe(true);

    checkbox.click();
    expect(checkbox.isUnchecked()).toBe(true);

    checkbox.click();
    expect(checkbox.isIndeterminate()).toBe(true);

    checkbox.click();
    expect(checkbox.isChecked()).toBe(true);
  });

  it("should toggle correctly through stages if initially in on state", () => {
    const checkbox = new TriStateCheckbox(TristateState.on);

    checkbox.click();
    expect(checkbox.isUnchecked()).toBe(true);

    checkbox.click();
    expect(checkbox.isChecked()).toBe(true);
  });

  it("should toggle correctly through stages if initially in off state", () => {
    const checkbox = new TriStateCheckbox(TristateState.off);

    checkbox.click();
    expect(checkbox.isChecked()).toBe(true);
    expect(checkbox.isUnchecked()).toBe(false);
    expect(checkbox.isIndeterminate()).toBe(false);

    checkbox.click();
    expect(checkbox.isChecked()).toBe(false);
    expect(checkbox.isUnchecked()).toBe(true);
    expect(checkbox.isIndeterminate()).toBe(false);
  });

  it("should tell if state is changed", () => {
    const checkbox = new TriStateCheckbox(TristateState.on);

    checkbox.click();
    expect(checkbox.ischanged()).toBe(true);
  });
});
