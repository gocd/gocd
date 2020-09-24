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
let stageOverviewPipelineName, stageOverviewPipelineCounter, stageOverviewStageName, stageOverviewStageCounter;


const StageOverviewState = {
  model: Stream(),

  isOpen: (pipeline, pCounter, stage, sCounter) => ((pipeline === stageOverviewPipelineName) && (pCounter === stageOverviewPipelineCounter) && (stage === stageOverviewStageName) && (sCounter === stageOverviewStageCounter)),

  show: (pipeline, pCounter, stage, sCounter) => {
    StageOverviewState.hide();
    stageOverviewPipelineName = pipeline;
    stageOverviewPipelineCounter = pCounter;
    stageOverviewStageName = stage;
    stageOverviewStageCounter = sCounter;
  },

  hide: () => {
    if(StageOverviewState.model()) {
      StageOverviewState.model().stopRepeater();
      StageOverviewState.model(undefined);
    }

    stageOverviewPipelineName = undefined;
    stageOverviewPipelineCounter = undefined;
    stageOverviewStageName = undefined;
    stageOverviewStageCounter = undefined;
  },

  matchesPipelineAndStage: (pipeline, stage) => ((pipeline === stageOverviewPipelineName) && (stage === stageOverviewStageName)),

  getPipelineName: () => stageOverviewPipelineName,

  getPipelineCounter: () => stageOverviewPipelineCounter,

  getStageName: () => stageOverviewStageName,

  getStageCounter: () => stageOverviewStageCounter
};

export default {
  StageOverviewState
};
