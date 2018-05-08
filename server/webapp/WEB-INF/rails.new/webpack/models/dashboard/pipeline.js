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

const _           = require('lodash');
const Stream      = require('mithril/stream');
const VMRoutes    = require('helpers/vm_routes');
const SparkRoutes = require('helpers/spark_routes');
const AjaxHelper  = require('helpers/ajax_helper');
const Routes      = require('gen/js-routes');

const PipelineInstance = require('models/dashboard/pipeline_instance');

const Pipeline = function (info) {
  const self = this;
  this.name  = info.name;

  this.canAdminister = info.can_administer;
  this.settingsPath  = Routes.pipelineEditPath('pipelines', info.name, 'general');
  this.quickEditPath = Routes.editAdminPipelineConfigPath(info.name);

  this.historyPath = VMRoutes.pipelineHistoryPath(info.name);
  this.instances   = _.map(info._embedded.instances, (instance) => new PipelineInstance(instance, info.name));

  this.isPaused    = info.pause_info.paused;
  this.pausedBy    = info.pause_info.paused_by;
  this.pausedCause = info.pause_info.pause_reason;
  this.canPause    = info.can_pause;


  this.isLocked  = info.locked;
  this.canUnlock = info.can_unlock;

  this.isDefinedInConfigRepo = () => info.from_config_repo;

  this.canOperate = info.can_operate;

  this.trackingTool = info.tracking_tool;

  this.isFirstStageInProgress = () => {
    for (let i = 0; i < self.instances.length; i++) {
      if (self.instances[i].isFirstStageInProgress()) {
        return true;
      }
    }
    return false;
  };

  this.triggerDisabled = Stream(false);
  if (!self.canOperate || self.isFirstStageInProgress() || self.isLocked || self.isPaused) {
    self.triggerDisabled(true);
  }

  function postURL(url, payload = {}) {
    return AjaxHelper.POST({url, apiVersion: 'v1', payload});
  }

  this.unpause = () => {
    return postURL(SparkRoutes.pipelineUnpausePath(self.name));
  };

  this.unlock = () => {
    return postURL(SparkRoutes.pipelineUnlockPath(self.name));
  };

  this.pause = (payload) => {
    return postURL(SparkRoutes.pipelinePausePath(self.name), payload);
  };

  this.trigger = (payload = {}) => {
    return postURL(SparkRoutes.pipelineTriggerPath(self.name), payload);
  };

  this.getInstanceCounters = () => {
    return _.map(this.instances, (instance) => instance.counter);
  };

  this.getDisabledTooltipText = () => {
    if (!self.canOperate) {
      return TooltipText.NO_OPERATE_PERMISSION;
    }
    if (self.isFirstStageInProgress()) {
      return TooltipText.FIRST_STAGE_IN_PROGRESS;
    }
    if (self.isPaused) {
      return TooltipText.PIPELINE_PAUSED;
    }
    if (self.isLocked) {
      return TooltipText.PIPELINE_LOCKED;
    }
  };

  this.getPauseDisabledTooltipText = () => {
    if (!self.canPause) {
      if (self.isPaused) {
        return TooltipText.NO_UNPAUSE_PERMISSION;
      }
      return TooltipText.NO_PAUSE_PERMISSION;
    }
  };

  this.getLockDisabledTooltipText = () => {
    return TooltipText.NO_UNLOCK_PERMISSION;
  };

  this.getSettingsDisabledTooltipText = () => {
    if (self.isDefinedInConfigRepo()) {
      return TooltipText.CONFIG_REPO_PIPELINE;
    }
    return TooltipText.NO_EDIT_PERMISSION;
  };

  const TooltipText = {
    NO_OPERATE_PERMISSION:   "You do not have permission to trigger the pipeline",
    PIPELINE_PAUSED:         "Cannot trigger pipeline - Pipeline is currently paused.",
    PIPELINE_LOCKED:         "Cannot trigger pipeline - Pipeline is currently locked.",
    FIRST_STAGE_IN_PROGRESS: "Cannot trigger pipeline - First stage is still in progress.",
    CONFIG_REPO_PIPELINE:    "Cannot edit pipeline defined in config repository.",
    NO_EDIT_PERMISSION:      "You do not have permission to edit the pipeline.",
    NO_UNLOCK_PERMISSION:    "You do not have permission to unlock the pipeline.",
    NO_PAUSE_PERMISSION:     "You do not have permission to pause the pipeline.",
    NO_UNPAUSE_PERMISSION:   "You do not have permission to unpause the pipeline."
  };

  self.hasStatus = (filterStatus) => {
    return _.some(self.instances, instance => instance.hasStatus(filterStatus));
  }
};

module.exports = Pipeline;
