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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Config, Group, PipelineActivity, PipelineRunInfo, Stage} from "models/pipeline_activity/pipeline_activity";
import styles from "./index.scss";
import {PipelineRunWidget} from "./pipeline_run_info_widget";
import {ShowForceBuildActionWidget} from "./show_force_build_action_widget";

const classnames = bind(styles);

interface Attrs {
  canOperatePipeline: boolean;
  canAdministerPipeline: boolean;
  pipelineUsingTemplate?: string;
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
  showCommentFor: Stream<string>;
  runPipeline: (name: string) => void;
  runStage: (stage: Stage) => void;
  stageOverviewState: any;
  showStageOverview: (pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number, status: any, e: any) => void;
  cancelStageInstance: (stage: Stage) => void;
  addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void;
}

export class PipelineActivityWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const pipelineActivity = vnode.attrs.pipelineActivity();
    if (!pipelineActivity) {
      return;
    }

    return <div class={styles.pipelineActivity}>
      <table class={styles.pipelineActivityTable}>
        {
          pipelineActivity.groups().map((group: Group, index: number) => {
            return [
              <HeaderWidget config={group.config}/>,
              this.renderForceBuildAction(index, group, pipelineActivity, vnode),
              <GroupWidget pipelineName={pipelineActivity.pipelineName()}
                           group={group}
                           showBuildCaseFor={vnode.attrs.showBuildCaseFor}
                           showCommentFor={vnode.attrs.showCommentFor}
                           runStage={vnode.attrs.runStage}
                           stageOverviewState={vnode.attrs.stageOverviewState}
                           showStageOverview={vnode.attrs.showStageOverview}
                           canOperatePipeline={vnode.attrs.canOperatePipeline}
                           canAdministerPipeline={vnode.attrs.canAdministerPipeline}
                           pipelineUsingTemplate={vnode.attrs.pipelineUsingTemplate}
                           addOrUpdateComment={vnode.attrs.addOrUpdateComment}
                           cancelStageInstance={vnode.attrs.cancelStageInstance}/>];
          })
        }
      </table>
    </div>;
  }

  renderForceBuildAction(index: number, group: Group, pipelineActivity: PipelineActivity, vnode: m.Vnode<Attrs>) {
    if (index === 0 && pipelineActivity.showForceBuildButton()) {
      return <ShowForceBuildActionWidget group={group}
                                         pipelineName={pipelineActivity.pipelineName()}
                                         canForce={pipelineActivity.canForce}
                                         runPipeline={vnode.attrs.runPipeline}/>;
    }
  }
}

interface GroupAttrs {
  group: Group;
  pipelineName: string;
  canOperatePipeline: boolean;
  canAdministerPipeline: boolean;
  pipelineUsingTemplate?: string;
  showBuildCaseFor: Stream<string>;
  showCommentFor: Stream<string>;
  runStage: (stage: Stage) => void;
  stageOverviewState: any;
  showStageOverview: (pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number, status: any, e: any) => void;
  cancelStageInstance: (stage: Stage) => void;
  addOrUpdateComment: (comment: string, counterOrLabel: string | number) => void;
}

class GroupWidget extends MithrilViewComponent<GroupAttrs> {
  view(vnode: m.Vnode<GroupAttrs, this>): m.Children {
    return vnode.attrs.group.history().map((history: PipelineRunInfo) => {
      return <PipelineRunWidget pipelineName={vnode.attrs.pipelineName}
                                pipelineRunInfo={history}
                                stageConfigs={vnode.attrs.group.config().stages()}
                                showBuildCaseFor={vnode.attrs.showBuildCaseFor}
                                showCommentFor={vnode.attrs.showCommentFor}
                                runStage={vnode.attrs.runStage}
                                stageOverviewState={vnode.attrs.stageOverviewState}
                                showStageOverview={vnode.attrs.showStageOverview}
                                canOperatePipeline={vnode.attrs.canOperatePipeline}
                                canAdministerPipeline={vnode.attrs.canAdministerPipeline}
                                pipelineUsingTemplate={vnode.attrs.pipelineUsingTemplate}
                                addOrUpdateComment={vnode.attrs.addOrUpdateComment}
                                cancelStageInstance={vnode.attrs.cancelStageInstance}/>;
    });
  }
}

interface HeaderAttrs {
  config: Stream<Config>;
}

class HeaderWidget extends MithrilViewComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, this>): m.Children | void | null {
    return <tr class={styles.groupHeader}>
      <td class={classnames(styles.left, styles.header)} data-test-id="instance-header">Instance</td>
      <td class={styles.right}>
        {vnode.attrs.config().stages().map((stage, index) => {
          const marginClass = index === 0
            ? styles.noMargin
            : stage.isAutoApproved() ? styles.margin23 : styles.margin18;
          return <span class={classnames(styles.header, styles.stageName, marginClass)} data-test-id={`stage-${stage.name()}`}>
                    {stage.name()}
                  </span>;
        })}
      </td>
    </tr>;
  }
}
