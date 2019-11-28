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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineActivity, Stage} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Page} from "views/pages/page";
import {ResultAwarePage} from "views/pages/page_operations";
import {PipelineActivityWidget} from "views/pages/pipeline_activity/pipeline_activity_widget";
import {PaginationWidget} from "../components/pagination";
import {Pagination} from "../components/pagination/models/pagination";
import styles from "./pipeline_activity/index.scss";
import {ApiResult} from "../../helpers/api_request_builder";
import {ConfirmationDialog} from "./pipeline_activity/confirmation_modal";
import {AjaxPoller} from "../../helpers/ajax_poller";

interface State {
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
}

export class PipelineActivityPage extends Page<null, State> implements ResultAwarePage<PipelineActivity>, State {
  pipelineActivity                         = Stream<PipelineActivity>();
  showBuildCaseFor                         = Stream<string>();
  private service: PipelineActivityService = new PipelineActivityService();
  private pagination                       = Stream<Pagination>(new Pagination(0, 10, 10));

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    new AjaxPoller({repeaterFn: this.fetchData.bind(this, vnode), initialIntervalSeconds: 10}).start();
  }

  runPipeline() {
    this.service.run(this.pipelineActivity().pipelineName())
      .then(this.handleActionApiResponse.bind(this));
  }

  runStage(stage: Stage) {
    new ConfirmationDialog("Run stage",
      <div>{`Do you want to run the stage '${stage.stageName()}'?`}</div>,
      () => this.service
        .runStage(stage)
        .then((result) => this.handleActionApiResponse(result, () => PipelineActivityPage.markStageBuilding(stage)))
    ).render();
  }

  cancelStageInstance(stage: Stage) {
    new ConfirmationDialog("Cancel stage instance",
      <div>{"This will cancel all active jobs in this stage. Are you sure?"}</div>,
      () => this.service
        .cancelStageInstance(stage)
        .then((result) => this.handleActionApiResponse(result, () => PipelineActivityPage.markStageCancelled(stage)))
    ).render();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (!this.pipelineActivity()) {
      return;
    }

    return [
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>,
      <PipelineActivityWidget pipelineActivity={vnode.state.pipelineActivity}
                              showBuildCaseFor={vnode.state.showBuildCaseFor}
                              runPipeline={this.runPipeline.bind(this)}
                              runStage={this.runStage.bind(this)}
                              cancelStageInstance={this.cancelStageInstance.bind(this)}/>,
      <div class={styles.paginationWrapper}>
        <PaginationWidget pagination={this.pagination()} onPageChange={this.pageChangeCallback.bind(this)}/>
      </div>
    ];
  }

  pageName(): string {
    return "Pipeline Activity";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.fetchPipelineHistory(this.pagination().offset);
    return Promise.resolve();
  }

  onFailure(message: string) {
    this.flashMessage.setMessage(MessageType.alert, message);
    this.setErrorState();
  }

  onSuccess(data: PipelineActivity) {
    this.pipelineActivity(data);
    this.pagination(new Pagination(data.start(), data.count(), data.perPage()));
  }

  private handleActionApiResponse(result: ApiResult<string>, onSuccess?: () => void) {
    result.do((successResponse) => {
        const body    = JSON.parse(successResponse.body);
        const isError = PipelineActivityPage.isErrorOrWarningMessage(body.message);
        this.flashMessage.setMessage(isError ? MessageType.alert : MessageType.success, body.message);
        this.fetchPipelineHistory.bind(this, this.pagination().offset);
        if (onSuccess && !isError) {
          onSuccess();
        }
      },
      (errorResponse) => this.onFailure(errorResponse.message))
  }

  private pageChangeCallback(pageNumber: number) {
    const offset = this.pipelineActivity().perPage() * (pageNumber - 1);
    this.fetchPipelineHistory(offset);
  }

  private fetchPipelineHistory(start: number) {
    this.service.activities(PipelineActivityPage.pipelineNameFromUrl(), start, this);
  }

  private static pipelineNameFromUrl(): string {
    return window.location.pathname.split("/").pop()!;
  }

  private static markStageBuilding(stage: Stage) {
    stage.stageStatus("building");
    stage.getCanRun(false);
    stage.getCanCancel(true);
  }

  private static markStageCancelled(stage: Stage) {
    stage.stageStatus("cancelled");
    stage.getCanRun(true);
    stage.getCanCancel(false);
  }

  private static isErrorOrWarningMessage(message: string) {
    return ["Stage is not active. Cancellation Ignored."].indexOf(message) != -1;
  }

}
