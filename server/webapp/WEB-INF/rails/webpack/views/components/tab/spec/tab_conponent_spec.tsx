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

import * as m from "mithril";
import * as simulateEvent from "simulate-event";
import {Tabs} from "views/components/tab/index";
import * as styles from "../index.scss";

describe("TabComponent", () => {
  let $root: any, root: any;
  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);

  it("should render tab with first tab selected", () => {
    mount();

    expect(findByDataTestId("tab-header-0")).toBeInDOM();
    expect(findByDataTestId("tab-header-0")).toHaveText("One");
    expect(findByDataTestId("tab-header-0")).toHaveClass(styles.active);
    expect(findByDataTestId("tab-content-0")).toBeInDOM();
    expect(findByDataTestId("tab-content-0")).toBeVisible();
    expect(findByDataTestId("tab-content-0")).toHaveText("Content for one");

    expect(findByDataTestId("tab-header-1")).toBeInDOM();
    expect(findByDataTestId("tab-header-1")).toHaveText("Two");
    expect(findByDataTestId("tab-header-1")).not.toHaveClass(styles.active);
    expect(findByDataTestId("tab-content-1")).toBeInDOM();
    expect(findByDataTestId("tab-content-1")).toBeHidden();
    expect(findByDataTestId("tab-content-1")).toHaveText("Content for two");
  });

  it("should change tab on click of tab name", () => {
    mount();

    expect(findByDataTestId("tab-header-0")).toHaveClass(styles.active);
    expect(findByDataTestId("tab-content-0")).toBeVisible();
    expect(findByDataTestId("tab-header-1")).not.toHaveClass(styles.active);
    expect(findByDataTestId("tab-content-1")).toBeHidden();

    simulateEvent.simulate(findByDataTestId("tab-header-1").get(0), "click");
    m.redraw();

    expect(findByDataTestId("tab-header-0")).not.toHaveClass(styles.active);
    expect(findByDataTestId("tab-content-0")).toBeHidden();
    expect(findByDataTestId("tab-header-1")).toHaveClass(styles.active);
    expect(findByDataTestId("tab-content-1")).toBeVisible();
  });

  function mount() {
    m.mount(root, {
      view() {
        return (
          <Tabs tabs={["One", "Two"]} contents={["Content for one", "Content for two"]}/>);
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function findByDataTestId(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }
});
