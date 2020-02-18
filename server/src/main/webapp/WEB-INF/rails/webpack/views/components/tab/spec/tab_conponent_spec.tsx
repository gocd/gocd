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
import {Tabs} from "views/components/tab/index";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../index.scss";

describe("TabComponent", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render tab with first tab selected", () => {
    mount();

    expect(helper.byTestId("tab-header-0")).toBeInDOM();
    expect(helper.byTestId("tab-header-0")).toHaveText("One");
    expect(helper.byTestId("tab-header-0")).toHaveClass(styles.active);
    expect(helper.byTestId("tab-content-0")).toBeInDOM();
    expect(helper.byTestId("tab-content-0")).toBeVisible();
    expect(helper.byTestId("tab-content-0")).toHaveText("Content for one");

    expect(helper.byTestId("tab-header-1")).toBeInDOM();
    expect(helper.byTestId("tab-header-1")).toHaveText("Two");
    expect(helper.byTestId("tab-header-1")).not.toHaveClass(styles.active);
    expect(helper.byTestId("tab-content-1")).toBeInDOM();
    expect(helper.byTestId("tab-content-1")).toBeHidden();
    expect(helper.byTestId("tab-content-1")).toHaveText("Content for two");
  });

  it("should change tab on click of tab name", () => {
    mount();

    expect(helper.byTestId("tab-header-0")).toHaveClass(styles.active);
    expect(helper.byTestId("tab-content-0")).toBeVisible();
    expect(helper.byTestId("tab-header-1")).not.toHaveClass(styles.active);
    expect(helper.byTestId("tab-content-1")).toBeHidden();

    helper.clickByTestId("tab-header-1");

    expect(helper.byTestId("tab-header-0")).not.toHaveClass(styles.active);
    expect(helper.byTestId("tab-content-0")).toBeHidden();
    expect(helper.byTestId("tab-header-1")).toHaveClass(styles.active);
    expect(helper.byTestId("tab-content-1")).toBeVisible();
  });

  function mount() {
    helper.mount(() => <Tabs tabs={["One", "Two"]} contents={["Content for one", "Content for two"]}/>);
  }
});
