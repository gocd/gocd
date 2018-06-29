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

const _                = require('lodash');
const Stream           = require('mithril/stream');
const DashboardFilterCollection = require('models/dashboard/dashboard_filter_collection');
const DashboardFilter  = require('models/dashboard/dashboard_filter');
const AjaxHelper       = require('helpers/ajax_helper');
const SparkRoutes      = require('helpers/spark_routes');

const PipelineSelection = function (pipelineGroups, filters) {
  const self = this;

  this.pipelineGroups = pipelineGroups;
  this.filters        = filters;
  this.currentFilter  = this.filters.defaultFilter();
  this.selections     = this.currentFilter.findSelections(this.pipelineGroups());

  this.isPipelineSelected = (pipelineName) => self.selections[pipelineName]();

  this.displayNames = () => {
    return this.filters.displayNames();
  };

  this.currentFilterName = () => {
    return this.currentFilter.name;
  };

  //NOTE: temp method for dev purposes
  this.cloneCurrentWithName = (name) => {
    if (!_.includes(this.filters.names(), name)) {
      const f = new DashboardFilter(name, this.currentFilter.pipelines, this.currentFilter.type);
      this.filters.filters.push(f);
      this.currentFilter = f;
    }
  };

  this.setCurrentFilter = (filterName) => {
    this.currentFilter = this.filters.getFilterNamed(filterName);
    this.selections = this.currentFilter.findSelections(this.pipelineGroups());
  };

  this.toggleBlacklist = () => {
    this.currentFilter.toggleType();
    this.currentFilter.invertPipelines(this.selections);
  };

  this.blacklist = () => {
    return this.currentFilter.isBlacklist();
  };

  this.addSelection = (pipelineName) => {
    if (this.blacklist()) {
      this.currentFilter.removePipeline(pipelineName);
    } else {
      this.currentFilter.pipelines.push(pipelineName);
    }
    this.selections[pipelineName](true);
  };

  this.removeSelection = (pipelineName) => {
    if (!this.blacklist()) {
      this.currentFilter.removePipeline(pipelineName);
    } else {
      this.currentFilter.pipelines.push(pipelineName);
    }
    this.selections[pipelineName](false);
  };

  this.toggleSelection = (pipelineName) => {
    if (_.includes(this.currentFilter.pipelines, pipelineName)) {
      this.currentFilter.removePipeline(pipelineName);
    } else {
      this.currentFilter.pipelines.push(pipelineName);
    }
    this.selections[pipelineName](!this.selections[pipelineName]());
  };

  this.selectAll = () => {
    if (this.blacklist()) {
      this.currentFilter.clearPipelines();
    } else {
      this.currentFilter.pipelines = _.keys(this.selections);
    }
    _.each(_.keys(this.selections), (pipelineName) => {
      this.selections[pipelineName](true);
    });
  };

  this.unselectAll = () => {
    if (this.blacklist()) {
      this.currentFilter.pipelines = _.keys(this.selections);
    } else {
      this.currentFilter.clearPipelines();
    }
    _.each(_.keys(this.selections), (pipelineName) => {
      this.selections[pipelineName](false);
    });
  };

  this.update = () => {
    return AjaxHelper.PUT({
      url:        SparkRoutes.pipelineSelectionPath(),
      apiVersion: 'v1',
      payload:     this.filters
    });
  };
};

PipelineSelection.fromJSON = (json) => {
  const pipelineGroups = json.pipelines;
  const filters        = new DashboardFilterCollection(json.filters);
  return new PipelineSelection(Stream(pipelineGroups), filters);
};

PipelineSelection.get = () => {
  return AjaxHelper.GET({
    url:        SparkRoutes.pipelineSelectionPath(),
    type:       PipelineSelection,
    apiVersion: 'v1'
  });
};

module.exports = PipelineSelection;
