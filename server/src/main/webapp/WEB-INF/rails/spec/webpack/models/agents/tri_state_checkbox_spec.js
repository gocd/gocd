/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {TriStateCheckbox} from "models/agents/tri_state_checkbox";

describe('TriStateCheckbox (Resource/Environment) Model', () => {

  it("should create a checked state checkbox if it present on all of the agent-values-set", () => {
    const resource = new TriStateCheckbox('Linux', [['Linux']]);
    expect(resource.name()).toBe('Linux');
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);
  });

  it("should create an unchecked state checkbox if it absent on all of the agent-values-set", () => {
    const resource = new TriStateCheckbox('Linux', [[]]);
    expect(resource.name()).toBe('Linux');
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(false);
  });

  it("should create an indeterminate state checkbox if it present on few of the agent-values-set", () => {
    const resource = new TriStateCheckbox('Linux', [['Linux'], ['Firefox']]);
    expect(resource.name()).toBe('Linux');
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(true);
  });

  it("should contain the resource information", () => {
    const resource = new TriStateCheckbox('Linux', [['Linux']]);
    expect(resource.name()).toBe('Linux');
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);
  });

  it('should check the checkbox', () => {
    const resource = new TriStateCheckbox('Chrome', [['Linux'], ['Chrome']]);
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(true);
    resource.becomeChecked();
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);
  });

  it('should uncheck the checkbox', () => {
    const resource = new TriStateCheckbox('Chrome', [['Chrome']]);
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);
    resource.becomeUnchecked();
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(false);
  });

  it('should indeterminate the checkbox', () => {
    const resource = new TriStateCheckbox('Chrome', [['Chrome']]);
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);
    resource.becomeIndeterminate();
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(true);
  });

  it('should cycle through 3 states on click of checkbox if initial state is indeterminate', () => {
    const resource = new TriStateCheckbox('Chrome', [['Linux'], ['Chrome']]);
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(true);

    resource.click();
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);

    resource.click();
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(false);

    resource.click();
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(true);
  });

  it('should cycle through 2 states on click of the checkbox if initial state is checked', () => {
    const resource = new TriStateCheckbox('Chrome', [['Chrome']]);
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);

    resource.click();
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(false);
  });

  it('should cycle through 2 states on click of the checkbox if initial state is unchecked', () => {
    const resource = new TriStateCheckbox('Linux', [['Chrome']]);
    expect(resource.isChecked()).toBe(false);
    expect(resource.isIndeterminate()).toBe(false);

    resource.click();
    expect(resource.isChecked()).toBe(true);
    expect(resource.isIndeterminate()).toBe(false);
  });

  it("should be disabled if the disabled property is set", () => {
    const resource = new TriStateCheckbox('Linux', [['Linux']], true);
    expect(resource.disabled()).toBe(true);
  });

  it("should not be disabled if the disabled property is not set", () => {
    const resource = new TriStateCheckbox('Linux', [['Linux']]);
    expect(resource.disabled()).toBe(false);
  });

  it('should set tooltip property', () => {
    const resource = new TriStateCheckbox('Linux', [['Linux']], false, "towering gila monster");
    expect(resource.tooltip()).toBe("towering gila monster");
  });
});
