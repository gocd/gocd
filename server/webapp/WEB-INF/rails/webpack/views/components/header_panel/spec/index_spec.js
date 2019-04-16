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

import {HeaderPanel} from "../index";
import {TestHelper} from "../../../pages/spec/test_helper";

const m = require("mithril");

describe("Header Panel Component", () => {

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render header panel component", () => {
    const pageTitle = "Test Header";
    mount(pageTitle, []);
    expect(helper.findByDataTestId('title')).toContainText(pageTitle);
  });

  it("should render header panel along with buttons", () => {
    const pageTitle = "Test Header";
    const buttons   = [m("button", "Do something"), m("button", "Do something more")];
    mount(pageTitle, buttons);

    expect(helper.findByDataTestId('title')).toContainText(pageTitle);

    const pageActionButtons = helper.findByDataTestId('pageActions').children();

    expect(pageActionButtons).toHaveLength(buttons.length);
    expect(pageActionButtons.get(0)).toContainText("Do something");
    expect(pageActionButtons.get(1)).toContainText("Do something more");
  });

  it("should not render buttons when no buttons are provided for header panel", () => {
    const pageTitle = "Test Header";
    mount(pageTitle);

    expect(helper.findByDataTestId('title')).toContainText(pageTitle);
    expect(helper.findByDataTestId('pageActions')).not.toBeInDOM();
  });

  function mount(pageTitle, buttons) {
    helper.mount(() => m(HeaderPanel, {
      title: pageTitle, buttons
    }));
  }

});
