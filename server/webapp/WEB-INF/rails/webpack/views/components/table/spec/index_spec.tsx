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
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {Table} from "../index";

describe("Table component", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  const headers = ["Col1", "Col2", "Col3"];
  const data    = [[1, 2, ""], [true, "two", null]];

  it("should render table component", () => {
    mount();
    expect(helper.findByDataTestId("table")).toBeVisible();
    expect(helper.findByDataTestId("table-header-row").length).toEqual(1);
    expect(helper.findByDataTestId("table-row").length).toEqual(2);
  });

  it("should render headers and data", () => {
    mount();
    expect(helper.findByDataTestId("table-header-row")).toContainText("Col1");
    expect(helper.findByDataTestId("table-header-row")).toContainText("Col2");
    expect(helper.findByDataTestId("table-header-row")).toContainText("Col3");

    expect(helper.findByDataTestId("table-row").get(0)).toContainText("1");
    expect(helper.findByDataTestId("table-row").get(0)).toContainText("2");
    expect(helper.findByDataTestId("table-row").get(0)).toContainText("");

    expect(helper.findByDataTestId("table-row").get(1)).toContainText("true");
    expect(helper.findByDataTestId("table-row").get(1)).toContainText("two");
    expect(helper.findByDataTestId("table-row").get(1)).toContainText("");
  });

  function mount() {
    helper.mount(() => <Table headers={headers} data={data}/>);
  }
});
