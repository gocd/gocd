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

import {bind} from "classnames/bind";
import m from "mithril";
import Stream from "mithril/stream";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {CheckboxField} from "../../components/forms/input_fields";
import * as styles from "./index.scss";
import {JobsViewModel} from "./models/jobs_view_model";

import pipelineHistoryStyles from "../../pages/pipeline_activity/index.scss";
import {JobProgressBarWidget} from "./job_progress_bar_widget";
import {JobJSON} from "./models/types";
import {StageInstance} from "./models/stage_instance";
import {JobStateWidget} from "./job_state_widget";
import {JobDurationStrategyHelper} from "./models/job_duration_stratergy_helper";

const classnames = bind(pipelineHistoryStyles);

export interface Attrs {
  jobsVM: Stream<JobsViewModel>;
  lastPassedStageInstance: Stream<StageInstance | undefined>;
}

export interface State {
  getTableRowForJob: (jobName: JobJSON, lastPassedStageInstance: Stream<StageInstance | undefined>) => m.Children;
}

export class JobsListWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.getTableRowForJob = (job: JobJSON, lastPassedStageInstance: Stream<StageInstance | undefined>) => {
      return <div class={styles.tableRow} data-test-id={`table-row-for-job-${job.name}`}>
        <div class={styles.checkboxCell} data-test-id={`checkbox-for-${job.name}`}>
          <CheckboxField property={Stream()}/>
        </div>
        <div class={styles.nameCell} data-test-id={`job-name-for-${job.name}`}>
          <div className={`${styles[job.result.toString().toLowerCase()]} ${styles.jobResult}`}/>
          <div className={styles.jobName}>{job.name}</div>
        </div>
        <div class={styles.stateCell} data-test-id={`state-for-${job.name}`}>
          <JobStateWidget job={job}/>
        </div>
        <div class={styles.statusCell} data-test-id={`status-for-${job.name}`}>
          <JobProgressBarWidget job={job} lastPassedStageInstance={lastPassedStageInstance}/>
        </div>
        <div class={styles.durationCell} data-test-id={`duration-for-${job.name}`}>
          {JobDurationStrategyHelper.getJobDurationForDisplay(job)}
        </div>
        <div class={styles.agentCell} data-test-id={`agent-for-${job.name}`}>
          agent1
        </div>
      </div>;
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    return <div data-test-id="jobs-list-widget" class={styles.jobListContainer}>
      <div class={styles.tableHeader} data-test-id="table-header">
        <div class={styles.checkboxCell} data-test-id="checkbox-header"/>
        <div class={styles.nameCell} data-test-id="job-name-header">Name</div>
        <div class={styles.stateCell} data-test-id="state-header">State</div>
        <div class={styles.statusCell} data-test-id="status-header">Status</div>
        <div class={styles.durationCell} data-test-id="duration-header">Duration</div>
        <div class={styles.agentCell} data-test-id="agent-header">Agent</div>
      </div>
      {vnode.attrs.jobsVM().getJobs().map(job => vnode.state.getTableRowForJob(job, vnode.attrs.lastPassedStageInstance))}
    </div>;
  }
}
