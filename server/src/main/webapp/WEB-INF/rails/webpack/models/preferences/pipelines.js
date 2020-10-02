/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import Stream from "mithril/stream";
import _ from "lodash";

export function Pipelines(json) {

  const DEFAULT_PIPELINE = "[Any Pipeline]";
  const DEFAULT_STAGE    = "[Any Stage]";

  const data = json.slice(); // duplicate because we will modify
  data.unshift({pipeline: DEFAULT_PIPELINE, stages: [DEFAULT_STAGE]});

  const pipelines = _.map(data, "pipeline");
  const events    = [
    "All",
    "Passes",
    "Fails",
    "Breaks",
    "Fixed",
    "Cancelled"
  ];

  const stagesByPipeline = _.reduce(data, (memo, entry) => {
    memo[entry.pipeline] = (entry.stages.indexOf(DEFAULT_STAGE) === -1) ? [DEFAULT_STAGE].concat(entry.stages) : entry.stages;
    return memo;
  }, {});

  const currentPipeline = Stream(DEFAULT_PIPELINE);
  const stages          = currentPipeline.map((pipeline) => {
    return stagesByPipeline[pipeline];
  });

  function reset() {
    currentPipeline(DEFAULT_PIPELINE);
  }

  return {
    pipelines,
    currentPipeline,
    stages,
    events,
    reset
  };
}
