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
import {Materials, ScmMaterialAttributes} from "models/drain_mode/material";
import {DrainModeInfo, Job, StageLocator} from "models/drain_mode/types";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Switch} from "views/components/forms/input_fields";
import {KeyValuePair} from "views/components/key_value_pair";
import {Table} from "views/components/table";
import {bind} from "classnames/bind";
import * as m from "mithril";
import * as Buttons from "views/components/buttons";
import * as styles from "./index.scss";

const classnames = bind(styles);

interface Attrs {
  drainModeInfo: DrainModeInfo;
  toggleDrainMode: (e: Event) => void;
  onCancelStage: (stageLocator: StageLocator, e: Event) => void;
}

export class DrainModeWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const drainModeInfo = vnode.attrs.drainModeInfo;

    const mayBeDrainInfo = (
      <div data-test-id="in-progress-subsystems" className={styles.inProgressSubsystems}>
        <DrainModeInfoWidget drainModeInfo={vnode.attrs.drainModeInfo} onCancelStage={vnode.attrs.onCancelStage}/>
      </div>
    );

    return (
      <div className={styles.drainModeWidget} data-test-id="drain-mode-widget">

        <div className={styles.drainModeDescription}>
          <p>
            The drain mode is a maintenance mode which a GoCD system administrator can put GoCD into so that it is
            safe to restart it or upgrade it without having running jobs reschedule when it is back.
          </p>
        </div>

        <div className={classnames(styles.drainModeInfo, styles.col2)}>
          <div className={styles.col}>
            <Switch
                    label={"Enable Drain Mode"}
                    onchange={vnode.attrs.toggleDrainMode}
                    property={drainModeInfo.drainModeState}/>
          </div>
          <div className={styles.col}>
            <p>Is server in drain mode: {`${drainModeInfo.drainModeState}`}</p>
            <p>Drain mode updated by: {drainModeInfo.metdata.updatedBy}</p>
            <p>Drain mode updated on: {drainModeInfo.metdata.updatedOn}</p>
          </div>
        </div>
        {mayBeDrainInfo}
      </div>
    );
  }
}

interface InfoAttrs {
  drainModeInfo: DrainModeInfo;
  onCancelStage: (stageLocator: StageLocator, e: Event) => void;
}

export class DrainModeInfoWidget extends MithrilViewComponent<InfoAttrs> {
  view(vnode: m.Vnode<InfoAttrs>): m.Children {
    return [
      <JobInfoWidget jobs={vnode.attrs.drainModeInfo.runningSystem.groupJobsByStage()}
                     onCancelStage={vnode.attrs.onCancelStage}/>,
      <MDUInfoWidget materials={vnode.attrs.drainModeInfo.runningSystem.mdu}/>
    ];
  }
}

interface JobInfoAttrs {
  jobs?: Map<string, Job[]>;
  onCancelStage: (stageLocator: StageLocator, e: Event) => void;
}

export class JobInfoWidget extends MithrilViewComponent<JobInfoAttrs> {

  view(vnode: m.Vnode<JobInfoAttrs>): m.Children {
    const runningStages: m.Children = [];
    if (!vnode.attrs.jobs || vnode.attrs.jobs.size === 0) {
      runningStages.push(<em>No running stages.</em>);
    } else {
      vnode.attrs.jobs.forEach((jobs, key) => {
        const stageLocator = StageLocator.fromStageLocatorString(key);
        runningStages.push(
          <CollapsiblePanel header={<KeyValuePair inline={true} data={stageLocator.asMap()}/>}
                            actions={JobInfoWidget.stageCancelButton(stageLocator, vnode)}>
            <Table headers={["Job", "State", "Agent"]} data={JobInfoWidget.dataForTable(jobs)}/>
          </CollapsiblePanel>
        );
      });
    }

    return (
      <div>
        <h3 className={styles.runningSystemHeader}>Running stages</h3>
        <div>{runningStages}</div>
      </div>
    );
  }

  private static dataForTable(jobs: Job[]): m.Child[][] {
    return jobs.reduce((groups: any[], job) => {
      const jobName = <a href={`/go/tab/build/detail/${job.locator()}`}>{job.jobName}</a>;
      groups.push([jobName, job.scheduledDate, JobInfoWidget.agentLink(job)]);
      return groups;
    }, []);
  }

  private static agentLink(job: Job): m.Children {
    if (job.agentUUID) {
      return <Buttons.Primary onclick={this.goTo.bind(this, `/go/agents/${job.agentUUID}`)}
                              data-test-id="agent-link"
                              small={true}>Agent</Buttons.Primary>;
    }
    return <em>(Not assigned)</em>;
  }

  private static stageCancelButton(stageLocator: StageLocator, vnode: m.Vnode<JobInfoAttrs>) {
    return (<Buttons.Primary onclick={vnode.attrs.onCancelStage.bind(vnode.attrs, stageLocator)}
                             data-test-id="job-link"
                             small={true}>Cancel stage</Buttons.Primary>);
  }

  private static goTo(href: string, event: Event): void {
    event.stopPropagation();
    window.location.href = href;
  }
}

class MDUInfoAttrs {
  materials?: Materials;
}

export class MDUInfoWidget extends MithrilViewComponent<MDUInfoAttrs> {
  view(vnode: m.Vnode<MDUInfoAttrs>): m.Children {
    let inProgressMaterials;

    if (!vnode.attrs.materials || vnode.attrs.materials.count() === 0) {
      inProgressMaterials = <em>No material update is in progress.</em>;
    } else {
      inProgressMaterials = vnode.attrs.materials.allScmMaterials().map((material) => {
        const attributes = material.attributes() as ScmMaterialAttributes;
        const nameOrUrl  = attributes.name() ? attributes.name() : attributes.url();
        const headerMap  = new Map([
                                     ["Type", material.type()],
                                     ["Name", nameOrUrl],
                                     ["Auto Update", attributes.name()],
                                     ["Started At", material.mduStartTime().toString()]
                                   ]);

        return (
          <CollapsiblePanel
            header={<KeyValuePair inline={true} data={headerMap}/>}>
            <KeyValuePair data={material.attributesAsMap()}/>
          </CollapsiblePanel>
        );
      });
    }

    return (
      <div>
        <h3 className={styles.runningSystemHeader}>Running MDUs</h3>
        {inProgressMaterials}
      </div>
    );
  }
}
