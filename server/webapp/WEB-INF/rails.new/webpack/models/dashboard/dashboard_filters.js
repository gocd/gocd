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

const _               = require('lodash');
const DashboardFilter = require('models/dashboard/dashboard_filter');

const DashboardFilters = function (filters) {
  this.filters = _.map(filters, (filter) => {
    return new DashboardFilter(filter.name, filter.pipelines, filter.type);
  });

  this.getFilterNamed = (name) => {
    return _.find(this.filters, (filter) => { return filter.name === name; });
  };
};

module.exports = DashboardFilters;
