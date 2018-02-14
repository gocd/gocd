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
const $           = require('jquery');
const m           = require('mithril');
const mrequest    = require('helpers/mrequest');
const VMRoutes    = require('helpers/vm_routes');
const SparkRoutes = require('helpers/spark_routes');
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

  const config = (xhr) => {
    xhr.setRequestHeader("X-GoCD-Confirm", "true");
    mrequest.xhrConfig.forVersion('v1')(xhr);
  };

  function postURL(url, data = {}) {
    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:     'POST',
        url,
        timeout:    mrequest.timeout,
        data:       JSON.stringify(data),
        beforeSend: config
      });

      jqXHR.then((data) => {
        deferred.resolve(data);
      });

      jqXHR.fail((res) => {
        deferred.reject(res);
      });

      jqXHR.always(m.redraw);
    }).promise();
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
};

module.exports = Pipeline;
