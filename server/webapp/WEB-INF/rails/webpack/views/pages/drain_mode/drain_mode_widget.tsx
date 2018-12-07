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
import {DrainModeSettings} from "models/drain_mode/drain_mode_settings";
import {Materials, ScmMaterialAttributes} from "models/drain_mode/material";
import {DrainModeInfo, Job} from "models/drain_mode/types";
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
  settings: DrainModeSettings;
  drainModeInfo?: DrainModeInfo;
  onSave: (drainModeSettings: DrainModeSettings, e: Event) => void;
  onReset: (drainModeSettings: DrainModeSettings, e: Event) => void;
}

export class DrainModeWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const settings = vnode.attrs.settings;
    let mayBeDrainInfo;
    if (vnode.attrs.drainModeInfo) {
      mayBeDrainInfo = (
        <div data-test-id="in-progress-subsystems" className={styles.inProgressSubsystems}>
          <DrainModeInfoWidget drainModeInfo={vnode.attrs.drainModeInfo}/>
        </div>
      );
    }

    return (
      <div className={styles.drainModeWidget} data-test-id="drain-mode-widget">
        <div className={styles.drainModeDescription}>
          <p>
            Some description about what is drain mode.
          </p>
        </div>

        <div className={classnames(styles.drainModeInfo, styles.col2)}>
          <div className={styles.col}>
            <Switch label={"Toggle Drain Mode"} property={settings.drain}/>
            <div className="button-wrapper">
              <Buttons.Primary onclick={vnode.attrs.onSave.bind(vnode.attrs, settings)}
                               data-test-id={"save-drain-mode-settings"}>Save</Buttons.Primary>
              <Buttons.Reset onclick={vnode.attrs.onReset.bind(vnode.attrs, settings)}
                             data-test-id={"reset-drain-mode-settings"}>Reset</Buttons.Reset>
            </div>
          </div>
          <div className={styles.col}>
            <p>Is server in drain mode: {`${settings.drain()}`}</p>
            <p>Drain mode updated by: {settings.updatedBy}</p>
            <p>Drain mode updated on: {settings.updatedOn}</p>
          </div>
        </div>
        {mayBeDrainInfo}
      </div>
    );
  }
}

interface InfoAttrs {
  drainModeInfo: DrainModeInfo;
}

export class DrainModeInfoWidget extends MithrilViewComponent<InfoAttrs> {
  view(vnode: m.Vnode<InfoAttrs>): m.Children {
    return [
      <JobInfoWidget jobs={vnode.attrs.drainModeInfo.runningSystem.groupJobsByStage()}/>,
      <MDUInfoWidget materials={vnode.attrs.drainModeInfo.runningSystem.mdu}/>
    ];
  }
}

class JobInfoAttrs {
  jobs?: Map<string, Job[]>;
}

export class JobInfoWidget extends MithrilViewComponent<JobInfoAttrs> {

  view(vnode: m.Vnode<JobInfoAttrs>): m.Children {
    if (!vnode.attrs.jobs || vnode.attrs.jobs.size === 0) {
      return "No running jobs";
    }

    const runningStages: m.Children = [];
    vnode.attrs.jobs.forEach((jobs, key) => {
      runningStages.push(
        <div className={styles.panel}>
          <h3 className={styles.panelHeader}>{key}</h3>
          <div className={styles.panelBody}>
            <Table headers={["Job", "State", "Action"]} data={JobInfoWidget.dataForTable(jobs)}/>
          </div>
        </div>
      );
    });

    return (
      <div className={styles.panel}>
        <h3 className={styles.panelHeader}>Running jobs</h3>
        <div className={styles.panelBody}>{runningStages}</div>
      </div>
    );
  }

  private static dataForTable(jobs: Job[]): m.Child[][] {
    return jobs.reduce((groups: any[], job) => {
      groups.push([job.jobName, job.scheduledDate, JobInfoWidget.links(job)]);
      return groups;
    }, []);
  }

  private static links(job: Job): m.Children {
    let mayBeAgentLink;
    if (job.agentUUID) {
      mayBeAgentLink = <Buttons.Primary onclick={this.goTo.bind(this, `/go/agents/${job.agentUUID}`)}
                                        data-test-id="agent-link"
                                        small={true}>Agent</Buttons.Primary>;
    }

    return (
      <div>
        <Buttons.Primary onclick={this.goTo.bind(this, `/go/tab/build/detail/${job.locator()}`)}
                         data-test-id="job-link"
                         small={true}>Job</Buttons.Primary>
        {mayBeAgentLink}
      </div>
    );
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
      inProgressMaterials = "No material update is in progress.";
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
      <div className={styles.panel}>
        <h3 className={styles.panelHeader}>MDU state</h3>
        <div className={styles.panelBody}>{inProgressMaterials}</div>
      </div>
    );
  }
}
