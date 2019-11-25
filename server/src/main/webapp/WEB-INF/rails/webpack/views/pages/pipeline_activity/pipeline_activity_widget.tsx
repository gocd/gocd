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

const classnames = bind(styles);

interface Attrs {
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
}

export class PipelineActivityWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const pipelineActivity = vnode.attrs.pipelineActivity();
    if (!pipelineActivity) {
      return;
    }

    return <div class={styles.pipelineActivity}>
      {
        pipelineActivity.groups().map((group: Group) => {
          return <GroupWidget pipelineName={pipelineActivity.pipelineName()}
                              group={group}
                              showBuildCaseFor={vnode.attrs.showBuildCaseFor}/>;
        })
      }
    </div>
  }
}

interface GroupAttrs {
  group: Group;
  pipelineName: string;
  showBuildCaseFor: Stream<string>;
}

class GroupWidget extends MithrilViewComponent<GroupAttrs> {
  view(vnode: m.Vnode<GroupAttrs, this>): m.Children {
    return <div class={styles.group}>
      <HeaderWidget config={vnode.attrs.group.config}/>
      {vnode.attrs.group.history().map((history: PipelineRunInfo) => {
        return <PipelineRunWidget pipelineName={vnode.attrs.pipelineName}
                                  pipelineRunInfo={history}
                                  stageConfigs={vnode.attrs.group.config().stages()}
                                  showBuildCaseFor={vnode.attrs.showBuildCaseFor}/>;
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
