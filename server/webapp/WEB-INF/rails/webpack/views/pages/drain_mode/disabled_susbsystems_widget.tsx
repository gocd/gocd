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

import * as m from "mithril";

import {MithrilViewComponent} from "jsx/mithril-component";
import {DrainModeInfo} from "models/drain_mode/types";
import {FlashMessage, MessageType} from "views/components/flash_message";
import * as Icons from "views/components/icons";

import * as styles from "./index.scss";

interface Attrs {
  drainModeInfo: DrainModeInfo;
}

class InformationWhenNotInDrainMode extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div data-test-id="info-when-not-in-drain-mode">
      <h3 data-test-id="info-when-not-in-drain-mode-header">Enabling GoCD Server Drain mode will:</h3>
      <ul class={styles.gocdSubSystemInfo}>
        <SubsystemInfoWithIconWidget isReadOnly={true}
                                     dataTestId={"stop-material"}
                                     text={"Stop the material subsystem so that no new materials are polled."}/>
        <SubsystemInfoWithIconWidget isReadOnly={true}
                                     dataTestId="stop-config-repo"
                                     text={"Stop polling on config repositories."}/>
        <SubsystemInfoWithIconWidget isReadOnly={true}
                                     dataTestId="stop-pipeline-scheduling"
                                     text={"Stop the scheduling subsystem so that no new pipelines are triggered (automatically or through timers)."}/>
        <SubsystemInfoWithIconWidget isReadOnly={true}
                                     dataTestId="stop-work-assignment"
                                     text={"Stop the agent subsystem, so that no agents can pick up work if they’re idle."}/>
        <SubsystemInfoWithIconWidget isReadOnly={true}
                                     dataTestId="stop-manual-trigger"
                                     text={"Prevent users from triggering pipelines."}/>
        <SubsystemInfoWithIconWidget isReadOnly={true}
                                     dataTestId="stop-config-changes"
                                     text={"Prevent users from modifying configurations."}/>
        <SubsystemInfoWithIconWidget isReadOnly={true}
                                     dataTestId="stop-db-changes"
                                     text={"Prevent users from performing operations that modifies state in the database or filesystem. "}/>
      </ul>
    </div>;
  }
}

interface RunningSystemAttrs {
  isReadOnly?: boolean;
  inProgress?: boolean;
  dataTestId: string;
  text: string;
}

class SubsystemInfoWithIconWidget extends MithrilViewComponent<RunningSystemAttrs> {
  view(vnode: m.Vnode<RunningSystemAttrs>) {
    let icon = (<Icons.Minus iconOnly={true}/>);

    if (!vnode.attrs.isReadOnly) {
      icon = vnode.attrs.inProgress
        ? <Icons.Spinner iconOnly={true}/>
        : <Icons.Check iconOnly={true}/>;
    }

    return <li data-test-id={vnode.attrs.dataTestId} class={styles.runningSystem}>
      {icon}
      <span class={styles.runningSystemText}>{vnode.attrs.text}</span>
    </li>;
  }
}

class InformationWhenInDrainMode extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isCompletelyDrained = vnode.attrs.drainModeInfo.isCompletelyDrained;

    const message = isCompletelyDrained
      ? <FlashMessage type={MessageType.info} message={"GoCD Server is completely drained."}/>
      : <FlashMessage type={MessageType.warning} message={"Some subsystems of GoCD are still in progress."}/>;

    const mduRunningSystem = vnode.attrs.drainModeInfo.runningSystem.mdu.count() === 0
      ? <SubsystemInfoWithIconWidget inProgress={false} dataTestId={"mdu-stopped"}
                                     text={"Stopped material subsystem."}/>
      : <SubsystemInfoWithIconWidget inProgress={true} dataTestId={"mdu-in-progress"}
                                     text={"Waiting for material subsystem to stop.."}/>;

    const buildingJobsSystem = vnode.attrs.drainModeInfo.runningSystem.jobs.length === 0
      ? <SubsystemInfoWithIconWidget inProgress={false} dataTestId={"scheduling-system-stopped"}
                                     text={"Stopped scheduling subsystem."}/>
      : <SubsystemInfoWithIconWidget inProgress={true} dataTestId={"scheduling-system-in-progress"}
                                     text={"Waiting for building jobs to finish.."}/>;

    return <div data-test-id="info-when-not-in-drain-mode">
      {message}
      <ul class={styles.runningSubSystem} data-test-id="running-sub-systems">
        {mduRunningSystem}
        <SubsystemInfoWithIconWidget inProgress={false} dataTestId={"config-repo-polling-stopped"}
                                     text={"Stopped polling on config repositories."}/>
        {buildingJobsSystem}
        <SubsystemInfoWithIconWidget inProgress={false} dataTestId={"agent-subsystem-stopped"}
                                     text={"Stopped assigning jobs to agents."}/>
        <SubsystemInfoWithIconWidget inProgress={false} dataTestId={"manual-trigger-stopped"}
                                     text={"Stopped pipeline triggers."}/>
        <SubsystemInfoWithIconWidget inProgress={false} dataTestId={"config-changes-stopped"}
                                     text={"Stopped config modifications."}/>
        <SubsystemInfoWithIconWidget inProgress={false} dataTestId={"db-changes-stopped"}
                                     text={"Stopped database and filesystem modifications."}/>
      </ul>
    </div>;
  }
}

export class DisabledSubsystemsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return vnode.attrs.drainModeInfo.drainModeState()
      ? <InformationWhenInDrainMode drainModeInfo={vnode.attrs.drainModeInfo}/>
      : <InformationWhenNotInDrainMode drainModeInfo={vnode.attrs.drainModeInfo}/>;
  }
}
