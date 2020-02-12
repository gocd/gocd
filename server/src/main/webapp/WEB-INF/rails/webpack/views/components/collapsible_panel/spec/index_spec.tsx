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
import {CollapsiblePanel} from "../index";
import styles from "../index.scss";

describe("Collapsible Panel Component", () => {

  const pageTitle = "Test Header";
  const body      = [<div class="collapse-content">This is body</div>];
  const helper    = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render expand collapsible component", () => {
    mount();
    expect(helper.byTestId("collapse-header")).toContainText(pageTitle);
    expect(helper.q(".collapse-content")).toBeInDOM();
  });

  it("should render component, collapsed by default", () => {
    mount();
    expect(helper.byTestId("collapse-header")).not.toHaveClass(styles.expanded);
    expect(helper.byTestId("collapse-body")).toHaveClass(styles.hide);
  });

  it("should toggle component state on click", () => {
    mount();
    expect(helper.byTestId("collapse-header")).not.toHaveClass(styles.expanded);
    expect(helper.byTestId("collapse-body")).toHaveClass(styles.hide);

    helper.clickByTestId("collapse-header");

    expect(helper.byTestId("collapse-header")).toHaveClass(styles.expanded);
    expect(helper.byTestId("collapse-body")).not.toHaveClass(styles.hide);

    helper.clickByTestId("collapse-header");

    expect(helper.byTestId("collapse-header")).not.toHaveClass(styles.expanded);
    expect(helper.byTestId("collapse-body")).toHaveClass(styles.hide);
  });

  it("should apply error state", () => {
    mount(true);
    expect(helper.byTestId("collapsible-panel-wrapper")).toHaveClass(styles.error);
  });

  function mount(error?: boolean) {
    helper.mount(() => <CollapsiblePanel error={error} dataTestId={"collapsible-panel-wrapper"}
                                         header={pageTitle}>{body}</CollapsiblePanel>);
  }

});
