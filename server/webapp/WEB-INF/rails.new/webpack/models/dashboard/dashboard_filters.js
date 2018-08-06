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

const _ = require('lodash');

function DashboardFilters(filters) {
  const NAME_DEFAULT_FILTER = "Default";

  function defaultDef() {
    return {name: NAME_DEFAULT_FILTER, type: "blacklist", pipelines: []};
  }

  this.clone = function clone() {
    return new DashboardFilters(_.map(this.filters, (f) => _.cloneDeep(f)));
  };

  this.filters = filters;

  this.replaceFilter = function replaceFilter(oldName, updatedFilter) {
    const idx = index(this.filters, oldName);

    if (idx !== -1) {
      this.filters.splice(idx, 1, updatedFilter);

      // make sure there is always a default filter, even if we try to rename it.
      if (oldName.toLowerCase() === NAME_DEFAULT_FILTER.toLowerCase() && !matchName(NAME_DEFAULT_FILTER)(updatedFilter)) {
        this.filters.unshift(defaultDef());
      }
    } else {
      console.error(`Couldn't locate filter named [${oldName}]; this shouldn't happen. Falling back to append().`); // eslint-disable-line no-console
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

  this.defaultFilter = () => {
    const f = _.find(this.filters, matchName(NAME_DEFAULT_FILTER));

    if (!f) {
      const blank = defaultDef();
      this.filters.unshift(blank);
      return blank;
    }

    return f;
  };

  this.names = () => {
    return _.map(this.filters, "name");
  };

  this.findFilter = (name) => {
    return _.find(this.filters, matchName(name)) || this.defaultFilter();
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

module.exports = DashboardFilters;
