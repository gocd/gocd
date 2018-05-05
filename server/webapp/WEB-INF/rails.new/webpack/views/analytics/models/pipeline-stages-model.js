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

const Stream = require('mithril/stream');
const _      = require("lodash");

const COMPARATOR = require("string-plus").caseInsensitiveCompare;

function PipelineStagesVM (stagesByPipeline) {
  const pipelines = Stream(Object.keys(stagesByPipeline).sort(COMPARATOR));
  const currentPipeline = Stream(pipelines()[0]);

  const stages = currentPipeline.map((pipeline) => {
    const s = stagesByPipeline[pipeline];
    s.unshift(null);
    return s;
  });
  const currentStage = Stream(stages()[0]);

  _.assign(this, {
    pipelines,
    stages,
    currentPipeline,
    currentStage
  });
}

module.exports = PipelineStagesVM;
