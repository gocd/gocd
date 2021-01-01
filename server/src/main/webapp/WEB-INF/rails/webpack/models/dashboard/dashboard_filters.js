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

export function DashboardFilters(filters) {
  this.clone = function clone() {
    return new DashboardFilters(_.map(this.filters, (f) => _.cloneDeep(f)));
  };

  this.filters = filters;

  this.replaceFilter = function replaceFilter(oldName, updatedFilter) {
    const idx = index(this.filters, oldName);

    if (idx !== -1) {
      this.filters.splice(idx, 1, updatedFilter);
    } else {
      console.warn(`Couldn't locate filter named [${oldName}]; this shouldn't happen. Falling back to append().`); // eslint-disable-line no-console
      this.addFilter(updatedFilter);
    }
  };

  this.removeFilter = (name) => {
    this.filters = _.reject(this.filters, matchName(name));
  };

  this.addFilter = (filter) => {
    this.filters.push(filter);
  };

  this.moveFilterByIndex = (from, to) => {
    validateIndex(this.filters, from);
    validateIndex(this.filters, to);

    move(this.filters, from, to);
  };

  this.defaultFilter = () => this.filters[0] ;

  this.names = () => {
    return _.map(this.filters, "name");
  };

  this.findFilter = (name) => {
    return _.cloneDeep(_.find(this.filters, matchName(name)) || this.defaultFilter());
  };
}

function move(arr, from, to) {
  if (from === to) { return; }

  arr.splice(to, 0, arr.splice(from, 1)[0]);
}

function validateIndex(arr, i) {
  if (i < 0 || i > arr.length - 1) { throw new RangeError(`Cannot resolve filter at index ${i}; out of bounds`); }
}

/** Resolves the index of element with name `name` in the array `arr` */
function index(arr, name) {
  return _.findIndex(arr, matchName(name));
}

function matchName(name) {
  if ("string" !== typeof name || !name) { return false; }

  const n = name.toLowerCase();
  return (f) => n === f.name.toLowerCase();
}

