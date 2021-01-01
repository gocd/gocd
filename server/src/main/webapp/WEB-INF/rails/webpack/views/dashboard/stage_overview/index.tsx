/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import m from "mithril";
import Stream from "mithril/stream";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {Spinner} from "../../components/spinner";
import * as styles from "./index.scss";
import {JobsListWidget} from "./jobs_list_widget";
import {JobCountAndRerunWidget} from "./job_count_and_rerun_widget";
import {StageOverviewViewModel} from "./models/stage_overview_view_model";
import {StageState} from "./models/types";
import {StageHeaderWidget} from "./stage_overview_header";

interface State {
  status: Stream<string>;
  userSelectedStageCounter: Stream<number | string>;
  shouldSelectLatestStageCounterOnUpdate: Stream<boolean>;
  latestStageCounter: Stream<string>;
  isLoading: Stream<boolean>;
}

export interface Attrs {
  pipelineName: string;
  pipelineCounter: string | number;
  stageName: string;
  stageCounter: string | number;
  stageStatus: StageState | string;
  stages: any[];
  templateName: string | undefined | null;
  pollingInterval?: number;
  canAdminister: boolean;
  stageInstanceFromDashboard: any;
  isDisplayedOnPipelineActivityPage?: boolean;
  isDisplayedOnVSMPage?: boolean;
  leftPositionForVSMStageOverview?: number;
  stageOverviewVM: Stream<StageOverviewViewModel | undefined>;
}

export class StageOverview extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.status = Stream(vnode.attrs.stageInstanceFromDashboard.status.toLowerCase());
    vnode.state.userSelectedStageCounter = Stream<string | number>(vnode.attrs.stageCounter);

    vnode.state.isLoading = Stream<boolean>(false);
    vnode.state.shouldSelectLatestStageCounterOnUpdate = Stream<boolean>(false);
    vnode.state.latestStageCounter = Stream<string>(vnode.attrs.stageInstanceFromDashboard.counter);
  }

  oncreate(vnode: m.Vnode<Attrs, State>) {
    // @ts-ignore
    vnode.dom.onclick = (e: any) => {
      e.stopPropagation();
    };

    const windowWidth = window.innerWidth;
    // @ts-ignore
    const stageOverviewRightBoundry = vnode.dom.getBoundingClientRect().right;

    // align stage overview to left when the right boundry of the stage overview is outside of the window width
    const shouldAlignLeft = stageOverviewRightBoundry > windowWidth;

    // @ts-ignore
    const status = styles[`${vnode.attrs.stageInstanceFromDashboard.status.toLowerCase()}-stage`];
    let classNames = `${styles.stageOverviewContainer} ${status}`;
    if (vnode.attrs.isDisplayedOnPipelineActivityPage) {
      classNames = `${classNames} ${styles.pipelineActivityClass}`;
      if (shouldAlignLeft) {
        classNames = `${classNames} ${styles.pipelineActivityAlignLeft}`;
      } else {
        classNames = `${classNames} ${styles.pipelineActivityAlignRight}`;
      }
    } else if (vnode.attrs.isDisplayedOnVSMPage) {
      classNames = `${classNames} ${styles.vsmClass}`;
      classNames = `${classNames} ${styles.pipelineActivityAlignRight}`;
    } else if (shouldAlignLeft) {
      classNames = `${classNames} ${styles.alignLeft}`;
    }

    // @ts-ignore
    classNames.split(" ").filter(c => !!c).forEach(c => {vnode.dom.classList.add(c);});

    if (vnode.attrs.isDisplayedOnPipelineActivityPage) {
      let top = 36;
      let left = -6;

      // for a user with no operate permission, the add comment feature is not available, making the stage overview mis-positioned,
      // hence, position stage overview a little above for read only users.
      if (!vnode.attrs.stageInstanceFromDashboard.canOperate) {
        top = 27;
      }

      if (shouldAlignLeft) {
        left = -655;
      }

      // @ts-ignore
      vnode.dom.style.top = `${top}px`;
      // @ts-ignore
      vnode.dom.style.left = `${left}px`;
      return;
    } else if (vnode.attrs.isDisplayedOnVSMPage) {
      const left = vnode.attrs.leftPositionForVSMStageOverview!;
      // @ts-ignore
      vnode.dom.style.marginTop = `22px`;
      // @ts-ignore
      vnode.dom.style.left = `${left}px`;
      return;
    }

    //   // horizontal left alignment
    //   // 725px is the initial alignment and each stage bar is of 45px in width (including margin)
    const currentStageIndex = vnode.attrs.stages.findIndex(s => s.name === vnode.attrs.stageName);
    const toOperate = vnode.attrs.stages.slice(0, currentStageIndex + 1);

    //initial alignment is 10px (margin)
    let stagesWidth = 10;
    let stageTriggerWidth = 0;

    toOperate.forEach((stage, index) => {
      if (index === 0) {
        return;
      }

      //the stage width container value changes based on whether the stage is manual or not.
      // if the stage is manual, width will be stage_width (45px) + manual_gate(18px) + margin(5px).
      const possibleStageWithManualGateWidth = stage.isManual() ? 68 : 45;

      // the pipeline tile in which stages are rendered in a row is of width is 235 px,
      // so, when the sum of the stage width reaches the row width, the stage is rendered in the next row
      if ((stagesWidth + stageTriggerWidth) + possibleStageWithManualGateWidth > 220) {
        stagesWidth = 10;
        stageTriggerWidth = stage.isManual() ? 18 : 0;
        return;
      }

      // each manual stage icon is of 18px in width
      if (stage.isManual()) {
        stageTriggerWidth = stageTriggerWidth + 18;
      }

      // each manual stage bar is of 44.5px in width
      stagesWidth = stagesWidth + 44.5;
    });

    // 563 is the initial right alignment
    const leftAlign = shouldAlignLeft ? -(563 - (stagesWidth + stageTriggerWidth)) : (stagesWidth + stageTriggerWidth);

    // horizontal alignment
    // 10px is the initial margin left and each stage bar is of 45px in width (including margin)
    // @ts-ignore
    vnode.dom.style.left = `${leftAlign}px`;
  }

  onupdate(vnode: m.VnodeDOM<Attrs, State>): any {
    if (vnode.attrs.stageInstanceFromDashboard.counter === vnode.state.userSelectedStageCounter()) {
      vnode.state.status(vnode.attrs.stageInstanceFromDashboard.status.toLowerCase());
    }

    const existingClassesToRemove = [styles.buildingStage, styles.cancelledStage, styles.passedStage, styles.failedStage, styles.failingStage];
    existingClassesToRemove.forEach((c) => vnode.dom.classList.remove(c));

    // @ts-ignore
    const status = styles[`${vnode.state.status()}-stage`];
    vnode.dom.classList.add(status);
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    // @ts-ignore
    const status = styles[`${vnode.state.status()}-stage`];

    if (!vnode.attrs.stageOverviewVM()) {
      return <div data-test-id="stage-overview-container-spinner" style="width: 813px">
        <div className={`${status} ${styles.stageOverviewStatus}`}/>
        <Spinner/>
      </div>;
    }

    const inProgressStageFromPipeline = Stream(vnode.attrs.stages.find((s) => s.isBuilding()));
    return <div data-test-id="stage-overview-container" style="width: 813px">
      <div class={`${status} ${styles.stageOverviewStatus}`}/>
      <StageHeaderWidget stageName={vnode.attrs.stageName}
                         stageCounter={vnode.attrs.stageCounter}
                         userSelectedStageCounter={vnode.state.userSelectedStageCounter}
                         pipelineName={vnode.attrs.pipelineName}
                         pipelineCounter={vnode.attrs.pipelineCounter}
                         templateName={vnode.attrs.templateName}
                         pollingInterval={vnode.attrs.pollingInterval}
                         status={vnode.state.status}
                         shouldSelectLatestStageCounterOnUpdate={vnode.state.shouldSelectLatestStageCounterOnUpdate}
                         latestStageCounter={vnode.state.latestStageCounter}
                         isLoading={vnode.state.isLoading}
                         stageInstanceFromDashboard={vnode.attrs.stageInstanceFromDashboard}
                         inProgressStageFromPipeline={inProgressStageFromPipeline}
                         stageOverviewVM={vnode.attrs.stageOverviewVM as Stream<StageOverviewViewModel>}
                         flashMessage={vnode.attrs.stageOverviewVM()!.flashMessage}
                         canAdminister={vnode.attrs.canAdminister}
                         stageInstance={vnode.attrs.stageOverviewVM()!.stageInstance}/>
      <JobCountAndRerunWidget stageName={vnode.attrs.stageName}
                              stageCounter={vnode.attrs.stageCounter}
                              userSelectedStageCounter={vnode.state.userSelectedStageCounter}
                              pipelineName={vnode.attrs.pipelineName}
                              pipelineCounter={vnode.attrs.pipelineCounter}
                              flashMessage={vnode.attrs.stageOverviewVM()!.flashMessage}
                              shouldSelectLatestStageCounterOnUpdate={vnode.state.shouldSelectLatestStageCounterOnUpdate}
                              isLoading={vnode.state.isLoading}
                              inProgressStageFromPipeline={inProgressStageFromPipeline}
                              jobsVM={vnode.attrs.stageOverviewVM()!.jobsVM}/>
      <JobsListWidget stageName={vnode.attrs.stageName}
                      stageCounter={vnode.state.userSelectedStageCounter()}
                      pipelineName={vnode.attrs.pipelineName}
                      pipelineCounter={vnode.attrs.pipelineCounter}
                      jobsVM={vnode.attrs.stageOverviewVM()!.jobsVM}
                      agents={vnode.attrs.stageOverviewVM()!.agents}
                      isStageInProgress={Stream(vnode.attrs.stageOverviewVM()!.stageInstance().isInProgress())}
                      lastPassedStageInstance={vnode.attrs.stageOverviewVM()!.lastPassedStageInstance}/>
    </div>;

  }
}
