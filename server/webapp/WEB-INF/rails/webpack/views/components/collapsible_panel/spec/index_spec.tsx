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

import {CollapsiblePanel} from "../index";
import * as styles from "../index.scss";

describe("Collapsible Panel Component", () => {
  const m = require("mithril");
  const simulateEvent = require("simulate-event");

  const pageTitle = "Test Header";
  const body = [<div class="collapse-content">This is body</div>];

  let $root: any, root: any;
  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });
  beforeEach(mount);

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render expand collapsible component", () => {
    expect(find('collapse-header')).toContainText(pageTitle);
    expect($root.find('.collapse-content')).toBeInDOM();
  });

  it("should render component, collapsed by default", () => {
    expect(find('collapse-header')).not.toHaveClass(styles.expanded);
    expect(find('collapse-body')).toHaveClass(styles.hide);
  });

  it("should toggle component state on click", () => {
    expect(find('collapse-header')).not.toHaveClass(styles.expanded);
    expect(find('collapse-body')).toHaveClass(styles.hide);

    simulateEvent.simulate(find('collapse-header').get(0), 'click');
    m.redraw();

    expect(find('collapse-header')).toHaveClass(styles.expanded);
    expect(find('collapse-body')).not.toHaveClass(styles.hide);

    simulateEvent.simulate(find('collapse-header').get(0), 'click');
    m.redraw();

    expect(find('collapse-header')).not.toHaveClass(styles.expanded);
    expect(find('collapse-body')).toHaveClass(styles.hide);
  });

  function mount() {
    m.mount(root, {
      view() {
        return (<CollapsiblePanel header={pageTitle}>{body}</CollapsiblePanel>);
      }
    });

    m.redraw(true);
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }
});
