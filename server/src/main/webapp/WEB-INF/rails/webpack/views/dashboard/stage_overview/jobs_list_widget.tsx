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
import {PipelineRunWidget} from "../../pages/pipeline_activity/pipeline_run_info_widget";
import * as styles from "./index.scss";
import {JobsViewModel} from "./models/jobs_view_model";
import {StageInstance} from "./models/stage_instance";

import pipelineHistoryStyles from "../../pages/pipeline_activity/index.scss";

const classnames = bind(pipelineHistoryStyles);

export interface Attrs {
  stageInstance: Stream<StageInstance>;
}

export interface State {
  getTableHeaders: () => string[];

  getTableRowForJob: (jobName: string, jobsVM: JobsViewModel) => m.Children;
}

export class JobsListWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.getTableHeaders = () => {
      return ["", "Status", "Job Name", "Duration", "Agent"];
    };

    vnode.state.getTableRowForJob = (jobName: string, jobsVM: JobsViewModel) => {
      return <tr>
        <td data-test-id={`checkbox-for-job-${jobName}`} class={styles.jobCheckbox}>
          <CheckboxField property={Stream()}/>
        </td>
        <td>
          <div class={classnames(pipelineHistoryStyles.stage)}>
            <div class={pipelineHistoryStyles.stageStatusWrapper}>
              <span class={classnames(PipelineRunWidget.stageStatusClass("building"))}/>
              <div class={pipelineHistoryStyles.stageInfoIconWrapper}>
              </div>
            </div>
          </div>
        </td>
        <td class={styles.jobName}> {jobName} </td>
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
          <th data-test-id="status-header">Status</th>
          <th data-test-id="job-name-header">Job Name</th>
          <th data-test-id="duration-header">Duration</th>
          <th data-test-id="agent-header">Agent</th>
        </tr>
        {
          ['buildingJobNames', 'failedJobNames', 'cancelledJobNames', 'passedJobNames'].map((m) => {
            return (vnode.attrs.stageInstance().jobsVM()[m]() as string[])
              .map(jobName => vnode.state.getTableRowForJob(jobName, vnode.attrs.stageInstance().jobsVM()));
          })
        }
      </table>
    </div>;
  }
}
