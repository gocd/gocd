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
import {TestHelper} from "views/pages/spec/test_helper";
import {HeaderPanel} from "../index";

describe("Header Panel Component", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render header panel component", () => {
    const pageTitle = "Test Header";
    mount(pageTitle, []);
    expect(helper.textByTestId("title")).toContain(pageTitle);
    expect(helper.byTestId("help-text-wrapper")).not.toBeInDOM();
  });

  it("should render header panel along with buttons", () => {
    const pageTitle = "Test Header";
    const buttons   = [m("button", "Do something"), m("button", "Do something more")];
    mount(pageTitle, buttons);

    expect(helper.textByTestId("title")).toContain(pageTitle);

    const pageActionButtons = helper.byTestId("pageActions").children;

    expect(pageActionButtons).toHaveLength(buttons.length);
    expect(pageActionButtons.item(0)).toContainText("Do something");
    expect(pageActionButtons.item(1)).toContainText("Do something more");
  });

  it("should not render buttons when no buttons are provided for header panel", () => {
    const pageTitle = "Test Header";
    mount(pageTitle);

    expect(helper.textByTestId("title")).toContain(pageTitle);
    expect(helper.byTestId("pageActions")).toBeFalsy();
  });

  it("should render key value pair when specified", () => {
    mount("Page Header", null, {
      Pipeline: "Up42",
      Instance: "3",
      Stage: "Up42_StAgE"
    });

    expect(helper.allByTestId("page-header-key-value-pair")).toHaveLength(3);

    expect(helper.byTestId("key-value-key-pipeline")).toHaveText("Pipeline");
    expect(helper.byTestId("key-value-value-pipeline")).toHaveText("Up42");

    expect(helper.byTestId("key-value-key-instance")).toHaveText("Instance");
    expect(helper.byTestId("key-value-value-instance")).toHaveText("3");

    expect(helper.byTestId("key-value-key-stage")).toHaveText("Stage");
    expect(helper.byTestId("key-value-value-stage")).toHaveText("Up42_StAgE");
  });

  it('should render help icon when help text is provided', () => {
    mount("Title", [], undefined, "Some help text");

    expect(helper.byTestId("help-text-wrapper")).toBeInDOM();
  });

  function mount(pageTitle: m.Children, buttons?: m.Children, keyValuePair?: { [key: string]: m.Children }, help?: m.Children) {
    helper.mount(() => <HeaderPanel buttons={buttons} title={pageTitle} keyValuePair={keyValuePair} help={help}/>);
  }
});
