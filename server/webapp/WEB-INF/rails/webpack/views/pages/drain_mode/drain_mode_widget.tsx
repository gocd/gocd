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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {DrainModeInfo, RunningSystem, StageLocator} from "models/drain_mode/types";
import {SwitchBtn} from "views/components/switch";
import {TooltipSize} from "views/components/tooltip";
import * as Tooltip from "views/components/tooltip";
import {DisabledSubsystemsWidget} from "views/pages/drain_mode/disabled_susbsystems_widget";
import {JobInfoWidget} from "views/pages/drain_mode/running_jobs_widget.tsx";
import {MDUInfoWidget} from "views/pages/drain_mode/running_mdus_widget";
import * as styles from "./index.scss";

interface Attrs {
  drainModeInfo: DrainModeInfo;
  toggleDrainMode: (e: Event) => void;
  onCancelStage: (stageLocator: StageLocator) => void;
}

export class DrainModeWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const drainModeInfo = vnode.attrs.drainModeInfo;

    let mayBeDrainInfo;
    if (drainModeInfo.drainModeState()) {
      mayBeDrainInfo = (
        <div data-test-id="in-progress-subsystems" className={styles.inProgressSubsystems}>
          <DrainModeInfoWidget drainModeInfo={vnode.attrs.drainModeInfo} onCancelStage={vnode.attrs.onCancelStage}/>
        </div>
      );
    }

    let drainModeStateMessage = "";
    if (drainModeInfo.drainModeState()) {
      drainModeStateMessage = drainModeInfo.isCompletelyDrained
        ? "GoCD Server is completely drained."
        : "Some subsystems of GoCD are still in progress.";
    }

    let updatedByMessage = "GoCD server drain mode is disabled by default.";
    if (drainModeInfo.metdata.updatedBy !== null) {
      updatedByMessage = `${drainModeInfo.metdata.updatedBy} changed the drain mode state on ${drainModeInfo.metdata.updatedOn}.`;
    }

    return (
      <div className={styles.drainModeWidget} data-test-id="drain-mode-widget">
        <p data-test-id="drain-mode-description" className={styles.drainModeDescription}>
          The drain mode is a maintenance mode which a GoCD system administrator can put GoCD into so that it is
          safe to restart it or upgrade it without having running jobs reschedule when it is back.
        </p>

        <div class={styles.drainModeInfo}>
          <span data-test-id="drain-mode-updated-by-info" class={styles.updatedBy}>
            {updatedByMessage}
          </span>
          <span class={styles.switchWrapper} data-test-id="switch-wrapper">
            <span class={styles.drainModeLabel}>Enable Drain Mode:</span>
            <SwitchBtn inProgress={drainModeInfo.drainModeState() && !drainModeInfo.isCompletelyDrained}
                       field={drainModeInfo.drainModeState}
                       onclick={vnode.attrs.toggleDrainMode}/>
            <div class={styles.drainModeStateMessage}>{drainModeStateMessage}</div>
          </span>
        </div>
        <DisabledSubsystemsWidget drainModeInfo={drainModeInfo}/>
        {mayBeDrainInfo}
      </div>
    );
  }
}

interface InfoAttrs {
  drainModeInfo: DrainModeInfo;
  onCancelStage: (stageLocator: StageLocator) => void;
}

export class DrainModeInfoWidget extends MithrilViewComponent<InfoAttrs> {
  view(vnode: m.Vnode<InfoAttrs>): m.Children {
    return [
      <JobInfoWidget stages={(vnode.attrs.drainModeInfo.runningSystem as RunningSystem).buildingJobsGroupedByStages}
                     title={"Running Stages"}
                     onCancelStage={vnode.attrs.onCancelStage}/>,
      <MDUInfoWidget materials={(vnode.attrs.drainModeInfo.runningSystem as RunningSystem).materialUpdateInProgress}/>,
      <JobInfoWidget stages={(vnode.attrs.drainModeInfo.runningSystem as RunningSystem).scheduledJobsGroupedByStages}
                     title={<span class={styles.scheduledStagesTitleWrapper}>
                       <div class={styles.scheduledStagesTitle}> Scheduled Stages </div>
                       <Tooltip.Info size={TooltipSize.large}
                                     content={"Scheduled stages contains the jobs which are scheduled but not yet assigned to any agent. As the job assignment to agents is stopped during drain mode, scheduled jobs will not have a side effect on the server state. Hence, Scheduled stages are ignored while considering server drain mode."}/>
                     </span>}
                     onCancelStage={vnode.attrs.onCancelStage}/>,
    ] as m.ChildArray;
  }
}
