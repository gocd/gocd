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
import {PaginationWidget} from "views/components/pagination/index";
import {Pagination} from "views/components/pagination/models/pagination";
import {TestHelper} from "views/pages/spec/test_helper";
import style from "../index.scss";

describe("Pagination Widget", () => {
  const helper = new TestHelper();
  let onPageChange: (newPageNumber: number) => void;
  let pagination: Pagination;

  beforeEach(() => {
    onPageChange = jasmine.createSpy("onPageChange");
    pagination   = new Pagination(0, 1000, 10);
    mount(pagination);
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render page numbers as per provided pagination model", () => {
    expect(helper.byTestId("pagination-showing-1-of-100")).toBeInDOM();
    expect(helper.textByTestId("pagination-previous-page")).toEqual("Previous");
    expect(helper.textByTestId("pagination-page-1")).toEqual("1");
    expect(helper.textByTestId("pagination-page-2")).toEqual("2");
    expect(helper.textByTestId("pagination-page-3")).toEqual("3");
    expect(helper.textByTestId("pagination-page-4")).toEqual("4");
    expect(helper.textByTestId("pagination-page-dummy-spacer")).toEqual("...");
    expect(helper.textByTestId("pagination-page-100")).toEqual("100");
    expect(helper.textByTestId("pagination-next-page")).toEqual("Next");
  });

  it("should not allow clicking previous link when on the first page", () => {
    helper.clickByTestId("pagination-previous-page");
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it("should not allow clicking next link when on the last page", () => {
    pagination.offset = pagination.total - pagination.pageSize;
    helper.redraw();
    helper.clickByTestId("pagination-next-page");
    expect(onPageChange).not.toHaveBeenCalled();
  });

  it("should go to the next page on clicking next link", () => {
    helper.clickByTestId("pagination-next-page");
    expect(onPageChange).toHaveBeenCalledWith(2);
  });

  it("should go to the specific page on clicking the page link", () => {
    helper.clickByTestId("pagination-page-2");
    expect(onPageChange).toHaveBeenCalledWith(2);

    helper.clickByTestId("pagination-page-3");
    expect(onPageChange).toHaveBeenCalledWith(3);

    helper.clickByTestId("pagination-page-4");
    expect(onPageChange).toHaveBeenCalledWith(4);
  });

  it('should not render when has only one page', () => {
    helper.unmount();
    mount(new Pagination(0, 10, 10));

    expect(helper.byClass(style.paginationContainer)).not.toBeInDOM();
  });

  it('should render when more than one page', () => {
    helper.unmount();
    mount(new Pagination(0, 11, 10));

    expect(helper.byClass(style.paginationContainer)).toBeInDOM();
  });

  it('current page should have attribute data-test-current-page with value true', () => {
    pagination.offset = pagination.pageSize;
    m.redraw.sync();

    expect(helper.byTestId("pagination-page-2")).toHaveAttr("data-test-current-page");
    expect(helper.byTestId("pagination-page-3")).not.toHaveAttr("data-test-current-page");
    expect(helper.byTestId("pagination-page-4")).not.toHaveAttr("data-test-current-page");

    pagination.offset = pagination.pageSize * 2;
    m.redraw.sync();
    expect(helper.byTestId("pagination-page-2")).not.toHaveAttr("data-test-current-page");
    expect(helper.byTestId("pagination-page-3")).toHaveAttr("data-test-current-page");
    expect(helper.byTestId("pagination-page-4")).not.toHaveAttr("data-test-current-page");
  });

  function mount(pagination: Pagination) {
    helper.mount(() => {
      return <PaginationWidget pagination={pagination} onPageChange={onPageChange}/>;
    });
  }
});
