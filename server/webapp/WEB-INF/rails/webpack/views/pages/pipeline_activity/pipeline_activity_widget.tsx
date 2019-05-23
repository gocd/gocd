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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Config, Group, PipelineActivity, PipelineRunInfo} from "models/pipeline_activity/pipeline_activity";
import {Stream} from "mithril/stream";
import * as styles from "./index.scss";

interface Attrs {
  pipelineActivity: Stream<PipelineActivity>;
}

export class PipelineActivityWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {

    const pipelineActivity = vnode.attrs.pipelineActivity();
    if (!pipelineActivity) {
      return <div>Nothing to display.</div>;
    }


    return pipelineActivity.groups().map((group: Group) => {
      return <GroupWidget group={group}/>;
    });
  }
}

interface PipelineRunAttrs {
  pipelineRunInfo: PipelineRunInfo;
}

class PipelineRunWidget extends MithrilViewComponent<PipelineRunAttrs> {
  view(vnode: m.Vnode<PipelineRunAttrs, this>): m.Children | void | null {
    return <div>
      <div>{vnode.attrs.pipelineRunInfo.counterOrLabel()}</div>
      <div>Changes</div>
      <div>TRigger by admin</div>
    </div>;
  }
}

interface GroupWidgetAttrs {
  group: Group;
}

class GroupWidget extends MithrilViewComponent<GroupWidgetAttrs> {
  view(vnode: m.Vnode<GroupWidgetAttrs, this>): m.Children | void | null {
    return <div>
      <HeaderWidget config={vnode.attrs.group.config}/>
      {vnode.attrs.group.history().map((history: PipelineRunInfo) => {
        return <PipelineRunWidget pipelineRunInfo={history}/>;
      })}
    </div>;
  }
}

interface HeaderAttrs {
  config: Stream<Config>;
}

class HeaderWidget extends MithrilViewComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, this>): m.Children | void | null {
    return <div className={styles.pipelineInstanceHeader}>
      <span>Instance</span>
      {vnode.attrs.config().stages().map((stage) => {
        return <span>
          {stage.name()}
        </span>;
      })}
    </div>;
  }
}