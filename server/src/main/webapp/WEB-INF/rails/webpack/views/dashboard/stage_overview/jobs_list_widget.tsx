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
      const jobName = job.name;
      return <tr>
        <td data-test-id={`checkbox-for-job-${jobName}`} class={styles.jobCheckbox}>
          <CheckboxField property={Stream()}/>
        </td>
        <td>
          {job.name}
        </td>
        <td class={styles.jobName}>
          <JobProgressBarWidget job={job} lastPassedStageInstance={lastPassedStageInstance}/>
        </td>
        <td> in progress</td>
        <td> agent 1</td>
      </tr>;
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    return <div data-test-id="jobs-list-widget" class={styles.jobListContainer}>
      <table>
        <tr>
          <th data-test-id="checkbox-header"/>
          <th data-test-id="job-name-header">Job Name</th>
          <th data-test-id="status-header">Status</th>
          <th data-test-id="duration-header">Duration</th>
          <th data-test-id="agent-header">Agent</th>
        </tr>
        {
          ['buildingJobNames', 'failedJobNames', 'cancelledJobNames', 'passedJobNames'].map((m) => {
            return (vnode.attrs.jobsVM()[m]() as JobJSON[])
              .map(job => vnode.state.getTableRowForJob(job, vnode.attrs.lastPassedStageInstance));
          })
        }
      </table>
    </div>;
  }
}
