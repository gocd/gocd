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
import {Job, StageLocator} from "models/drain_mode/types";
import * as Buttons from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {KeyValuePair} from "views/components/key_value_pair";
import {Table} from "views/components/table";
import * as styles from "views/pages/drain_mode/index.scss";

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
            <Table headers={["Job", "State", "Scheduled At", "Agent"]} data={JobInfoWidget.dataForTable(jobs)}/>
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
      groups.push([jobName, job.state, job.scheduledDate, JobInfoWidget.agentLink(job)]);
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
