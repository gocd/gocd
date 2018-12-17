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
  drainModeInfo: DrainModeInfo
}

class InformationWhenNotInDrainMode extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div data-test-id="info-when-not-in-drain-mode">
      <div data-test-id="info-when-not-in-drain-mode-header">Enabling GoCD Server Drain mode will:</div>
      <ul>
        <li data-test-id="stop-material">Stop the material subsystem so that no new materials are polled.</li>
        <li data-test-id="stop-config-repo">Stop polling on config repositories.</li>
        <li data-test-id="stop-pipeline-scheduling">Stop the scheduling subsystem so that no new pipelines are
          triggered (automatically or through timers).
        </li>
        <li data-test-id="stop-work-assignment">Stop the agent subsystem, so that no agents can pick up work if they’re
          idle.
        </li>
        <li data-test-id="stop-manual-trigger">Prevent users from triggering pipelines.</li>
        <li data-test-id="stop-config-changes">Prevent users from modifying configurations.</li>
        <li data-test-id="stop-db-changes">Prevent users from performing operations that modifies state in the database
          or filesystem.
        </li>
      </ul>
    </div>;
  }
}


interface RunningSystemAttrs {
  inProgess: boolean;
  dataTestId: string;
  text: string;
}

class RunningSystemWidget extends MithrilViewComponent<RunningSystemAttrs> {
  view(vnode: m.Vnode<RunningSystemAttrs>) {
    const icon = vnode.attrs.inProgess
      ? <Icons.Spinner iconOnly={true}/>
      : <Icons.Check iconOnly={true}/>;

    return <div data-test-id={vnode.attrs.dataTestId} class={styles.runningSystem}>
      {icon}
      <div class={styles.runningSystemText}>{vnode.attrs.text}</div>
    </div>;
  }
}

class InformationWhenInDrainMode extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isCompletelyDrained = vnode.attrs.drainModeInfo.isCompletelyDrained;

    const message = isCompletelyDrained
      ? <FlashMessage type={MessageType.info} message={"GoCD Server is completely drained."}/>
      : <FlashMessage type={MessageType.warning} message={"Some subsystems of GoCD are still in progress."}/>;

    const mduRunningSystem = vnode.attrs.drainModeInfo.runningSystem.mdu.count() === 0
      ? <RunningSystemWidget inProgess={false} dataTestId={"mdu-stopped"} text={"Stopped material subsystem."}/>
      : <RunningSystemWidget inProgess={true} dataTestId={"mdu-in-progress"}
                             text={"Waiting for material subsystem to stop.."}/>;

    const buildingJobsSystem = vnode.attrs.drainModeInfo.runningSystem.jobs.length === 0
      ? <RunningSystemWidget inProgess={false} dataTestId={"scheduling-system-stopped"} text={"Stopped scheduling subsystem."}/>
      : <RunningSystemWidget inProgess={true} dataTestId={"scheduling-system-in-progress"}
                             text={"Waiting for building jobs to finish.."}/>;

    return <div data-test-id="info-when-not-in-drain-mode">
      {message}
      <div data-test-id="running-sub-systems">
        {mduRunningSystem}
        <RunningSystemWidget inProgess={false} dataTestId={"config-repo-polling-stopped"} text={"Stopped polling on config repositories."}/>
        {buildingJobsSystem}
        <RunningSystemWidget inProgess={false} dataTestId={"agent-subsystem-stopped"} text={"Stopped assigning jobs to agents."}/>
        <RunningSystemWidget inProgess={false} dataTestId={"manual-trigger-stopped"} text={"Stopped pipeline triggers."}/>
        <RunningSystemWidget inProgess={false} dataTestId={"config-changes-stopped"} text={"Stopped config modifications."}/>
        <RunningSystemWidget inProgess={false} dataTestId={"db-changes-stopped"} text={"Stopped database and filesystem modifications."}/>
      </div>
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
