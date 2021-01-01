/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import _ from "lodash";
import {PaginationJSON} from "models/agent_job_run_history";

export class Pagination {
  /** Max number of pages to show before truncating */
  static readonly MAX_PAGE_COUNT = 7;
  /** Number of sibling pages to show around the current page */
  static readonly SIBLING_RANGE  = 2;

  offset: number;
  total: number;
  pageSize: number;

  constructor(offset: number, total: number, pageSize: number) {
    this.offset   = offset;
    this.total    = total;
    this.pageSize = pageSize;
  }

  static fromJSON(json: PaginationJSON) {
    return new Pagination(json.offset, json.total, json.page_size);
  }

  getVisiblePageNumbers() {
    const range               = this.getRange();
    const possiblePageNumbers = range.sort((a, b) => a - b)
                                     .filter((value) => this.filter(value));

    return _.uniq(possiblePageNumbers);
  }

  totalNumberOfPages() {
    return Math.ceil(this.total / this.pageSize);
  }

  currentPageNumber() {
    return Math.ceil(this.offset / this.pageSize) + 1;
  }

  hasPreviousPage() {
    return this.currentPageNumber() > 1;
  }

  hasNextPage() {
    return this.currentPageNumber() < this.totalNumberOfPages();
  }

  previousPageNumber() {
    return this.currentPageNumber() - 1;
  }

  nextPageNumber() {
    return this.currentPageNumber() + 1;
  }

  private static createRangeInclusiveOfStartAndEnd(start: number, end: number) {
    return _.range(start, end + 1);
  }

  private getRange() {
    const totalPageCount    = this.totalNumberOfPages();
    const currentPageNumber = this.currentPageNumber();

    if (totalPageCount <= Pagination.MAX_PAGE_COUNT) {
      // show all pages upto `MAX_PAGE_COUNT`
      return [
        ...Pagination.createRangeInclusiveOfStartAndEnd(1, Pagination.MAX_PAGE_COUNT)
      ];
    } else if (currentPageNumber <= Pagination.SIBLING_RANGE * 2) {
      return [
        ...Pagination.createRangeInclusiveOfStartAndEnd(1, (Pagination.SIBLING_RANGE * 2)),
        totalPageCount];
    } else if (currentPageNumber >= totalPageCount - (Pagination.SIBLING_RANGE * 2)) {
      return [
        1,
        ...Pagination.createRangeInclusiveOfStartAndEnd(totalPageCount - (Pagination.SIBLING_RANGE * 2) - 1,
                                                        totalPageCount)];
    } else {
      return [
        1,
        ...Pagination.createRangeInclusiveOfStartAndEnd(currentPageNumber - Pagination.SIBLING_RANGE,
                                                        currentPageNumber + Pagination.SIBLING_RANGE),
        totalPageCount
      ];
    }
  }

  private filter(pageNumber: number) {
    return (pageNumber >= 1) && (pageNumber <= this.totalNumberOfPages());
  }
}
