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
import {SparkRoutes} from "helpers/spark_routes";
import {timeFormatter as TimeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineRunInfo, Stage, StageConfigs} from "models/pipeline_activity/pipeline_activity";
import s from "underscore.string";
import * as Icons from "views/components/icons";
import {StageOverview} from "../../dashboard/stage_overview";
import {BuildCauseWidget} from "./build_cause_widget";
import {CommentWidget} from "./comment_widget";
import styles from "./index.scss";

const classnames = bind(styles);

interface PipelineRunAttrs {
  canOperatePipeline: boolean;
  canAdministerPipeline: boolean;
  pipelineUsingTemplate?: string;
  pipelineRunInfo: PipelineRunInfo;
  pipelineName: string;
  showBuildCaseFor: Stream<string>;
  showCommentFor: Stream<string>;
  stageConfigs: StageConfigs;
  stageOverviewState: any;
  showStageOverview: (pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number, status: any, e: any) => void;
  runStage: (stage: Stage) => void;
  addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void;
}

type StringOrNumber = string | number;

export class PipelineRunWidget extends MithrilViewComponent<PipelineRunAttrs> {

  public static stageStatusClass(status: string) {
    if (!status) {
      return;
    }

    switch (status.trim().toLowerCase()) {
      case "building":
        return styles.building;
      case "failing":
        return styles.failing;
      case "failed":
        return styles.failed;
      case "cancelled":
        return styles.cancelled;
      case "passed":
        return styles.passed;
      case "waiting":
        return styles.waiting;
      default:
        return styles.unknown;
    }
  }

  view(vnode: m.Vnode<PipelineRunAttrs>): m.Children | void | null {
    const pipelineRunInfo = vnode.attrs.pipelineRunInfo;

    return <tr class={styles.groupContent}
               data-test-id={this.dataTestId("pipeline-instance", pipelineRunInfo.label())}>
      <td class={styles.left} data-test-id={"meta"}>
        <div class={classnames(styles.run, styles.header)}>
          <span data-test-id={"counter"}>
            {pipelineRunInfo.label().substr(0, 17)}
          </span>
          <span data-test-id={"vsm"}>
            {PipelineRunWidget.getVSMLink(vnode, pipelineRunInfo)}
          </span>
        </div>
        <div class={styles.revision}>Revision: {pipelineRunInfo.revision()}</div>
        <div class={styles.scheduleInfo}
             data-test-id={"time"}
             title={PipelineRunWidget.getTimeServer(pipelineRunInfo.scheduledTimestamp())}>
          {PipelineRunWidget.getTimeLocal(pipelineRunInfo.scheduledTimestamp())}
        </div>
        <BuildCauseWidget pipelineRunInfo={pipelineRunInfo}
                          showBuildCaseFor={vnode.attrs.showBuildCaseFor}
                          show={Stream(vnode.attrs.showBuildCaseFor() === pipelineRunInfo.counterOrLabel())}/>
        <CommentWidget comment={pipelineRunInfo.comment}
                       counterOrLabel={pipelineRunInfo.counterOrLabel()}
                       canOperatePipeline={vnode.attrs.canOperatePipeline}
                       addOrUpdateComment={vnode.attrs.addOrUpdateComment}
                       showCommentFor={vnode.attrs.showCommentFor}
                       show={Stream(vnode.attrs.showCommentFor() === `${pipelineRunInfo.counterOrLabel()}`)}/>
      </td>
      <td class={styles.right} data-test-id="stage-status">
        {pipelineRunInfo.stages().map((stage, index) => {

          const pipelineName = stage.pipelineName();
          const pipelineCounter = stage.pipelineCounter();
          const stageName = stage.stageName();
          const stageCounter = stage.stageCounter();

          let optionalStageOverview: m.Children | undefined;

          let isModelOpen = vnode.attrs.stageOverviewState.isOpen(pipelineName, pipelineCounter, stageName, stageCounter);
          if (!isModelOpen && vnode.attrs.stageOverviewState.getPipelineCounter() === pipelineCounter) {
            const previousStageCounter = `${((+stageCounter) - 1)}`;
            const isOpenForPreviousStageInstance = vnode.attrs.stageOverviewState.isOpen(pipelineName, pipelineCounter, stageName, previousStageCounter);

            if (isOpenForPreviousStageInstance) {
              isModelOpen = true;
              vnode.attrs.stageOverviewState.hide();
              vnode.attrs.showStageOverview(pipelineName, pipelineCounter, stageName, stageCounter, stage.stageStatus(), {stopPropagation: () => undefined});
            }
          }

          const isScheduled: boolean = stage.scheduled();

          if (isModelOpen) {
            // @ts-ignore
            stage.status = stage.stageStatus();
            // @ts-ignore
            stage.counter = stage.stageCounter();
            // @ts-ignore
            stage.canOperate = vnode.attrs.canOperatePipeline;

            //@ts-ignore
            pipelineRunInfo.stages().forEach((s: Stage) => s.name = s.stageName());

            optionalStageOverview = [<StageOverview pipelineName={stage.pipelineName()}
                                                   isDisplayedOnPipelineActivityPage={true}
                                                   canAdminister={vnode.attrs.canAdministerPipeline}
                                                   pipelineCounter={stage.pipelineCounter()}
                                                   stageName={stageName}
                                                   stageCounter={stageCounter}
                                                   stages={pipelineRunInfo.stages()}
                                                   key={vnode.attrs.stageOverviewState.modelId()}
                                                   templateName={vnode.attrs.pipelineUsingTemplate}
                                                   stageInstanceFromDashboard={stage}
                                                   stageOverviewVM={vnode.attrs.stageOverviewState.model}
                                                   stageStatus={stage.stageStatus()}/>];
          }

          return <div
            data-test-id={this.dataTestId("stage-status-container", stage.stageName())}
            class={classnames(styles.stageBoxPipelineActivity, {[styles.disabledIcon]: PipelineRunWidget.shouldDisableApprovalIcon(stage)})}>
            {this.getStageGateIcon(index, stage, vnode)}
            <div class={styles.stageStatusWrapper}>
              <span data-test-id={this.dataTestId("stage-status", stage.stageName())}
                    onclick={(e: any) => {
                      if (isScheduled) {
                        return vnode.attrs.showStageOverview(pipelineName, pipelineCounter, stageName, stageCounter, stage.stageStatus(), e);
                      }
                    }}
                    class={classnames(styles.stageStatus, PipelineRunWidget.stageStatusClass(stage.stageStatus()))}/>
              {optionalStageOverview}
            </div>
          </div>;
        })}
      </td>
    </tr>;
  }

  private static getTitle(stage: Stage) {
    if (stage.errorMessage()) {
      return stage.errorMessage();
    }

    if (!stage.approvedBy()) {
      return "Awaiting approval";
    }
    if (stage.approvedBy() === "changes") {
      return "Automatically approved";
    }
    return `Approved by ${stage.approvedBy()}`;
  }

  private static shouldDisableApprovalIcon(stage: Stage) {
    if (!stage.getCanRun()) {
      return true;
    }

    return stage.scheduled();
  }

  private static getVSMLink(vnode: m.Vnode<PipelineRunAttrs>, pipelineRunInfo: PipelineRunInfo) {
    if (pipelineRunInfo.label().toLowerCase() === "unknown") {
      return <span class={styles.disabled}>VSM</span>;
    }
    const link = SparkRoutes.pipelineVsmLink(vnode.attrs.pipelineName, pipelineRunInfo.counterOrLabel());
    return <a href={link}>VSM</a>;
  }

  private static getTimeLocal(timestamp: Date) {
    return timestamp ? TimeFormatter.format(timestamp) : "N/A";
  }

  private static getTimeServer(timestamp: Date) {
    return timestamp ? TimeFormatter.formatInServerTime(timestamp) : null;
  }

  private getStageGateIcon(index: number, stage: Stage, vnode: m.Vnode<PipelineRunAttrs>): m.Children {
    if (index === 0) {
      return;
    }

    const isAutoApproved = vnode.attrs.stageConfigs.isAutoApproved(stage.stageName());
    const dataTestId     = this.dataTestId("gate", "icon");

    const disabled = PipelineRunWidget.shouldDisableApprovalIcon(stage);
    if (isAutoApproved) {
      return <Icons.Forward iconOnly={true}
                            title={PipelineRunWidget.getTitle(stage)}
                            disabled={disabled}
                            data-test-id={dataTestId}
                            onclick={() => vnode.attrs.runStage(stage)}/>;
    }

    return <span className={styles.manualWrapper}>
      <Icons.StepForward iconOnly={true}
                         title={PipelineRunWidget.getTitle(stage)}
                         disabled={disabled}
                         data-test-id={dataTestId}
                         onclick={() => vnode.attrs.runStage(stage)}/>
    </span>;
  }

  private dataTestId(...parts: StringOrNumber[]) {
    return s.slugify(parts.join("-").trim().toLowerCase());
  }
}
