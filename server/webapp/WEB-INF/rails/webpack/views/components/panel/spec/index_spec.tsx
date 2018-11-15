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

import {Panel} from "../index";

describe("Panel Component", () => {
  const m         = require("mithril");
  const pageTitle = "Panel Header";
  const body      = [<div class="panel-content">This is body</div>];

  let $root: any, root: any;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });
  beforeEach(mount);

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function mount() {
    m.mount(root, {
      view() {
        return (<Panel header={pageTitle} actions={[<button>Click Me!</button>]}>{body}</Panel>);
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

  it("should render panel component with header and body", () => {
    expect(find("panel-header")).toBeInDOM();
    expect(find("panel-body")).toBeInDOM();
  });

  it("should render panel header with header and actions", () => {
    expect(find("panel-header")).toContainText(pageTitle);
    expect(find("panel-header-actions")).toBeInDOM();
    expect(find("panel-header-actions")).toContainHtml("<button>Click Me!</button>");
  });

  it("should render panel body with content", () => {
    expect(find("panel-body")).toContainText("This is body");
  });
});
