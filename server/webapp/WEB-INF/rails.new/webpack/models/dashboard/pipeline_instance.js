/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const _      = require('lodash');
const Stream = require('mithril/stream');

const StageInstance = function (json) {
  this.name       = Stream(json.name);
  this.status     = Stream(json.status);
  this.isBuilding = () => json.status === 'Building';
};

const PipelineInstance = function (info) {
  this.label       = Stream(info.label);
  this.scheduledAt = Stream(new Date(info.scheduled_at));
  this.triggeredBy = Stream(info.triggered_by);

  this.vsmPath     = Stream(info._links.vsm_url.href);
  this.comparePath = Stream(info._links.compare_url.href);

  this.stages = Stream(_.map(info._embedded.stages, (stage) => new StageInstance(stage)));

  this.latestStageInfo = () => {
    const stages = this.stages();

    for (let i = 0; i < stages.length; i++) {
      if (stages[i].isBuilding()) {
        return `${stages[i].status()}: ${stages[i].name()}`;
      }
    }

    const lastStage = stages[stages.length - 1];
    return `${lastStage.status()}: ${lastStage.name()}`;
  };
};

module.exports = PipelineInstance;

