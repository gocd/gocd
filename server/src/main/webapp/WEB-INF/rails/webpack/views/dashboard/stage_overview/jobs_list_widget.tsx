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

import m from "mithril";
import Stream from "mithril/stream";
import * as Icons from "views/components/icons";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {Agents} from "../../../models/agents/agents";
import {CheckboxField} from "../../components/forms/input_fields";
import * as styles from "./index.scss";
import {JobAgentWidget} from "./job_agent_widget";
import {JobProgressBarWidget} from "./job_progress_bar_widget";
import {JobStateWidget} from "./job_state_widget";
import {JobsViewModel} from "./models/jobs_view_model";
import {JobDuration, JobDurationStrategyHelper} from "./models/job_duration_stratergy_helper";
import {StageInstance} from "./models/stage_instance";
import {JobJSON} from "./models/types";

export interface Attrs {
  stageName: string;
  stageCounter: string | number;
  pipelineName: string;
  pipelineCounter: string | number;
  jobsVM: Stream<JobsViewModel>;
  agents: Stream<Agents>;
  lastPassedStageInstance: Stream<StageInstance | undefined>;
}

export interface State {
  getTableRowForJob: (jobName: JobJSON, lastPassedStageInstance: Stream<StageInstance | undefined>, agents: Stream<Agents>, checkboxStream: Stream<boolean>, jobDuration: JobDuration, longestTotalTime: number) => m.Children;
}

export class JobsListWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.getTableRowForJob = (job: JobJSON, lastPassedStageInstance: Stream<StageInstance | undefined>, agents: Stream<Agents>, checkboxStream: Stream<boolean>, jobDuration: JobDuration, longestTotalTime: number) => {
      const jobDetailsPageLink = `/go/tab/build/detail/${vnode.attrs.pipelineName}/${vnode.attrs.pipelineCounter}/${vnode.attrs.stageName}/${vnode.attrs.stageCounter}/${job.name}`;

      return <div class={styles.tableRow} data-test-id={`table-row-for-job-${job.name}`}>
        <div class={styles.checkboxCell} data-test-id={`checkbox-for-${job.name}`}>
          <CheckboxField property={checkboxStream}/>
        </div>
        <div class={styles.nameCell} data-test-id={`job-name-for-${job.name}`}>
          <div className={`${(styles as any)[job.result.toString().toLowerCase() as string]} ${styles.jobResult}`}/>
          <span title={job.name} className={styles.jobName}>{job.name}</span>
          <Icons.ConsoleLog onclick={() => window.open(jobDetailsPageLink)} target={"_blank"} iconOnly={true}/>
        </div>
        <div class={styles.stateCell} data-test-id={`state-for-${job.name}`}>
          <JobStateWidget job={job}/>
        </div>
        <div class={styles.statusCell} data-test-id={`status-for-${job.name}`}>
          <JobProgressBarWidget job={job} lastPassedStageInstance={lastPassedStageInstance} jobDuration={jobDuration} longestTotalTime={longestTotalTime}/>
        </div>
        <div class={styles.durationCell} data-test-id={`duration-for-${job.name}`}>
          {JobDurationStrategyHelper.getJobDurationForDisplay(job)}
        </div>
        <div class={styles.agentCell} data-test-id={`agent-for-${job.name}`}>
          <JobAgentWidget job={job} agents={agents}/>
        </div>
      </div>;
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    let longestTotalTime: number = 0;
    const duration: JobDuration[] = vnode.attrs.jobsVM().getJobs().map((job) => {
      const duration = JobDurationStrategyHelper.getDuration(job, vnode.attrs.lastPassedStageInstance());
      if (duration.totalTime > longestTotalTime) {
        longestTotalTime = duration.totalTime.valueOf();
      }
      return duration;
    });

    return <div data-test-id="jobs-list-widget" class={styles.jobListContainer}>
      <div class={styles.tableHeader} data-test-id="table-header">
        <div class={styles.checkboxCell} data-test-id="checkbox-header"/>
        <div class={styles.nameCell} data-test-id="job-name-header">Job Name</div>
        <div class={styles.stateCell} data-test-id="state-header">State</div>
        <div class={styles.statusCell} data-test-id="status-header">Status</div>
        <div class={styles.durationCell} data-test-id="duration-header">Duration</div>
        <div class={styles.agentCell} data-test-id="agent-header">Agent</div>
      </div>
      <div id="scrollable-jobs-table-body" class={styles.tableBody} data-test-id="table-body">
        {vnode.attrs.jobsVM().getJobs().map((job, index) => {
          const checkboxStream = vnode.attrs.jobsVM().checkedState.get(job.name)!;
          return vnode.state.getTableRowForJob(job, vnode.attrs.lastPassedStageInstance, vnode.attrs.agents, checkboxStream, duration[index], longestTotalTime);
        })}
      </div>
    </div>;
  }
}
