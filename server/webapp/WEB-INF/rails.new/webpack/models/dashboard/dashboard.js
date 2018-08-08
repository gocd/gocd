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

const DashboardGroups = require('models/dashboard/dashboard_groups');
const Pipelines      = require('models/dashboard/pipelines');

function Dashboard() {
  const self               = this;
  let pipelineGroups       = DashboardGroups.fromJSON([]);
  let pipelines            = Pipelines.fromJSON([]);
  let filteredGroups       = pipelineGroups.filterBy('');
  const internalSearchText = Stream('');

  this.message           = Stream();
  this.getPipelineGroups = () => filteredGroups.groups;
  this.getPipelines      = () => pipelines.pipelines;
  this.allPipelineNames  = () => Object.keys(pipelines.pipelines);
  this.findPipeline      = (pipelineName) => pipelines.find(pipelineName);

  this.initialize = (json) => {
    const newPipelineGroups = DashboardGroups.fromJSON(_.get(json, '_embedded.pipeline_groups', []));
    const newPipelines      = Pipelines.fromJSON(_.get(json, '_embedded.pipelines', []));

    //set it on the current object only on a successful deserialization of both pipeline groups and pipelines
    pipelineGroups = newPipelineGroups;
    pipelines      = newPipelines;

    filteredGroups = pipelineGroups.filterBy(internalSearchText());
  };

  const performSearch = _.debounce(() => {
    self._performRouting();
    filteredGroups = pipelineGroups.filterBy(internalSearchText());
    m.redraw();
  }, 200);

  //needed on model for stubbing in tests
  this._performRouting = () => {
    m.route.set(`/${internalSearchText()}`);
  };

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
}

Dashboard.API_VERSION = 'v2';

Dashboard.get = (viewName, etag) => {
  return AjaxHelper.GET({
    url:        Routes.apiv2ShowDashboardPath({viewName}),
    apiVersion: Dashboard.API_VERSION,
    etag
  });
};

module.exports = Dashboard;
