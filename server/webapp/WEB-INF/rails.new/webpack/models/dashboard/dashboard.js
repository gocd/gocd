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

const _          = require('lodash');
const m          = require('mithril');
const Stream     = require('mithril/stream');
const AjaxHelper = require('helpers/ajax_helper');
const Routes     = require('gen/js-routes');

const PipelineGroups = require('models/dashboard/pipeline_groups');
const Pipelines      = require('models/dashboard/pipelines');

const Dashboard = function () {
  let pipelineGroups, pipelines;
  let filteredGroups;
  const internalSearchText = Stream('');

  this.getPipelineGroups = () => filteredGroups.groups;
  this.getPipelines      = () => pipelines.pipelines;
  this.allPipelineNames  = () => Object.keys(pipelines.pipelines);
  this.findPipeline      = (pipelineName) => pipelines.find(pipelineName);

  this.initialize = (json) => {
    pipelineGroups = PipelineGroups.fromJSON(json._embedded.pipeline_groups);
    pipelines      = Pipelines.fromJSON(json._embedded.pipelines);
    filteredGroups = pipelineGroups.filterBy(internalSearchText());
  };

  const performSearch = _.debounce(() => {
    filteredGroups = pipelineGroups.filterBy(internalSearchText());
    m.redraw();
  }, 200);

  //Stream API with filtering capability
  this.searchText = (searchedBy) => {
    if (searchedBy !== undefined) {
      searchedBy = searchedBy.toLowerCase();
      internalSearchText(searchedBy);
      performSearch();
    } else {
      return internalSearchText();
    }
  };
};

Dashboard.API_VERSION = 'v2';

Dashboard.get = () => {
  return AjaxHelper.GET({
    url:        Routes.apiv2ShowDashboardPath(), //eslint-disable-line camelcase
    apiVersion: Dashboard.API_VERSION
  });
};

module.exports = Dashboard;
