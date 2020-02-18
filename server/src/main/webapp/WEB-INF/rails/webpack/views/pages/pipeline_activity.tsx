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

import {AjaxPoller} from "helpers/ajax_poller";
import {ApiResult} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineActivity, Stage} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField, Size, TextAreaField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {PaginationWidget} from "views/components/pagination";
import {Pagination} from "views/components/pagination/models/pagination";
import {Page, PageState} from "views/pages/page";
import {ResultAwarePage} from "views/pages/page_operations";
import {PipelineActivityWidget} from "views/pages/pipeline_activity/pipeline_activity_widget";
import {ConfirmationDialog} from "./pipeline_activity/confirmation_modal";
import styles from "./pipeline_activity/index.scss";
import {PipelineActivityHeader} from "./pipeline_activity/page_header";

interface PageMeta {
  pipelineName: string;
  canOperatePipeline: boolean;
}

interface State {
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
  showCommentFor: Stream<string>;
  filterText: Stream<string>;
  meta: PageMeta;
}

export class PipelineActivityPage extends Page<null, State> implements ResultAwarePage<PipelineActivity>, State {
  protected service: PipelineActivityService = new PipelineActivityService();
  protected pagination                       = Stream<Pagination>(new Pagination(0, 10, 10));
  pipelineActivity                           = Stream<PipelineActivity>();
  showBuildCaseFor                           = Stream<string>();
  showCommentFor                             = Stream<string>();
  filterText                                 = Stream<string>();
  meta                                       = this.getMeta() as PageMeta;
  private poller: AjaxPoller<void>;

  constructor() {
    super();
    this.poller = new AjaxPoller({
      repeaterFn: this.fetchPipelineHistory.bind(this),
      initialIntervalSeconds: 10
    });
  }

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    this.startPolling();
  }

  pausePipeline() {
    const cause = Stream<string>();
    new ConfirmationDialog("Pause pipeline",
      <TextAreaField required={false} size={Size.MATCH_PARENT}
                     property={cause}
                     rows={5}
                     dataTestId="pause-pipeline-textarea"
                     label="Specify the reason why you want to stop scheduling on this pipeline"/>,
      () => this.service.pausePipeline(this.meta.pipelineName, cause())
        .then((result) => this.handleActionApiResponse(result, () => {
          this.pipelineActivity().paused(true);
          this.pipelineActivity().canForce(false);
          this.pipelineActivity().pauseCause(cause());
          const user = Page.readAttribute("data-user-display-name");
          this.pipelineActivity().pauseBy(user ? user : "");
        }))
    ).render();
  }

  unpausePipeline() {
    this.service.unpausePipeline(this.meta.pipelineName)
      .then((result) => this.handleActionApiResponse(result, () => {
        this.pipelineActivity().paused(false);
      }));
  }

  runPipeline() {
    this.service.run(this.meta.pipelineName)
      .then((result) => this.handleActionApiResponse(result, () => {
        this.pipelineActivity().canForce(false);
      }));
  }

  runStage(stage: Stage) {
    new ConfirmationDialog("Run stage",
      <div>{`Do you want to run the stage '${stage.stageName()}'?`}</div>,
      () => this.service
        .runStage(stage)
        .then((result) => this.handleActionApiResponse(result, () => {
          stage.getCanRun(false);
          stage.stageStatus("waiting");
        }))
    ).render();
  }

  addOrUpdateComment(updatedComment: string, labelOrCounter: string | number): void {
    this.service
      .commentOnPipelineRun(this.meta.pipelineName, labelOrCounter, updatedComment)
      .then((result) => this.handleActionApiResponse(result));
  }

  cancelStageInstance(stage: Stage) {
    new ConfirmationDialog("Cancel stage instance",
      <div>{"This will cancel all active jobs in this stage. Are you sure?"}</div>,
      () => this.service
        .cancelStageInstance(stage)
        .then((result) => this.handleActionApiResponse(result, () => {
          stage.getCanCancel(false);
          stage.stageStatus("waiting");
        }))
    ).render();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (!this.pipelineActivity()) {
      return <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>;
    }

    return [
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>,
      <PipelineActivityWidget pipelineActivity={vnode.state.pipelineActivity}
                              showBuildCaseFor={vnode.state.showBuildCaseFor}
                              showCommentFor={vnode.state.showCommentFor}
                              runPipeline={this.runPipeline.bind(this)}
                              runStage={this.runStage.bind(this)}
                              canOperatePipeline={this.meta.canOperatePipeline}
                              addOrUpdateComment={this.addOrUpdateComment.bind(this)}
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
    return this.fetchPipelineHistory();
  }

  onFailure(message: string) {
    this.flashMessage.setMessage(MessageType.alert, message);
    this.pageState = PageState.OK;
  }

  onSuccess(data: PipelineActivity) {
    this.pipelineActivity(data);
    this.pagination(new Pagination(data.start(), data.count(), data.perPage()));
    this.pageState = PageState.OK;
  }

  pageChangeCallback(pageNumber: number) {
    if (pageNumber === this.pagination().currentPageNumber()) {
      return;
    }
    this.pageState = PageState.LOADING;
    const offset = this.pipelineActivity().perPage() * (pageNumber - 1);
    this.pagination(new Pagination(offset, this.pagination().total, this.pagination().pageSize));
    this.fetchPipelineHistory();
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const title = <PipelineActivityHeader pipelineActivity={this.pipelineActivity()}
                                          unpausePipeline={this.unpausePipeline.bind(this)}
                                          pausePipeline={this.pausePipeline.bind(this)}
                                          isAdmin={Page.isUserAnAdmin()}
                                          isGroupAdmin={Page.isUserAGroupAdmin()}/>;
    return <HeaderPanel title={title} sectionName={this.pageName()} buttons={
      <SearchField property={this.filterText} label={"Search"}
                   dataTestId={"search-field"}
                   placeholder={"Filter history..."}
                   onchange={this.fetchData.bind(this)}/>
    }/>;
  }

  protected fetchPipelineHistory(): Promise<void> {
    this.service.activities(this.meta.pipelineName, this.pagination().offset, this.filterText(), this);
    return Promise.resolve();
  }

  private handleActionApiResponse(result: ApiResult<string>, onSuccess?: () => void) {
    this.startPolling();
    result.do((successResponse) => {
        const body = JSON.parse(successResponse.body);
        this.flashMessage.setMessage(MessageType.success, body.message);
        this.fetchPipelineHistory();
        if (onSuccess) {
          onSuccess();
        }
      },
      (errorResponse) => this.flashMessage.setMessage(MessageType.alert, errorResponse.message));
  }

  private startPolling() {
    if (this.meta.pipelineName) {
      this.poller.restart();
    }
  }
}
