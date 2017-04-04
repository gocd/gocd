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

;(function () {
  "use strict";

  const Stream = require("mithril/stream");
  const _      = require("lodash");

  function PipelinesModel(json) {

    const DEFAULT_PIPELINE = "[Any Pipeline]";
    const DEFAULT_STAGE    = "[Any Stage]";

    const pipelineNames    = _.map(json, "pipeline");
    const events           = [
      "All",
      "Passes",
      "Fails",
      "Breaks",
      "Fixed",
      "Cancelled"
    ];
    const stagesByPipeline = _.reduce(json, function (memo, entry) {
      memo[entry.pipeline] = [DEFAULT_STAGE].concat(entry.stages);
      return memo;
    }, {});

    pipelineNames.unshift(DEFAULT_PIPELINE);
    stagesByPipeline[DEFAULT_PIPELINE] = [DEFAULT_STAGE];

    const current = Stream(DEFAULT_PIPELINE);
    const stages  = current.map(function (pipeline) {
      return stagesByPipeline[pipeline];
    });

    return {
      pipelines:       Stream(pipelineNames),
      currentPipeline: current,
      stages:          stages,
      events:          Stream(events)
    };
  }

  module.exports = PipelinesModel;
})();
