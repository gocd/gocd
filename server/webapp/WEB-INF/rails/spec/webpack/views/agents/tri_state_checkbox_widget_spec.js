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
import {TestHelper} from "views/pages/spec/test_helper";

describe("TriStateCheckboxWidget", () => {
  const m = require("mithril");

  require('jasmine-jquery');

  const TriStateCheckboxWidget = require("views/agents/tri_state_checkbox_widget");
  const TriStateCheckbox       = require("models/agents/tri_state_checkbox");
  const helper                 = new TestHelper();

  const resources = [['Firefox'], ['Firefox', 'Chrome']];

  afterEach(helper.unmount.bind(helper));

  it('should have checkbox with value', () => {
    mount(new TriStateCheckbox('Firefox', resources));
    const checkbox = helper.find('input')[0];
    expect(checkbox).toHaveValue('Firefox');
  });

  it('should select the box as checked depending upon check field', () => {
    mount(new TriStateCheckbox('Firefox', resources));
    const checkbox = helper.find('input')[0];
    expect(checkbox).toBeChecked();
  });

  it('should select the box as unchecked depending upon check field', () => {
    mount(new TriStateCheckbox('Linux', resources));
    const checkbox = helper.find('input')[0];
    expect(checkbox).not.toBeChecked();
  });

  it('should select the box as indeterminate depending upon the isIndeterminate field', () => {
    mount(new TriStateCheckbox('Chrome', resources));
    const checkbox = helper.find('input')[0];
    expect(checkbox.indeterminate).toBe(true);
  });

  it('should disable the checkbox when disabled flag is set', () => {
    mount(new TriStateCheckbox('Chrome', resources, true));
    const checkbox = helper.find('input')[0];
    expect(checkbox).toHaveAttr('disabled');
  });

  it('should not disable the checkbox when disabled flag is not set', () => {
    mount(new TriStateCheckbox('Chrome', resources));
    const checkbox = helper.find('input')[0];
    expect(checkbox).not.toHaveAttr('disabled');
  });

  const mount = (triStateCheckbox) => {
    helper.mount(() => m(TriStateCheckboxWidget, {triStateCheckbox, 'index': 1}));
  };
});
