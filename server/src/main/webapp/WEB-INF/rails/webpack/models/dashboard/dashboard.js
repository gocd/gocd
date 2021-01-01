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
import {SparkRoutes} from "helpers/spark_routes";
import Stream from "mithril/stream";
import {AjaxHelper} from "helpers/ajax_helper";
import {DashboardGroups} from "models/dashboard/dashboard_groups";
import {Pipelines} from "./pipelines";

export function Dashboard() {
  let pipelineGroups = DashboardGroups.fromPipelineGroupsJSON([]);
  let environments   = DashboardGroups.fromEnvironmentsJSON([]);
  let pipelines      = Pipelines.fromJSON([]);

  this.message           = Stream();
  this.getPipelineGroups = () => pipelineGroups;
  this.getEnvironments   = () => environments;

  this.getPipelines     = () => pipelines.pipelines;
  this.allPipelineNames = () => Object.keys(pipelines.pipelines);
  this.findPipeline     = (pipelineName) => pipelines.find(pipelineName);

  this.initialize = (json, showEmptyGroups) => {
    const newPipelineGroups = DashboardGroups.fromPipelineGroupsJSON(_.get(json, '_embedded.pipeline_groups', []), showEmptyGroups);
    const newEnvironments   = DashboardGroups.fromEnvironmentsJSON(_.get(json, '_embedded.environments', []));
    const newPipelines      = Pipelines.fromJSON(_.get(json, '_embedded.pipelines', []));

    const pipelinesNoEnv = _.difference(Object.keys(newPipelines.pipelines), _.reduce(newEnvironments.groups, (memo, group) => memo.concat(group.pipelines), []));
    newEnvironments.groups.push(new DashboardGroups.Environment({
      name:           null,
      can_administer: false,// eslint-disable-line camelcase
      pipelines:      pipelinesNoEnv
    }));

    //set it on the current object only on a successful deserialization of both pipeline groups and pipelines
    pipelineGroups = newPipelineGroups;
    environments   = newEnvironments;
    pipelines      = newPipelines;
  };
}

Dashboard.API_VERSION = "v4";

Dashboard.get = (viewName, etag, allowEmpty) => {
  return AjaxHelper.GET({
    url:        SparkRoutes.showDashboardPath(viewName, !!allowEmpty),
    apiVersion: Dashboard.API_VERSION,
    etag
  });
};

