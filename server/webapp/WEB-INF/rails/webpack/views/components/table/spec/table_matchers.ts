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

// tslint:disable
/// <reference path="./table_matchers.d.ts"/>

import * as _ from "lodash";

const addMatchers = require("add-matchers");

addMatchers({
  toContainHeaderCells: (headers: string[], table: HTMLTableElement) => {
    const tableHeaders = $(table).find("thead tr th").map((index, element) => {
      return $(element).text();
    });
    return _.isEqual(tableHeaders.toArray(), headers);
  }
});
