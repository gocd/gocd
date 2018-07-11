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

const Stream           = require('mithril/stream');
const DashboardFilters = require('models/dashboard/dashboard_filters');
const AjaxHelper       = require('helpers/ajax_helper');
const SparkRoutes      = require('helpers/spark_routes');

const Personalization = function (filters, pipelineGroups) {
  let filterSet       = new DashboardFilters(filters);
  this.pipelineGroups = Stream(pipelineGroups);

  this.names = () => filterSet.names();

  this.namedFilter = (name) => {
    return filterSet.findFilter(name);
  };

  this.addOrReplaceFilter = (existingName, newFilter) => {
    const filters = filterSet.clone();

    if (existingName) {
      filters.replaceFilter(existingName, newFilter);
    } else {
      filters.addFilter(newFilter);
    }

    return this.updateFilters(filters);
  };

  this.removeFilter = (name) => {
    const filters = filterSet.clone();
    filters.removeFilter(name);

    return this.updateFilters(filters);
  };

  this.updateFilters = (filters) => {
    if (!(filters instanceof DashboardFilters)) { filters = new DashboardFilters(filters); }

    return AjaxHelper.PUT({
      url:        SparkRoutes.pipelineSelectionPath(),
      apiVersion: 'v1',
      payload:     filters
    }).done(() => {
      filterSet = filters; // only changes local copy when successful
    });
  };
};

Personalization.fromJSON = (json) => {
  return new Personalization(json.filters, json.pipelines);
};

Personalization.get = () => {
  return AjaxHelper.GET({
    url:        SparkRoutes.pipelineSelectionPath(),
    type:       Personalization,
    apiVersion: 'v1'
  });
};

module.exports = Personalization;
