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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Job, Stage, StageLocator} from "models/maintenance_mode/types";
import * as Buttons from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import * as Icons from "views/components/icons/index";
import {KeyValuePair} from "views/components/key_value_pair";
import {Table} from "views/components/table";
import styles from "views/pages/maintenance_mode/index.scss";

interface JobInfoAttrs {
  stages: Stage[];
  title: m.Children;
  onCancelStage: (stageLocator: StageLocator) => void;
}

export class JobInfoWidget extends MithrilViewComponent<JobInfoAttrs> {
  view(vnode: m.Vnode<JobInfoAttrs>): m.Children {
    const runningStages: m.Children = [];

    if (vnode.attrs.stages.length === 0) {
      runningStages.push(<em data-test-id="no-running-stages">No running stages.</em>);
    } else {
      vnode.attrs.stages.forEach((stage: Stage) => {
        const stageLocator = stage.getStageLocator();
        runningStages.push(
          <CollapsiblePanel header={<KeyValuePair data-test-id={`header-for-${stageLocator.toString()}`}
                                                  inline={true}
                                                  data={stageLocator.asMap()}/>}
                            actions={[
                              stage.isStageCancelInProgress() ? <Icons.Spinner iconOnly={true}/> : null,
                              <Buttons.Secondary data-test-id={`cancel-stage-btn-for-${stageLocator.toString()}`}
                                                 small={true}
                                                 disabled={stage.isStageCancelInProgress()}
                                                 onclick={(e: MouseEvent) => JobInfoWidget.onStageCancel(vnode, stage, e)}>
                                {stage.isStageCancelInProgress() ? `Canceling..` : `Cancel stage`}
                              </Buttons.Secondary>]}>
            <Table data-test-id={`table-for-${stageLocator.toString()}`}
                   headers={["Job", "State", "Scheduled At", "Agent"]}
                   data={JobInfoWidget.dataForTable(stage.getJobs())}/>
          </CollapsiblePanel>
        );
      });
    }

    return (
      <CollapsiblePanel header={<h3 class={styles.runningSystemHeader}>{vnode.attrs.title}</h3>} expanded={true}>
        <div>{runningStages}</div>
      </CollapsiblePanel>
    );
  }

  private static onStageCancel(vnode: m.Vnode<JobInfoAttrs>, stage: Stage, e: Event) {
    e.stopPropagation();
    stage.startCancelling();
    vnode.attrs.onCancelStage(stage.getStageLocator());
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

  private static goTo(href: string, event: Event): void {
    event.stopPropagation();
    window.location.href = href;
  }
}
