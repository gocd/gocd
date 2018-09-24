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

describe("Header Panel Component", () => {
  const m           = require("mithril");
  const HeaderPanel = require("views/components/header_panel");
  const styles      = require('views/components/header_panel/css/index.scss');

  let $root, root;

  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);

  it("should render header panel component", () => {
    const pageTitle = "Test Header";
    mount(pageTitle, []);

    expect($root.find(`.page-title h1`)).toContainText(pageTitle);
  });

  it("should render header panel along with buttons", () => {
    const pageTitle = "Test Header";
    const buttons   = [m("button", "Do something"), m("button", "Do something more")];
    mount(pageTitle, buttons);

    expect($root.find(`.page-title h1`)).toContainText(pageTitle);

    const pageActionButtons = $root.find(`.page-actions`).children();

    expect(pageActionButtons).toHaveLength(buttons.length);
    expect(pageActionButtons.get(0)).toContainText("Do something");
    expect(pageActionButtons.get(1)).toContainText("Do something more");
  });

  it("should not render buttons when no buttons are provided for header panel", () => {
    const pageTitle = "Test Header";
    mount(pageTitle);

    expect($root.find(`.page-title h1`)).toContainText(pageTitle);
    expect($root.find(`.page-actions`)).not.toBeInDOM();
  });

  function mount(pageTitle, buttons) {
    m.mount(root, {
      view() {
        return m(HeaderPanel, {
          title: pageTitle,
          buttons
        });
      }
    });

    m.redraw(true);
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }
});
