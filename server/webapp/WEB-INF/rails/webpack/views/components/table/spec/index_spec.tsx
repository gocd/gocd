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

import * as m from "mithril";
import {Table} from "../index";

describe("Table component", () => {
  let $root: any, root: Element;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();

  });

  afterEach(unmount);

  const headers = ["Col1", "Col2", "Col3"];
  const data = [[1, 2, ''], [true, "two", null]];

  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render table component", () => {
    mount();
    expect(find("table")).toBeVisible();
    expect(find("table-header-row").length).toEqual(1);
    expect(find("table-row").length).toEqual(2);
  });

  it("should render headers and data", () => {
    mount();
    expect(find("table-header-row")).toContainText("Col1");
    expect(find("table-header-row")).toContainText("Col2");
    expect(find("table-header-row")).toContainText("Col3");

    expect(find("table-row").get(0)).toContainText("1");
    expect(find("table-row").get(0)).toContainText("2");
    expect(find("table-row").get(0)).toContainText("");

    expect(find("table-row").get(1)).toContainText("true");
    expect(find("table-row").get(1)).toContainText("two");
    expect(find("table-row").get(1)).toContainText("");
  });

  function mount() {
    m.mount(root, {
      view() {
        return (<Table headers={headers} data={data} />);
      }
    });

    // @ts-ignore
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
