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
import * as stream from "mithril/stream";
import * as simulateEvent from "simulate-event";
import {Comparator, Table, TableHeader} from "../index";
import * as styles from "../index.scss";

describe("TableComponent", () => {
  let $root: any, root: Element;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();

  });

  afterEach(unmount);

  const headers = ["Col1", "Col2", <lable>Col3</lable>];
  const data    = [[1, 2, "something"], [true, "two", null]];

  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render table component", () => {
    mount(headers, data);
    expect(findByDataTestId("table")).toBeVisible();
    expect(findByDataTestId("table-header-row").length).toEqual(1);
    expect(findByDataTestId("table-row").length).toEqual(2);
  });

  it("should render headers and data", () => {
    mount(headers, data);
    expect(findByDataTestId("table-header-row")).toContainText("Col1");
    expect(findByDataTestId("table-header-row")).toContainText("Col2");
    expect(findByDataTestId("table-header-row")).toContainText("Col3");

    expect(findByDataTestId("table-row").get(0)).toContainText("1");
    expect(findByDataTestId("table-row").get(0)).toContainText("2");
    expect(findByDataTestId("table-row").get(0)).toContainText("");

    expect(findByDataTestId("table-row").get(1)).toContainText("true");
    expect(findByDataTestId("table-row").get(1)).toContainText("two");
    expect(findByDataTestId("table-row").get(1)).toContainText("");
  });

  describe("ColumnWidth", () => {
    it("should not apply column width if not specified", () => {
      mount([<TableHeader name={"Name"}/>, <TableHeader name={"Release Date"}/>], [[]]);

      expect(findByTag("th")).not.toHaveAttr("width");
    });

    it("should apply column width in pixel", () => {
      mount([<TableHeader name={"Name"} width="100px"/>, <TableHeader name={"Release Date"}/>], [[]]);

      expect(findByTag("th")).toHaveAttr("width", "100px");
    });

    it("should apply column width in percentage", () => {
      mount([<TableHeader name={"Name"} width="20%"/>, <TableHeader name={"Release Date"}/>], [[]]);

      expect(findByTag("th")).toHaveAttr("width", "20%");
    });
  });

  describe("Sort", () => {
    const initialOrderOfData = "AZMCXNBYL";
    const ascendingOrder     = "AZMBYLCXN";
    const descendingOrder    = "CXNBYLAZM";

    it("should have sort button for first column", () => {
      const testData = getTestData();
      mount(headersWithSortCapability(testData), testData());

      expect(findByClass(styles.sortButton)).toHaveLength(1);
      expect(findByClass(styles.sortButton).parent()).toHaveText("Col-1");
    });

    it("should sort data based on comparator", () => {
      const testData = getTestData();
      mount(headersWithSortCapability(testData), testData());

      expect(findByDataTestId("table-row")).toHaveText(initialOrderOfData);

      simulateEvent.simulate(findByClass(styles.sortButton).get(0), "click");
      m.redraw();

      expect(findByDataTestId("table-row")).toHaveText(ascendingOrder);
    });

    it("should reverse the sorted data if user click's twice on the sort button", () => {
      const testData = getTestData();
      mount(headersWithSortCapability(testData), testData());

      expect(findByDataTestId("table-row")).toHaveText(initialOrderOfData);

      simulateEvent.simulate(findByClass(styles.sortButton).get(0), "click");
      m.redraw();

      expect(findByDataTestId("table-row")).toHaveText(ascendingOrder);

      simulateEvent.simulate(findByClass(styles.sortButton).get(0), "click");
      m.redraw();

      expect(findByDataTestId("table-row")).toHaveText(descendingOrder);
    });
  });

  function mount(headers: any, data: any) {
    m.mount(root, {
      view() {
        return (<Table headers={headers} data={data}/>);
      }
    });

    // @ts-ignore
    m.redraw(true);
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function findByDataTestId(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  function findByClass(className: string) {
    return $root.find(`.${className}`);
  }

  function findByTag(tag: string) {
    return $root.find(tag);
  }
});

function getTestData() {
  return stream(
    [
      ["A", "Z", "M"],
      ["C", "X", "N"],
      ["B", "Y", "L"],
    ]);
}

class TestComparator extends Comparator<string[]> {
  constructor(testData: any) {
    super(testData);
  }

  compare(element1: string[], element2: string[]): number {
    return element1[0] < element2[0] ? -1 : element1[0] > element2[0] ? 1 : 0;
  }
}

function headersWithSortCapability(testData: any) {
  return [
    <TableHeader name={"Col-1"} comparator={new TestComparator(testData)} width="10%"/>,
    <TableHeader name={"Col-2"}/>,
    <TableHeader name={"Col-3"}/>];
}
