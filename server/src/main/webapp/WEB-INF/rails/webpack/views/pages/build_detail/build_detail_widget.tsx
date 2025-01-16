/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import {SparkRoutes} from "helpers/spark_routes";
import {timeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Agent} from "models/agents/agents";
import {findTransition, JobInstanceJSON} from "models/job_detail/job_detail";
import {JobIdentifier} from "models/shared/job_identifier";
import moment from "moment";
import {Link} from "views/components/link";
import styles from './index.scss';

export interface Attrs {
  jobIdentifier: JobIdentifier;
  jobInstance: Stream<JobInstanceJSON>;
  agent: Stream<Agent>;
  buildCause: string;
}

// if buildstatus == building/completing
// if has build history
//    Elapsed: ... Longer by:
//    Elapsed: ... ETA: ... (progress bar)

// if first build
//    Elapsed: ...

// if completed (passed/failed)
// Duration: ...


////
// if has agent_uuid
//    if alive: ip+link to agent
//    else: ip
// else Not yet assigned
export class BuildDetailWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const jobInstance: JobInstanceJSON = vnode.attrs.jobInstance();

    let progressLabel: m.Children;
    let progressValue: m.Children;
    let completedAt: m.Children = 'In progress';

    if (jobInstance.state === "Completed") {
      progressLabel = 'Duration:';
      const endTime = moment(findTransition(jobInstance, "Completed")!.state_change_time as number);
      const startTime = moment(findTransition(jobInstance, "Building")!.state_change_time as number);

      const duration = moment.duration(endTime.diff(startTime)).format('h [hours] m [minutes] s [seconds]');
      progressValue = duration;

      completedAt = <span title={timeFormatter.formatInServerTime(endTime)}>{timeFormatter.format(endTime)}</span>;
    } else if (jobInstance.state === 'Building' || jobInstance.state === 'Completing') {
      progressLabel = 'Progress:';
      const jobScheduleTime = moment(findTransition(jobInstance, 'Scheduled')!.state_change_time as number);

      const elapsedTime = moment.duration(moment().diff(jobScheduleTime));

      progressValue = [
        <span>elapsed {elapsedTime}</span>
      ];

      const hasPreviousBuild = true;
      if (hasPreviousBuild) {
        // const remainingTime = moment.duration()
        progressValue.push(<span>remaining xxx</span>);
      }
    }

    const scheduledDate = moment(jobInstance.scheduled_date);

    return (
      <div class={styles.jobInfoBox}>
        <ul class={styles.jobInfo}>

          <li class={styles.keyValuePair}>
            <label class={styles.key}>Scheduled onx:</label>
            <span class={styles.value} data-test-id='scheduled-at'
                  title={timeFormatter.formatInServerTime(scheduledDate)}>{timeFormatter.format(scheduledDate)}</span>
          </li>

          <li class={styles.keyValuePair}>
            <label class={styles.key}>Agent:</label>
            <span class={styles.value} data-test-id='agent-id'>{BuildDetailWidget.agentDetails(vnode)}</span>
          </li>

          <li class={styles.keyValuePair}>
            <label class={styles.key}>Completed on:</label>
            <span class={styles.value} data-test-id='completed-at'>{completedAt}</span>
          </li>

          <li class={styles.keyValuePair}>
            <label class={styles.key}>Build cause:</label>
            <span class={styles.value} data-test-id='build-cause'>{vnode.attrs.buildCause}</span>
          </li>

          <li class={styles.keyValuePair}>
            <label class={styles.key} data-test-id='progress-label'>{progressLabel}</label>
            <span class={styles.value} data-test-id='progress'>{progressValue}</span></li>
        </ul>
      </div>
    );
  }

  private static agentDetails(vnode: m.Vnode<Attrs>): m.Children {
    const jobInstance: JobInstanceJSON = vnode.attrs.jobInstance();
    const agentUuid = jobInstance.agent_uuid;
    const agent = vnode.attrs.agent();

    if (agentUuid) {
      if (agent) {
        return (<Link href={SparkRoutes.agentJobRunHistoryPath(agentUuid)}>{agent.hostname} ({agent.ipAddress})</Link>);
      } else {
        return jobInstance.agent_uuid;
      }
    }
    return 'Not yet assigned';
  }
}
