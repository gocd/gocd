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
import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {MaintenanceModeInfo, RunningSystem, StageLocator} from "models/maintenance_mode/types";
import {Link} from "views/components/link";
import {SwitchBtn} from "views/components/switch";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {DisabledSubsystemsWidget} from "views/pages/maintenance_mode/disabled_susbsystems_widget";
import {JobInfoWidget} from "views/pages/maintenance_mode/running_jobs_widget.tsx";
import {MDUInfoWidget} from "views/pages/maintenance_mode/running_mdus_widget";
import styles from "./index.scss";

interface Attrs {
  maintenanceModeInfo: MaintenanceModeInfo;
  toggleMaintenanceMode: (e: Event) => void;
  onCancelStage: (stageLocator: StageLocator) => void;
}

export class MaintenanceModeWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const maintenanceModeInfo = vnode.attrs.maintenanceModeInfo;

    let mayBeMaintenanceInfo;
    if (maintenanceModeInfo.maintenanceModeState()) {
      mayBeMaintenanceInfo = (
        <div data-test-id="in-progress-subsystems" class={styles.inProgressSubsystems}>
          <MaintenanceModeInfoWidget maintenanceModeInfo={vnode.attrs.maintenanceModeInfo} onCancelStage={vnode.attrs.onCancelStage}/>
        </div>
      );
    }

    let maintenanceModeStateMessage = "";
    if (maintenanceModeInfo.maintenanceModeState()) {
      maintenanceModeStateMessage = maintenanceModeInfo.hasRunningSystems
        ? "Some subsystems of GoCD are still in progress."
        : "GoCD Server has no running subsystems.";
    }

    let updatedByMessage = "GoCD server maintenance mode is disabled by default.";
    if (maintenanceModeInfo.metdata.updatedBy !== null) {
      if (maintenanceModeInfo.metdata.updatedBy === "GoCD") {
        updatedByMessage = `GoCD Server is started in maintenance mode at ${maintenanceModeInfo.metdata.updatedOn}.`;
      } else {
        updatedByMessage = `${maintenanceModeInfo.metdata.updatedBy} changed the maintenance mode state on ${maintenanceModeInfo.metdata.updatedOn}.`;
      }
    }

    return (
      <div class={styles.maintenanceModeWidget} data-test-id="maintenance-mode-widget">
        <p data-test-id="maintenance-mode-description" class={styles.maintenanceModeDescription}>
          When put into maintenance mode, it is safe to restart or upgrade the GoCD server without having any running
          jobs reschedule when the server is back up.
          &nbsp;
          <Link target="_blank" href={docsUrl("/advanced_usage/maintenance_mode.html")}>Learn more..</Link>
        </p>

        <div class={styles.maintenanceModeInfo}>
          <span data-test-id="maintenance-mode-updated-by-info" class={styles.updatedBy}>
            {updatedByMessage}
          </span>
          <span class={styles.switchWrapper} data-test-id="switch-wrapper">
            <span class={styles.maintenanceModeLabel}>Enable Maintenance Mode:</span>
            <SwitchBtn inProgress={maintenanceModeInfo.maintenanceModeState() && maintenanceModeInfo.hasRunningSystems}
                       field={() => maintenanceModeInfo.maintenanceModeState()}
                       onclick={vnode.attrs.toggleMaintenanceMode}/>
            <div class={styles.maintenanceModeStateMessage}>{maintenanceModeStateMessage}</div>
          </span>
        </div>
        <DisabledSubsystemsWidget maintenanceModeInfo={maintenanceModeInfo}/>
        {mayBeMaintenanceInfo}
      </div>
    );
  }
}

interface InfoAttrs {
  maintenanceModeInfo: MaintenanceModeInfo;
  onCancelStage: (stageLocator: StageLocator) => void;
}

export class MaintenanceModeInfoWidget extends MithrilViewComponent<InfoAttrs> {
  view(vnode: m.Vnode<InfoAttrs>): m.Children {
    return [
      <JobInfoWidget stages={(vnode.attrs.maintenanceModeInfo.runningSystem as RunningSystem).buildingJobsGroupedByStages}
                     title={"Running Stages"}
                     onCancelStage={vnode.attrs.onCancelStage}/>,
      <MDUInfoWidget materials={(vnode.attrs.maintenanceModeInfo.runningSystem as RunningSystem).materialUpdateInProgress}/>,
      <JobInfoWidget stages={(vnode.attrs.maintenanceModeInfo.runningSystem as RunningSystem).scheduledJobsGroupedByStages}
                     title={<span class={styles.scheduledStagesTitleWrapper}>
                       <div class={styles.scheduledStagesTitle}> Scheduled Stages </div>
                       <Tooltip.Info size={TooltipSize.large}
                                     content={"Scheduled stages contains the jobs which are scheduled but not yet assigned to any agent. As the job assignment to agents is stopped during maintenance mode, scheduled jobs will not have a side effect on the server state. Hence, Scheduled stages are ignored while considering server maintenance mode."}/>
                     </span>}
                     onCancelStage={vnode.attrs.onCancelStage}/>,
    ] as m.ChildArray;
  }
}
