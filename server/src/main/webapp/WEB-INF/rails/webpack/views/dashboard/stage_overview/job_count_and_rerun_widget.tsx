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
import {ErrorResponse} from "../../../helpers/api_request_builder";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {ButtonGroup, Secondary} from "../../components/buttons";
import {FlashMessageModelWithTimeout, MessageType} from "../../components/flash_message";
import * as styles from "./index.scss";
import {JobsViewModel} from "./models/jobs_view_model";

export interface Attrs {
  stageName: string;
  stageCounter: string | number;
  pipelineName: string;
  pipelineCounter: string | number;
  flashMessage: FlashMessageModelWithTimeout;
  jobsVM: Stream<JobsViewModel>;
  inProgressStageFromPipeline: Stream<any | undefined>;
  userSelectedStageCounter: Stream<string | number>;
}

export interface State {
  rerunFailed: (vnode: m.Vnode<Attrs, State>) => void;
  rerunSelected: (vnode: m.Vnode<Attrs, State>) => void;
}

export class JobCountAndRerunWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    function getResultHandler(attrs: Attrs) {
      return (result: any) => {
        result.do((successResponse: any) => {
          attrs.flashMessage.setMessage(MessageType.success, JSON.parse(successResponse.body).message);
        }, (errorResponse: ErrorResponse) => {
          attrs.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
        });
      };
    }

    vnode.state.rerunFailed = (vnode: m.Vnode<Attrs, State>) => {
      vnode.attrs.jobsVM().rerunFailedJobs(vnode.attrs.pipelineName, vnode.attrs.pipelineCounter, vnode.attrs.stageName, vnode.attrs.userSelectedStageCounter()).then(getResultHandler(vnode.attrs));
    };

    vnode.state.rerunSelected = (vnode: m.Vnode<Attrs, State>) => {
      vnode.attrs.jobsVM().rerunSelectedJobs(vnode.attrs.pipelineName, vnode.attrs.pipelineCounter, vnode.attrs.stageName, vnode.attrs.userSelectedStageCounter()).then(getResultHandler(vnode.attrs));
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    const failedJobNames = vnode.attrs.jobsVM().failedJobNames().map(j => j.name);
    let rerunFailedTitle = `Rerun all the failed jobs from the stage. Reruning failed jobs will reschedule '${failedJobNames.join(', ')}' job(s).`;
    let rerunSelectedTitle = `Rerun selected jobs from the stage.`;

    let disableRerunFailed = false, disableRerunSelected = false;

    if (vnode.attrs.inProgressStageFromPipeline() !== undefined) {
      rerunFailedTitle = `Can not rerun failed jobs. Stage '${vnode.attrs.inProgressStageFromPipeline().name}' from the current pipeline is still in progress.`;
      rerunSelectedTitle = `Can not rerun selected jobs. Stage '${vnode.attrs.inProgressStageFromPipeline().name}' from the current pipeline is still in progress.`;
      disableRerunFailed = disableRerunSelected = true;
    }

    const isStageCompleted = vnode.attrs.jobsVM().buildingJobNames().length === 0;
    if (!isStageCompleted) {
      rerunFailedTitle = `Can not rerun failed jobs. Some jobs from the stage are still in progress.`;
      rerunSelectedTitle = `Can not rerun selected jobs. Some jobs from the stage are still in progress.`;
      disableRerunFailed = disableRerunSelected = true;
    }

    if (failedJobNames.length === 0) {
      disableRerunFailed = true;
      rerunFailedTitle = `Can not rerun failed jobs. No jobs from the current stage are in failed state.`;
    }

    if (vnode.attrs.jobsVM().getCheckedJobNames().length === 0) {
      disableRerunSelected = true;
      rerunSelectedTitle = `Can not rerun selected jobs. No jobs have been selected for rerun.`;
    }

    return <div class={styles.jobCountAndRerunContainer} data-test-id="job-count-and-rerun-container">
      <div class={styles.jobRerunContainer} data-test-id="job-rerun-container">
        <ButtonGroup>
          <Secondary title={rerunFailedTitle} disabled={disableRerunFailed} small={true}
                     onclick={vnode.state.rerunFailed.bind(vnode.state, vnode)}>Rerun Failed</Secondary>
          <Secondary title={rerunSelectedTitle} disabled={disableRerunSelected} small={true}
                     onclick={vnode.state.rerunSelected.bind(vnode.state, vnode)}>Rerun Selected</Secondary>
        </ButtonGroup>
      </div>
      <div class={styles.jobCountContainer} data-test-id="job-cont-container">
        <div class={styles.countContainer} data-test-id="in-progress-jobs-container">
          <div class={styles.countLabel}>Building :</div>
          <div className={styles.countText}> {vnode.attrs.jobsVM().buildingJobNames().length}</div>
        </div>
        <div class={styles.countContainer} data-test-id="passed-jobs-container">
          <div className={styles.countLabel}>Passed :</div>
          <div class={styles.countText}> {vnode.attrs.jobsVM().passedJobNames().length}</div>
        </div>
        <div class={styles.countContainer} data-test-id="failed-jobs-container">
          <div className={styles.countLabel}>Failed :</div>
          <div class={styles.countText}> {vnode.attrs.jobsVM().failedJobNames().length}</div>
        </div>
      </div>
    </div>;
  }

}
