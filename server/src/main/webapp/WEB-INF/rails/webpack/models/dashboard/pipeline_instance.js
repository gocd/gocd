/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {SparkRoutes} from "helpers/spark_routes";
import {MaterialRevision} from "models/dashboard/material_revision";
import {AjaxHelper} from "helpers/ajax_helper";
import {VMRoutes} from "helpers/vm_routes";
import Routes from "gen/js-routes";
import _ from "lodash";

const StageInstance = function (json, pipelineName, pipelineCounter) {
  this.name                  = json.name;
  this.counter               = json.counter;
  this.status                = json.status;
  this.cancelledBy           = json.cancelled_by;
  this.stageDetailTabPath    = Routes.stageDetailTabDefaultPath(pipelineName, pipelineCounter, json.name, json.counter);
  this.isBuilding            = () => json.status === 'Building';
  this.isFailing             = () => json.status === 'Failing';
  this.isFailed              = () => json.status === 'Failed';
  this.isBuildingOrCompleted = () => json.status !== 'Unknown';
  this.isCancelled           = () => json.status === 'Cancelled';
  this.isCompleted           = () => json.status === 'Passed' || json.status === 'Failed' || json.status === 'Cancelled';

  this.approvedBy                         = json.approved_by;
  this.isManual                           = () => json.approval_type === 'manual';
  this.triggerOnCompletionOfPreviousStage = () => !this.isManual();

  this.trigger = () => {
    return AjaxHelper.POST({
      url: SparkRoutes.stageTriggerPath(pipelineName, pipelineCounter, json.name),
      apiVersion: 'v2'
    });
  };
};

export const PipelineInstance = function (info, pipelineName) {
  const self = this;

  this.pipelineName = pipelineName;
  this.label        = info.label;
  this.counter      = info.counter;
  this.scheduledAt  = info.scheduled_at;
  this.triggeredBy  = info.triggered_by;

  this.vsmPath     = VMRoutes.vsmPath(this.pipelineName, this.counter);
  this.comparePath = VMRoutes.comparePath(this.pipelineName, this.counter - 1, this.counter);

  this.stages = _.map(info._embedded.stages, (stage) => new StageInstance(stage, this.pipelineName, this.counter));

  this.isFirstStageInProgress = () => self.stages[0].isBuilding();

  this.getBuildCause = () => {
    return AjaxHelper.GET({
      url:        SparkRoutes.buildCausePath(this.pipelineName, this.counter),
      apiVersion: 'v1',
    }).then((buildCause) => {
      return _.map(buildCause.material_revisions, (revision) => new MaterialRevision(revision));
    });
  };
};


