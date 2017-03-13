/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const m = require('mithril');

const SortOrder = function () {
  let sortBy  = 'agentState';
  let orderBy = 'asc';

  this.orderBy = () => orderBy;

  this.sortBy = () => sortBy;

  this.isSortedOn = (columnName) => sortBy === columnName;

  this.toggleSortingOrder = (newSortBy) => {
    if (sortBy !== newSortBy) {
      sortBy  = newSortBy;
      orderBy = 'asc';
    } else {
      orderBy = (orderBy === 'asc') ? 'desc' : 'asc';
    }
    this.perform();
  };

  this.initialize = () => {
    sortBy  = m.route.param('sortBy') || this.sortBy();
    orderBy = m.route.param('orderBy') || this.orderBy();

    this.perform();
  };

  this.perform = function () {
    m.route.set(`/${this.sortBy()}/${this.orderBy()}`);
  };
};

module.exports = SortOrder;
