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

const $        = require('jquery');
const m        = require('mithril');
const mrequest = require('helpers/mrequest');
const Routes   = require('gen/js-routes');

const PipelineGroups = require('models/dashboard/pipeline_groups');
const Pipelines      = require('models/dashboard/pipelines');

const Dashboard = function (pipelineGroups, pipelines) {
  this.getPipelineGroups = () => pipelineGroups.groups;

  this.getPipelines = () => pipelines.pipelines;

  this.allPipelineNames = () => Object.keys(pipelines.pipelines);

  this.findPipeline = (pipelineName) => pipelines.find(pipelineName);

  //do not filter pipelines as it is accessed using pipeline group references
  this.filterBy = (filterText) => new Dashboard(pipelineGroups.filterBy(filterText), pipelines);
};

Dashboard.API_VERSION = 'v2';

Dashboard.fromJSON = (json) => {
  const pipelineGroups = PipelineGroups.fromJSON(json._embedded.pipeline_groups);

  const pipelines = Pipelines.fromJSON(json._embedded.pipelines);
  return new Dashboard(pipelineGroups, pipelines);
};

Dashboard.get = () => {
  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv2ShowDashboardPath(), //eslint-disable-line camelcase
      beforeSend:  mrequest.xhrConfig.forVersion(Dashboard.API_VERSION),
      contentType: false
    });

    jqXHR.then((data) => {
      deferred.resolve(data);
    });

    jqXHR.always(m.redraw);

  }).promise();
};

module.exports = Dashboard;
