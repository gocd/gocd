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
import m from "mithril";
import Stream from "mithril/stream";
import * as simulateEvent from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import {Table, TableSortHandler} from "../index";
import styles from "../index.scss";

describe("TableComponent", () => {
  const helper  = new TestHelper();
  const headers = ["Col1", "Col2", <lable>Col3</lable>];
  const data    = [[1, 2, "something"], [true, "two", null]];

  afterEach(helper.unmount.bind(helper));

  it("should render table component", () => {
    mount(headers, data);
    expect(helper.find(`[data-test-id='${"table"}']`)).toBeVisible();
    expect(helper.find(`[data-test-id='${"table-header-row"}']`).length).toEqual(1);
    expect(helper.find(`[data-test-id='${"table-row"}']`).length).toEqual(2);
  });

  it("should render headers and data", () => {
    mount(headers, data);
    expect(helper.find(`[data-test-id='${"table-header-row"}']`)).toContainText("Col1");
    expect(helper.find(`[data-test-id='${"table-header-row"}']`)).toContainText("Col2");
    expect(helper.find(`[data-test-id='${"table-header-row"}']`)).toContainText("Col3");

    expect(helper.find(`[data-test-id='${"table-row"}']`).get(0)).toContainText("1");
    expect(helper.find(`[data-test-id='${"table-row"}']`).get(0)).toContainText("2");
    expect(helper.find(`[data-test-id='${"table-row"}']`).get(0)).toContainText("");

    expect(helper.find(`[data-test-id='${"table-row"}']`).get(1)).toContainText("true");
    expect(helper.find(`[data-test-id='${"table-row"}']`).get(1)).toContainText("two");
    expect(helper.find(`[data-test-id='${"table-row"}']`).get(1)).toContainText("");
  });

  describe("Sort", () => {
    const initialOrderOfData = "AZMCXNBYL";
    const ascendingOrder     = "AZMBYLCXN";
    const descendingOrder    = "CXNBYLAZM";

    it("should have sort button for first column", () => {
      const testData = getTestData();
      mount(["Col-1", "Col-2", "Col-3"], testData(), new TestTableSortHandler(testData, [0]));

      expect(helper.find(`.${styles.sortButton}`)).toHaveLength(1);
      expect(helper.find(`.${styles.sortButton}`).parent()).toHaveText("Col-1");
    });

    it("should call handler to sort data", () => {
      const testData = getTestData();
      mount(["Col-1", "Col-2", "Col-3"], testData(), new TestTableSortHandler(testData, [0]));

      expect(helper.find(`[data-test-id='${"table-row"}']`)).toHaveText(initialOrderOfData);

      simulateEvent.simulate(helper.find(`.${styles.sortableColumn}`).get(0), "click");
      m.redraw.sync();

      expect(helper.find(`[data-test-id='${"table-row"}']`)).toHaveText(ascendingOrder);
    });

    it("should reverse the sorted data if user click's twice on the sort button", () => {
      const testData = getTestData();
      mount(["Col-1", "Col-2", "Col-3"], testData(), new TestTableSortHandler(testData, [0]));

      expect(helper.find(`[data-test-id='${"table-row"}']`)).toHaveText(initialOrderOfData);

      simulateEvent.simulate(helper.find(`.${styles.sortableColumn}`).get(0), "click");
      m.redraw.sync();

      expect(helper.find(`[data-test-id='${"table-row"}']`)).toHaveText(ascendingOrder);

      simulateEvent.simulate(helper.find(`.${styles.sortableColumn}`).get(0), "click");
      m.redraw.sync();

      expect(helper.find(`[data-test-id='${"table-row"}']`)).toHaveText(descendingOrder);
    });

    it("should highlight current sorted column", () => {
      const testData = getTestData();
      mount(["Col-1", "Col-2", "Col-3"], testData(), new TestTableSortHandler(testData, [0, 1]));
      expect(helper.find(`.${styles.sortButton}`)).toHaveClass(styles.inActive);

      simulateEvent.simulate(helper.find(`.${styles.sortableColumn}`).get(0), "click");
      m.redraw.sync();

      expect(helper.find(`.${styles.sortButton}`).eq(0)).not.toHaveClass(styles.inActive);
      expect(helper.find(`.${styles.sortButton}`).eq(1)).toHaveClass(styles.inActive);

      simulateEvent.simulate(helper.find(`.${styles.sortableColumn}`).get(1), "click");
      m.redraw.sync();

      expect(helper.find(`.${styles.sortButton}`).eq(0)).toHaveClass(styles.inActive);
      expect(helper.find(`.${styles.sortButton}`).eq(1)).not.toHaveClass(styles.inActive);
    });
  });

  describe("Draggable", () => {
    it("should have a drag icon for each row if draggable to set to true", () => {
      mount(headers, getTestData()(), undefined, true);

      expect(helper.find(`.${styles.draggable}`)).toBeInDOM();

      expect(helper.find(`.${styles.dragIcon}`)).toBeInDOM();
      expect(helper.find(`.${styles.dragIcon}`).length).toBe(3);
    });

    it("should not have a drag icon for each row if draggable is set to false", () => {
      mount(headers, getTestData()(), undefined, false);

      expect(helper.find(`.${styles.draggable}`)).not.toBeInDOM();
      expect(helper.find(`.${styles.dragIcon}`)).not.toBeInDOM();
    });

    it("should give a callback on dragover", () => {
      const spy = jasmine.createSpy();
      helper.mount(() => <Table headers={headers} data={testdata()} draggable={true} dragHandler={spy}/>);

      if (/(MSIE|Trident|Edge|Safari)/i.test(navigator.userAgent)) {
        return;
      }

      const rowFirst  = helper.find("tr[data-id=\"0\"]").get(0);
      const rowSecond = helper.find("tr[data-id=\"1\"]").get(0);

      expect(rowFirst.textContent).toBe("AZM");
      expect(rowSecond.textContent).toBe("CXN");

      const dataTransfer         = new DataTransfer();
      dataTransfer.effectAllowed = "move";
      rowFirst.dispatchEvent(new DragEvent("dragstart", {
        dataTransfer
      }));
      m.redraw.sync();
      rowSecond.dispatchEvent(new DragEvent("dragover"));
      m.redraw.sync();

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith(0, 1);
      expect(helper.find(`.${styles.draggableOver}`)).toBeInDOM();

      rowFirst.dispatchEvent(new DragEvent("dragend"));
      m.redraw.sync();

      expect(rowFirst.textContent).toBe("CXN");
      expect(rowSecond.textContent).toBe("AZM");
    });

  });

  function mount(headers: any, data: any, sortHandler?: TableSortHandler, draggable?: boolean) {
    helper.mount(() => <Table headers={headers} data={data} sortHandler={sortHandler} draggable={draggable}/>);
  }
});

function getTestData() {
  return Stream(
    [
      ["A", "Z", "M"],
      ["C", "X", "N"],
      ["B", "Y", "L"],
    ]);
}

const testdata = Stream([
                          ["A", "Z", "M"],
                          ["C", "X", "N"],
                          ["B", "Y", "L"]]);

class TestTableSortHandler implements TableSortHandler {
  private data: Stream<any[]>;
  private readonly columns: number[];
  private sortOrders                       = new Map();
  private currentSortedColumnIndex: number = 0;

  constructor(data: Stream<any[]>, columns: number[]) {
    this.data    = data;
    this.columns = columns;
    columns.forEach((c) => this.sortOrders.set(c, -1));
  }

  onColumnClick(columnIndex: number): void {
    this.currentSortedColumnIndex = columnIndex;
    this.sortOrders.set(columnIndex, this.sortOrders.get(columnIndex) * -1);
    this.data()
        .sort((element1, element2) => TestTableSortHandler.compare(element1,
                                                                   element2,
                                                                   columnIndex) * this.sortOrders.get(
          columnIndex));
  }

  getSortableColumns(): number[] {
    return this.columns;
  }

  getCurrentSortedColumnIndex(): number {
    return this.currentSortedColumnIndex;
  }

  private static compare(element1: any, element2: any, index: number) {
    return element1[index] < element2[index] ? -1 : element1[index] > element2[index] ? 1 : 0;
  }
}
