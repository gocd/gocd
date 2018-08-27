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
const Routes           = require('gen/js-routes');
const sparkRoutes      = require('helpers/spark_routes');
const AjaxHelper       = require('helpers/ajax_helper');
const MaterialRevision = require('models/dashboard/material_revision');

const StageInstance = function (json, pipelineName, pipelineCounter) {
  this.name                  = json.name;
  this.counter               = json.counter;
  this.status                = json.status;
  this.stageDetailTabPath    = Routes.stageDetailTabPath(pipelineName, pipelineCounter, json.name, json.counter);
  this.isBuilding            = () => json.status === 'Building';
  this.isFailing             = () => json.status === 'Failing';
  this.isFailed              = () => json.status === 'Failed';
  this.isBuildingOrCompleted = () => json.status !== 'Unknown';
};

const PipelineInstance = function (info, pipelineName) {
  const self = this;

  this.pipelineName = pipelineName;
  this.label        = info.label;
  this.counter      = info.counter;
  this.scheduledAt  = info.scheduled_at;
  this.triggeredBy  = info.triggered_by;

  this.vsmPath     = info._links.vsm_url.href;
  this.comparePath = info._links.compare_url.href;

  this.stages = _.map(info._embedded.stages, (stage) => new StageInstance(stage, this.pipelineName, this.counter));

  this.isFirstStageInProgress = () => self.stages[0].isBuilding();

  this.getBuildCause = () => {
    return AjaxHelper.GET({
      url:        sparkRoutes.buildCausePath(this.pipelineName, this.counter),
      apiVersion: 'v1',
    }).then((buildCause) => {
      return _.map(buildCause.material_revisions, (revision) => new MaterialRevision(revision));
    });
  };
};

module.exports = PipelineInstance;

