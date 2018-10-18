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

describe("Expand Collapsible Component", () => {
  const _                = require('lodash');
  const m                = require("mithril");
  const simulateEvent    = require("simulate-event");
  const CollapsiblePanel = require("views/components/collapsible_panel");

  const pageTitle = "Test Header";
  const body      = [<div class="collapse-content">This is body</div>];

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  beforeEach(mount);

  afterEach(unmount);
  afterEach(window.destroyDomElementForTest);

  it("should render expand collapsible component", () => {
    expect(find('collapse_header')).toContainText(pageTitle);
    expect($root.find('.collapse-content')).toBeInDOM();
  });

  it("should render component, collapsed by default", () => {
    const headerClasses = find('collapse_header').get(0).classList;
    const bodyClasses   = find('collapse_body').get(0).classList;

    expect(containsClass(headerClasses, "collapsed")).toBe(true);
    expect(containsClass(bodyClasses, "hide")).toBe(true);
  });

  it("should toggle component state on click", () => {
    expect(containsClass(find('collapse_header').get(0).classList, "collapsed")).toBe(true);
    expect(containsClass(find('collapse_header').get(0).classList, "expanded")).toBe(false);
    expect(containsClass(find('collapse_body').get(0).classList, "hide")).toBe(true);
    expect(containsClass(find('collapse_body').get(0).classList, "show")).toBe(false);

    simulateEvent.simulate(find('collapse_header').get(0), 'click');
    m.redraw();

    expect(containsClass(find('collapse_header').get(0).classList, "collapsed")).toBe(false);
    expect(containsClass(find('collapse_header').get(0).classList, "expanded")).toBe(true);
    expect(containsClass(find('collapse_body').get(0).classList, "hide")).toBe(false);
    expect(containsClass(find('collapse_body').get(0).classList, "show")).toBe(true);

    simulateEvent.simulate(find('collapse_header').get(0), 'click');
    m.redraw();

    expect(containsClass(find('collapse_header').get(0).classList, "collapsed")).toBe(true);
    expect(containsClass(find('collapse_header').get(0).classList, "expanded")).toBe(false);
    expect(containsClass(find('collapse_body').get(0).classList, "hide")).toBe(true);
    expect(containsClass(find('collapse_body').get(0).classList, "show")).toBe(false);
  });


  function mount() {
    m.mount(root, {
      view() {
        return m(CollapsiblePanel, {
          header: pageTitle
        }, body);
      }
    });

    m.redraw(true);
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id) {
    return $root.find(`[data-test-id='${id}']`);
  }

  function containsClass(classList, expectedClass) {
    return _.some(classList, (c) => _.includes(c, expectedClass))
  }
});
