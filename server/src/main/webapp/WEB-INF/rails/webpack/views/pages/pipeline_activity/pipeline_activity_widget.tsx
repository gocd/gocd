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
import {Config, Group, PipelineActivity, PipelineRunInfo} from "models/pipeline_activity/pipeline_activity";
import styles from "./index.scss"
import {PipelineRunWidget} from "./pipeline_run_info_widget";
import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {ShowForceBuildActionWidget} from "./show_force_build_action_widget";
import {FlashMessageModelWithTimeout} from "../../components/flash_message";

const classnames = bind(styles);

interface Attrs {
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
  service: PipelineActivityService;
  message: FlashMessageModelWithTimeout;
}

export class PipelineActivityWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const pipelineActivity = vnode.attrs.pipelineActivity();
    if (!pipelineActivity) {
      return;
    }

    return <div class={styles.pipelineActivity}>
      {
        pipelineActivity.groups().map((group: Group, index: number) => {
          return [
            <HeaderWidget config={group.config}/>,
            this.renderForceBuildAction(index, group, pipelineActivity, vnode),
            <GroupWidget pipelineName={pipelineActivity.pipelineName()}
                         group={group}
                         showBuildCaseFor={vnode.attrs.showBuildCaseFor}
                         service={vnode.attrs.service}
                         message={vnode.attrs.message}/>
          ];
        })
      }
    </div>
  }

  renderForceBuildAction(index: number, group: Group, pipelineActivity: PipelineActivity, vnode: m.Vnode<Attrs>) {
    if (index === 0 && pipelineActivity.showForceBuildButton()) {
      return <ShowForceBuildActionWidget group={group}
                                         pipelineName={pipelineActivity.pipelineName()}
                                         canForce={pipelineActivity.canForce}
                                         service={vnode.attrs.service}
                                         message={vnode.attrs.message}/>;
    }
  }
}

interface GroupAttrs {
  group: Group;
  pipelineName: string;
  showBuildCaseFor: Stream<string>;
  service: PipelineActivityService;
  message: FlashMessageModelWithTimeout;
}

class GroupWidget extends MithrilViewComponent<GroupAttrs> {
  view(vnode: m.Vnode<GroupAttrs, this>): m.Children {
    return <div class={styles.group}>
      {vnode.attrs.group.history().map((history: PipelineRunInfo) => {
        return <PipelineRunWidget pipelineName={vnode.attrs.pipelineName}
                                  pipelineRunInfo={history}
                                  stageConfigs={vnode.attrs.group.config().stages()}
                                  showBuildCaseFor={vnode.attrs.showBuildCaseFor}
                                  service={vnode.attrs.service}
                                  message={vnode.attrs.message}/>;
      })}
    </div>;
  }
}

interface HeaderAttrs {
  config: Stream<Config>;
}

class HeaderWidget extends MithrilViewComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, this>): m.Children | void | null {
    return <div class={styles.pipelineRunHeader}>
      <div class={classnames(styles.runInfoSection, styles.header)}
           data-test-id="instance-header">Instance
      </div>
      <div class={styles.stagesSection}>
        {vnode.attrs.config().stages().map((stage) => {
          return <span class={classnames(styles.header, styles.stageName)} data-test-id={`stage-${stage.name()}`}>
                    {stage.name()}
                  </span>;
        })}
      </div>
    </div>
  }
}
