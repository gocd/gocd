/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {timeFormatter as TimeFormatter} from "helpers/time_formatter";
import m from "mithril";
import {JobRunHistoryJSON, State} from "models/agent_job_run_history";
import {KeyValuePair} from "views/components/key_value_pair";
import {Modal, Size} from "views/components/modal";
import {Table} from "views/components/table";
import styles from "./index.scss";

export class AgentJobStateTransitionModal extends Modal {
  private readonly job: JobRunHistoryJSON;

  constructor(job: JobRunHistoryJSON) {
    super(Size.small);
    this.job = job;
  }

  body(): m.Children {
    const tableData = this.job.job_state_transitions.map((transition) => {
      return [<i>{transition.state}</i>, TimeFormatter.format(transition.state_change_time)];
    });

    return <div data-test-id={`job-state-transitions-for-${this.jobRepresentation()}`}>
      <div class={styles.jobStateTransitionInformationContainer} data-test-id="additional-information-container">
        <KeyValuePair data={new Map(
          [
            ["Job", this.jobRepresentation()],
            ["Wait Time", `${this.findTimeDifference("Scheduled", "Assigned")}`],
            ["Building Time", `${this.findTimeDifference("Assigned", "Completed")}`],
            ["Total Time", `${this.findTimeDifference("Scheduled", "Completed")}`]
          ])
        }/>
      </div>
      <Table data={tableData} headers={["Agent State", "Time"]}/>
    </div>;
  }

  title(): string {
    return "Job State Transitions";
  }

  private jobRepresentation() {
    return `${this.job.pipeline_name}/${this.job.pipeline_counter}/${this.job.stage_name}/${this.job.stage_counter}/${this.job.job_name}`;
  }

  private findTimeDifference(from: State, to: State) {
    const fromState = this.job.job_state_transitions.find((t) => t.state === from);
    const toState   = this.job.job_state_transitions.find((t) => t.state === to);

    if (!fromState || !toState) {
      throw new Error(`Expected to find a Job State Transition entry for state(s): ${from} and ${to}`);
    }

    return TimeFormatter.formattedTimeDiff(fromState.state_change_time, toState.state_change_time);
  }
}
