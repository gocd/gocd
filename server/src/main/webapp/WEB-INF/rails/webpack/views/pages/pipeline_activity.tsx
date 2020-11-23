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

import {docsUrl} from "gen/gocd_version";
import {AjaxPoller} from "helpers/ajax_poller";
import {ApiResult} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineActivity, Stage} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {Link} from "views/components/link";
import {PaginationWidget} from "views/components/pagination";
import {Pagination} from "views/components/pagination/models/pagination";
// @ts-ignore
import state from "views/dashboard/models/stage_overview_state";
import {Page, PageState} from "views/pages/page";
import {ResultAwarePage} from "views/pages/page_operations";
import {PipelinePauseHeader} from "views/pages/pipeline_activity/common/pipeline_pause_header";
import {PipelineActivityWidget} from "views/pages/pipeline_activity/pipeline_activity_widget";
import {StageOverviewViewModel} from "../dashboard/stage_overview/models/stage_overview_view_model";
import {ConfirmationDialog} from "./pipeline_activity/confirmation_modal";
import styles from "./pipeline_activity/index.scss";

interface PageMeta {
  pipelineName: string;
  canOperatePipeline: boolean;
  canAdministerPipeline: boolean;
  pipelineUsingTemplate?: string;
}

interface State {
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
  showCommentFor: Stream<string>;
  filterText: Stream<string>;
  meta: PageMeta;
  showStageOverview?: (pipelineName: string, pipelineCounter: string | number | any, stageName: string, stageCounter: string | number | any, status: any, e: any) => void;
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
  private stageOverviewState = state.StageOverviewState;

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

    vnode.state.showStageOverview = (pipelineName, pipelineCounter, stageName, stageCounter, stageStatus, e) => {
      this.stageOverviewState.show(pipelineName, pipelineCounter, stageName, stageCounter);
      StageOverviewViewModel.initialize(pipelineName, pipelineCounter, stageName, stageCounter, stageStatus).then((result) => this.stageOverviewState.model(result));
      e.stopPropagation();
    };

    // close changes popup when clicked outside.
    document.body.onclick = () => {
      vnode.state.showBuildCaseFor("");
      this.stageOverviewState.hide();
    };
  }

  runPipeline() {
    new ConfirmationDialog("Run pipeline",
      <div>{`Do you want to run the pipeline '${this.meta.pipelineName}'?`}</div>,
                           () => this.service
                                     .run(this.meta.pipelineName)
                                     .then((result) => this.handleActionApiResponse(result, () => {
                                       this.pipelineActivity().canForce(false);
                                     }))
    ).render();
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
                              stageOverviewState={this.stageOverviewState}
                              //@ts-ignore
                              showStageOverview={vnode.state.showStageOverview}
                              canOperatePipeline={this.meta.canOperatePipeline}
                              canAdministerPipeline={this.meta.canAdministerPipeline}
                              pipelineUsingTemplate={this.meta.pipelineUsingTemplate}
                              addOrUpdateComment={this.addOrUpdateComment.bind(this)}/>,
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
    const offset   = this.pipelineActivity().perPage() * (pageNumber - 1);
    this.pagination(new Pagination(offset, this.pagination().total, this.pagination().pageSize));
    this.fetchPipelineHistory();
  }

  helpText(): m.Children {
    return <div>
      The pipeline activity helps GoCD users to see the status of historical runs of a pipeline. The current page makes it easier to browse
      through the pipeline runs by filtering pipeline runs using label, user or material revision (e.g. git commit sha).
      <Link href={docsUrl('advanced_usage/pipeline_activity.html')} externalLinkIcon={true}> Learn More</Link>
    </div>;
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    let title: m.Children = <div data-test-id="pipeline-pause-header"/>;

    if (this.pipelineActivity()) {
      title = <PipelinePauseHeader pipelineName={this.pipelineActivity().pipelineName()}
                                   flashMessage={this.flashMessage}
                                   shouldShowPauseUnpause={this.pipelineActivity().canPause()}
                                   shouldShowPipelineSettings={Page.isUserAnAdmin() || Page.isUserAGroupAdmin()}/>;
    }

    return <HeaderPanel title={title} sectionName={this.pageName()} buttons={
      <SearchField property={this.filterText} label={"Search"}
                   dataTestId={"search-field"}
                   placeholder={"Filter history..."}
                   onchange={this.fetchData.bind(this)}/>
    } help={this.helpText()}/>;
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
