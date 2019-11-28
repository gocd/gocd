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
import m from "mithril"
import {PipelineRunInfo, Stage, StageConfigs} from "models/pipeline_activity/pipeline_activity";
import styles from "./index.scss";
import Stream from "mithril/stream";
import {MithrilViewComponent} from "jsx/mithril-component";
import {BuildCauseWidget} from "./build_cause_widget";
import * as Icons from "../../components/icons";
import {timeFormatter as TimeFormatter} from "helpers/time_formatter";
import s from "underscore.string";
import {SparkRoutes} from "helpers/spark_routes";
import {Link} from "views/components/link";

const classnames = bind(styles);

interface PipelineRunAttrs {
  pipelineRunInfo: PipelineRunInfo;
  pipelineName: string;
  showBuildCaseFor: Stream<string>;
  stageConfigs: StageConfigs;
  runStage: (stage: Stage) => void;
  cancelStageInstance: (stage: Stage) => void;
}

type StringOrNumber = string | number;

export class PipelineRunWidget extends MithrilViewComponent<PipelineRunAttrs> {
  view(vnode: m.Vnode<PipelineRunAttrs>): m.Children | void | null {
    const pipelineRunInfo = vnode.attrs.pipelineRunInfo;
    return <div data-test-id={this.dataTestId("instance-header", pipelineRunInfo.pipelineId())}
                class={styles.pipelineRun}>
      <div class={styles.runInfoSection}>
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
      </div>

      <div class={styles.stagesSection}>
        {pipelineRunInfo.stages().map((stage, index) => {
          return <div
            data-test-id={this.dataTestId("stage-status-wrapper", pipelineRunInfo.pipelineId(), stage.stageName())}
            class={classnames(styles.stage, {[styles.disabledIcon]: PipelineRunWidget.shouldDisableApprovalIcon(stage)})}>
            {this.getStageGateIcon(index, stage, vnode)}
            <div class={styles.stageStatusWrapper}>
              <span data-test-id={this.dataTestId("stage-status", pipelineRunInfo.pipelineId(), stage.stageName())}
                    class={classnames(PipelineRunWidget.stageStatusClass(stage.stageStatus()))}/>
              {this.getStageActions(stage, vnode)}
            </div>
          </div>;
        })}
      </div>
    </div>
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
      return [infoIcon,
        <div class={styles.stageInfoIconWrapper}>
          <Icons.Repeat iconOnly={true}
                        title="Rerun stage"
                        data-test-id={`rerun-${dataTestIdSuffix}`}
                        onclick={() => vnode.attrs.runStage(stage)}/>
        </div>];
    }

    if (stage.getCanCancel()) {
      return [infoIcon,
        <div class={styles.stageInfoIconWrapper}>
          <Icons.Close iconOnly={true}
                       data-test-id={`cancel-${dataTestIdSuffix}`}
                       title="Cancel stage"
                       onclick={() => vnode.attrs.cancelStageInstance(stage)}/>
        </div>];
    }

    return infoIcon;
  }

  private getStageGateIcon(index: number, stage: Stage, vnode: m.Vnode<PipelineRunAttrs>): m.Children {
    if (index === 0) {
      return;
    }

    const isAutoApproved = vnode.attrs.stageConfigs.isAutoApproved(stage.stageName());
    const dataTestId     = this.dataTestId(isAutoApproved ? "auto" : "manual", "gate", "icon", stage.stageName(), stage.stageId());

    if (isAutoApproved) {
      return <Icons.Forward iconOnly={true}
                            title="Auto approved"
                            disabled={PipelineRunWidget.shouldDisableApprovalIcon(stage)}
                            data-test-id={dataTestId}
                            onclick={() => vnode.attrs.runStage(stage)}/>;
    }

    return <Icons.StepForward iconOnly={true}
                              title="Awaiting approval"
                              disabled={PipelineRunWidget.shouldDisableApprovalIcon(stage)}
                              data-test-id={dataTestId}
                              onclick={() => vnode.attrs.runStage(stage)}/>;
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

  private dataTestId(...parts: StringOrNumber[]) {
    return s.slugify(parts.join("-").trim().toLowerCase());
  }
}
