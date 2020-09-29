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

import "foundation-sites";
import $ from "jquery";
import m from "mithril";
// @ts-ignore
import state from "views/dashboard/models/stage_overview_state";
import {StageOverview} from "../views/dashboard/stage_overview";
import {StageOverviewViewModel} from "../views/dashboard/stage_overview/models/stage_overview_view_model";

$(() => {
  window.addEventListener("DOMContentLoaded", () => {
    // @ts-ignore
    window.stageOverviewStateForVSM = state.StageOverviewState;

    function closeStageOverview() {
      // @ts-ignore
      const state = window.stageOverviewStateForVSM;

      if (state.model()) {
        const ele = document.getElementById(`stage-overview-container-for-pipeline-${state.getPipelineName()}-${state.getPipelineCounter()}-stage-${state.getStageName()}-${state.getStageCounter()}`)!;
        ele.innerHTML = "";

        state.hide();
      }
    }

    document.getElementById("vsm-container")!.onclick = closeStageOverview;

    // @ts-ignore
    window.getStageOverviewFor = (pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number, status: string, currentStageIndex: string, totalNumberOfStages: string, canEdit: boolean, templateName: string) => {
      closeStageOverview();
      const repeatInterval = 9999999;

      // @ts-ignore
      window.stageOverviewStateForVSM.show(pipelineName, pipelineCounter, stageName, stageCounter);
      // @ts-ignore
      StageOverviewViewModel.initialize(pipelineName, pipelineCounter, stageName, stageCounter, status, repeatInterval).then((result) => window.stageOverviewStateForVSM.model(result));

      const stageOverviewContainer = document.getElementById(`stage-overview-container-for-pipeline-${pipelineName}-${pipelineCounter}-stage-${stageName}-${stageCounter}`)!;
      const stageInstanceFromDashboard = {
        status
      };

      // @ts-ignore
      const totalWidth = new Array(...document.querySelector(`#${pipelineName}`).classList).indexOf("current") !== -1 ? 237 : 192;
      const spaceBetweenStages = 4;
      const initialLeftPosition = -36;

      const widthOfEachStage = (totalWidth - (4 * +totalNumberOfStages)) / +totalNumberOfStages;
      const leftPosition = (widthOfEachStage * +currentStageIndex) + (spaceBetweenStages * +currentStageIndex) + (widthOfEachStage/2) + initialLeftPosition;
      const templateNameFromString = templateName === 'null' ? null : templateName;

      m.mount(stageOverviewContainer, {
        view(vnode: m.Vnode<{}, {}>): m.Children | void | null {
          return <StageOverview pipelineName={pipelineName}
                                canAdminister={canEdit}
                                pipelineCounter={pipelineCounter}
                                stageName={stageName}
                                stageCounter={stageCounter}
                                stages={[]}
                                templateName={templateNameFromString}
                                pollingInterval={repeatInterval}
                                isDisplayedOnVSMPage={true}
                                leftPositionForVSMStageOverview={leftPosition}
                                stageInstanceFromDashboard={stageInstanceFromDashboard}
                                // @ts-ignore
                                stageOverviewVM={window.stageOverviewStateForVSM.model}
                                stageStatus={status}/>;
        }
      });
    };
  });
});
