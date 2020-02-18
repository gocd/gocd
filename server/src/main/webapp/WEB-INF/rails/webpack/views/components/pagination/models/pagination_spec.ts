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

import _ from "lodash";
import {Pagination} from "views/components/pagination/models/pagination";

describe("Pagination", () => {

  _.range(1, Pagination.MAX_PAGE_COUNT + 1).forEach((currentPageNumber) => {
    it(`should show numbers 1-${Pagination.MAX_PAGE_COUNT} if number of pages is less than or equal to ${Pagination.MAX_PAGE_COUNT} and current page number is '${currentPageNumber}'`,
       () => {
         const pagination = new Pagination(10 * (currentPageNumber - 1), 10 * Pagination.MAX_PAGE_COUNT, 10);
         expect(pagination.getVisiblePageNumbers()).toEqual([1, 2, 3, 4, 5, 6, 7]);
       });

  });

  _.range(100 - Pagination.SIBLING_RANGE, 100 + 1).forEach((currentPageNumber) => {
    it(`should show first page, last page and last ${Pagination.SIBLING_RANGE - 1} numbers when selecting page ${currentPageNumber}`,
       () => {
         const pagination = new Pagination(10 * (currentPageNumber - 1), 1000, 10);

         expect(pagination.getVisiblePageNumbers()).toEqual([1, 95, 96, 97, 98, 99, 100]);
       });
  });

  it(
    "should show first page, last page, current page, with 2 leading and 2 trailing page numbers when current page number is somewhere in between",
    () => {
      const pagination = new Pagination(90, 1000, 10);

      expect(pagination.getVisiblePageNumbers()).toEqual([1, 8, 9, 10, 11, 12, 100]);
    });

});
