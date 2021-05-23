/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import classNames from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Group, StageConfig} from "models/pipeline_activity/pipeline_activity";
import {Primary} from "views/components/buttons";
import * as Icons from "views/components/icons";
import styles from "./index.scss";

const classnames = classNames.bind(styles);

interface ShowForceBuildActionAttrs {
  group: Group;
  canForce: Stream<boolean>;
  pipelineName: string;
  runPipeline: (name: string) => void;
}

export class ShowForceBuildActionWidget extends MithrilViewComponent<ShowForceBuildActionAttrs> {
  view(vnode: m.Vnode<ShowForceBuildActionAttrs, this>): m.Children {
    return <tr class={classnames(styles.groupContent, styles.forceBuild)}>
      <td class={styles.left}>
        <Primary onclick={() => vnode.attrs.runPipeline(vnode.attrs.pipelineName)}
                 small={true}
                 dataTestId="force-trigger-pipeline"
                 disabled={!vnode.attrs.canForce()}>
          <Icons.Trigger iconOnly={true}/>
        </Primary>
      </td>
      <td class={styles.right}>
        {vnode.attrs.group.config().stages().map((stage, index) => {
          return <div class={classnames(styles.stageBoxPipelineActivity, styles.disabledIcon)}>
            {ShowForceBuildActionWidget.getStageApprovalIcon(index, stage)}
            <span class={classnames(styles.stageStatus, styles.unknown)}/>
          </div>;
        })}
      </td>
    </tr>;
  }

  private static getStageApprovalIcon(index: number, stage: StageConfig): m.Children {
    if (index === 0) {
      return;
    }

    if (stage.isAutoApproved()) {
      return <Icons.Forward iconOnly={true} disabled={true}/>;
    }

    return <span className={styles.manualWrapper}><Icons.StepForward iconOnly={true} disabled={true}/></span>;
  }
}
