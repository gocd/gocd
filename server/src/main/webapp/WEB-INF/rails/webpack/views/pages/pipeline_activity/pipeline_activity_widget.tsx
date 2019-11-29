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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Config, Group, PipelineActivity, PipelineRunInfo, Stage} from "models/pipeline_activity/pipeline_activity";
import styles from "./index.scss"
import {PipelineRunWidget} from "./pipeline_run_info_widget";
import {ShowForceBuildActionWidget} from "./show_force_build_action_widget";

const classnames = bind(styles);

interface Attrs {
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
  runPipeline: (name: string) => void;
  runStage: (stage: Stage) => void;
  cancelStageInstance: (stage: Stage) => void;
}

export class PipelineActivityWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const pipelineActivity = vnode.attrs.pipelineActivity();
    if (!pipelineActivity) {
      return;
    }

    return <table class={styles.pipelineActivity}>
      {
        pipelineActivity.groups().map((group: Group, index: number) => {
          return [
            <HeaderWidget config={group.config}/>,
            this.renderForceBuildAction(index, group, pipelineActivity, vnode),
            <GroupWidget pipelineName={pipelineActivity.pipelineName()}
                         group={group}
                         showBuildCaseFor={vnode.attrs.showBuildCaseFor}
                         runStage={vnode.attrs.runStage}
                         cancelStageInstance={vnode.attrs.cancelStageInstance}/>];
        })
      }
    </table>
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
  showBuildCaseFor: Stream<string>;
  runStage: (stage: Stage) => void;
  cancelStageInstance: (stage: Stage) => void;
}

class GroupWidget extends MithrilViewComponent<GroupAttrs> {
  view(vnode: m.Vnode<GroupAttrs, this>): m.Children {
    return vnode.attrs.group.history().map((history: PipelineRunInfo) => {
      return <PipelineRunWidget pipelineName={vnode.attrs.pipelineName}
                                pipelineRunInfo={history}
                                stageConfigs={vnode.attrs.group.config().stages()}
                                showBuildCaseFor={vnode.attrs.showBuildCaseFor}
                                runStage={vnode.attrs.runStage}
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
        {vnode.attrs.config().stages().map((stage) => {
          return <span class={classnames(styles.header, styles.stageName)} data-test-id={`stage-${stage.name()}`}>
                    {stage.name()}
                  </span>;
        })}
      </td>
    </tr>
  }
}
