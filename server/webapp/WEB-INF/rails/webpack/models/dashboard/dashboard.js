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
const Stream     = require('mithril/stream');
import AjaxHelper from "helpers/ajax_helper";
const Routes     = require('gen/js-routes');

const DashboardGroups = require('models/dashboard/dashboard_groups');
const Pipelines       = require('models/dashboard/pipelines');

function Dashboard() {
  let pipelineGroups = DashboardGroups.fromPipelineGroupsJSON([]);
  let environments   = DashboardGroups.fromEnvironmentsJSON([]);
  let pipelines      = Pipelines.fromJSON([]);

  this.message           = Stream();
  this.getPipelineGroups = () => pipelineGroups;
  this.getEnvironments   = () => environments;

  this.getPipelines      = () => pipelines.pipelines;
  this.allPipelineNames  = () => Object.keys(pipelines.pipelines);
  this.findPipeline      = (pipelineName) => pipelines.find(pipelineName);

  this.initialize = (json) => {
    const newPipelineGroups = DashboardGroups.fromPipelineGroupsJSON(_.get(json, '_embedded.pipeline_groups', []));
    const newEnvironments   = DashboardGroups.fromEnvironmentsJSON(_.get(json, '_embedded.environments', []));
    const newPipelines      = Pipelines.fromJSON(_.get(json, '_embedded.pipelines', []));

    const pipelinesNoEnv = _.difference(Object.keys(newPipelines.pipelines), _.reduce(newEnvironments.groups, (memo, group) => memo.concat(group.pipelines), []));
    newEnvironments.groups.push(new DashboardGroups.Environment({name: null, can_administer: false, pipelines: pipelinesNoEnv})); // eslint-disable-line camelcase

    //set it on the current object only on a successful deserialization of both pipeline groups and pipelines
    pipelineGroups = newPipelineGroups;
    environments   = newEnvironments;
    pipelines      = newPipelines;
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
