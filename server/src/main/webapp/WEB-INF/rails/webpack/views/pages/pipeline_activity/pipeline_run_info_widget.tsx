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

import {bind} from "classnames/bind";
import {SparkRoutes} from "helpers/spark_routes";
import {timeFormatter as TimeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineRunInfo, Stage, StageConfigs} from "models/pipeline_activity/pipeline_activity";
import s from "underscore.string";
import * as Icons from "views/components/icons";
import {Link} from "views/components/link";
import {BuildCauseWidget} from "./build_cause_widget";
import {CommentWidget} from "./comment_widget";
import styles from "./index.scss";

const classnames = bind(styles);

interface PipelineRunAttrs {
  canOperatePipeline: boolean;
  pipelineRunInfo: PipelineRunInfo;
  pipelineName: string;
  showBuildCaseFor: Stream<string>;
  showCommentFor: Stream<string>;
  stageConfigs: StageConfigs;
  runStage: (stage: Stage) => void;
  cancelStageInstance: (stage: Stage) => void;
  addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void;
}

type StringOrNumber = string | number;

export class PipelineRunWidget extends MithrilViewComponent<PipelineRunAttrs> {
  view(vnode: m.Vnode<PipelineRunAttrs>): m.Children | void | null {
    const pipelineRunInfo = vnode.attrs.pipelineRunInfo;

    return <tr class={styles.groupContent}
               data-test-id={this.dataTestId("instance-header", pipelineRunInfo.pipelineId())}>
      <td class={styles.left}>
        <div class={classnames(styles.run, styles.header)}>
          <span data-test-id={this.dataTestId("counter-for", pipelineRunInfo.pipelineId())}>
            {pipelineRunInfo.label().substr(0, 17)}
          </span>
          <span data-test-id={this.dataTestId("vsm-for", pipelineRunInfo.pipelineId())}>
            {PipelineRunWidget.getVSMLink(vnode, pipelineRunInfo)}
          </span>
        </div>
        <div class={styles.revision}>Revision: {pipelineRunInfo.revision()}</div>
        <div class={styles.scheduleInfo}
             data-test-id={this.dataTestId("time-for", pipelineRunInfo.pipelineId())}>
          {PipelineRunWidget.getTime(pipelineRunInfo.scheduledTimestamp())}
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
      <td class={styles.right}>
        {pipelineRunInfo.stages().map((stage, index) => {
          return <div
            data-test-id={this.dataTestId("stage-status-wrapper", pipelineRunInfo.pipelineId(), stage.stageName())}
            class={classnames(styles.stage, {[styles.disabledIcon]: PipelineRunWidget.shouldDisableApprovalIcon(stage)})}>
            {this.getStageGateIcon(index, stage, vnode)}
            <div class={styles.stageStatusWrapper}>
              <span data-test-id={this.dataTestId("stage-status", pipelineRunInfo.pipelineId(), stage.stageName())}
                    class={classnames(PipelineRunWidget.stageStatusClass(stage.stageStatus()))}/>
              <div class={styles.stageInfoIconWrapper}>
                {this.getStageActions(stage, vnode)}
              </div>
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

  private static stageStatusClass(status: string) {
    if (!status) {
      return;
    }

    if (status.trim().toLowerCase() === "building") {
      return styles.building;
    } else if (status.trim().toLowerCase() === "failed") {
      return styles.failed;
    } else if (status.trim().toLowerCase() === "cancelled") {
      return styles.cancelled;
    } else if (status.trim().toLowerCase() === "passed") {
      return styles.passed;
    } else if (status.trim().toLowerCase() === "waiting") {
      return styles.waiting;
    }

    return styles.unknown;
  }

  private static getVSMLink(vnode: m.Vnode<PipelineRunAttrs>, pipelineRunInfo: PipelineRunInfo) {
    if (pipelineRunInfo.label().toLowerCase() === "unknown") {
      return <span class={styles.disabled}>VSM</span>;
    }
    const link = SparkRoutes.pipelineVsmLink(vnode.attrs.pipelineName, pipelineRunInfo.counterOrLabel());
    return <a href={link}>VSM</a>;
  }

  private static getTime(timestamp: Date) {
    return timestamp ? TimeFormatter.format(timestamp) : "N/A";
  }

  private getStageActions(stage: Stage, vnode: m.Vnode<PipelineRunAttrs>): m.Children {
    if (!stage.scheduled()) {
      return;
    }

    const dataTestIdSuffix = this.dataTestId("stage", "action", "icon", stage.stageName(), stage.stageId());
    const infoIcon         = <Link target="_blank" href={`/go/pipelines/${stage.stageLocator()}`}>
      <Icons.InfoCircle iconOnly={true} data-test-id={`info-${dataTestIdSuffix}`}/>
    </Link>;
    if (stage.getCanRun()) {
      return <div class={styles.stageInfoIconContainer}>
        {infoIcon}
        <Icons.Repeat iconOnly={true}
                      title="Rerun stage"
                      data-test-id={`rerun-${dataTestIdSuffix}`}
                      onclick={() => vnode.attrs.runStage(stage)}/>
      </div>;
    }

    if (stage.getCanCancel()) {
      return <div class={styles.stageInfoIconContainer}>
        {infoIcon}
        <Icons.Close iconOnly={true}
                     data-test-id={`cancel-${dataTestIdSuffix}`}
                     title="Cancel stage"
                     onclick={() => vnode.attrs.cancelStageInstance(stage)}/>
      </div>;
    }

    return <div class={styles.stageInfoIconContainer}>{infoIcon}</div>;
  }

  private getStageGateIcon(index: number, stage: Stage, vnode: m.Vnode<PipelineRunAttrs>): m.Children {
    if (index === 0) {
      return;
    }

    const isAutoApproved = vnode.attrs.stageConfigs.isAutoApproved(stage.stageName());
    const dataTestId     = this.dataTestId(isAutoApproved ? "auto" : "manual", "gate", "icon", stage.stageName(), stage.stageId());

    const disabled = PipelineRunWidget.shouldDisableApprovalIcon(stage);
    if (isAutoApproved) {
      return <Icons.Forward iconOnly={true}
                            title={PipelineRunWidget.getTitle(stage)}
                            disabled={disabled}
                            data-test-id={dataTestId}
                            onclick={() => vnode.attrs.runStage(stage)}/>;
    }

    return <Icons.StepForward iconOnly={true}
                              title={PipelineRunWidget.getTitle(stage)}
                              disabled={disabled}
                              data-test-id={dataTestId}
                              onclick={() => vnode.attrs.runStage(stage)}/>;
  }

  private dataTestId(...parts: StringOrNumber[]) {
    return s.slugify(parts.join("-").trim().toLowerCase());
  }
}
