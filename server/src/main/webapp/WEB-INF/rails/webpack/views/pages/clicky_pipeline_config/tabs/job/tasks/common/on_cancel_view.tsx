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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {RunIfCondition, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {OnCancelTaskWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_widget";
import {RunIfConditionWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/run_if_widget";

interface Attrs {
  showOnCancel: boolean;
  runIf: Stream<RunIfCondition[]>;
  onCancel: Stream<Task | undefined>
  pluginInfos: PluginInfos;
}

export class OnCancelView extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    let onCancelView: m.Child | undefined;

    if (vnode.attrs.showOnCancel) {
      onCancelView = <div data-test-id="nant-on-cancel-view">
        <RunIfConditionWidget runIf={vnode.attrs.runIf}/>
        <h3>Advanced Option</h3>
        <OnCancelTaskWidget onCancel={vnode.attrs.onCancel}
                            pluginInfos={vnode.attrs.pluginInfos}/>
      </div>;
    }

    return onCancelView;

  }

}
