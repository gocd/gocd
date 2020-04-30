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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {CheckboxField, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {TasksWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";

export interface State {
  onCancelCheckbox: Stream<boolean>;
  selectedTaskTypeToAdd: Stream<string>;
  allTaskTypes: string[];
}

export interface Attrs {
  onCancel: Stream<Task | undefined>;
  pluginInfos: PluginInfos;
  readonly: boolean;
}

export class OnCancelTaskWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.onCancelCheckbox = Stream<boolean>(!!vnode.attrs.onCancel());
    vnode.state.allTaskTypes     = Array.from(TasksWidget.getTaskTypes().values())
                                        .filter((t) => t !== "Fetch Artifact");

    if (vnode.attrs.onCancel()) {
      vnode.state.selectedTaskTypeToAdd = Stream(TasksWidget.getTaskTypes().get(vnode.attrs.onCancel()!.type)!);
    } else {
      vnode.state.selectedTaskTypeToAdd = Stream(vnode.state.allTaskTypes[3]);
    }
  }

  view(vnode: m.Vnode<Attrs, State>) {
    let onCancelView: m.Child | undefined;

    if (vnode.state.onCancelCheckbox()) {
      onCancelView = <div data-test-id="on-cancel-body">
        <SelectField property={vnode.state.selectedTaskTypeToAdd}
                     readonly={vnode.attrs.readonly}
                     onchange={this.updateOnCancelTask.bind(this, vnode)}>
          <SelectFieldOptions selected={vnode.state.selectedTaskTypeToAdd()}
                              items={vnode.state.allTaskTypes}/>
        </SelectField>
        {this.getTaskModal(vnode).body()}
      </div>;
    }

    return <div data-test-id="on-cancel-view">
      <CheckboxField label="On Cancel Task"
                     helpText="Task which needs to be run, if the parent task is cancelled. Note: The default action of killing the parent task will not be performed."
                     readonly={vnode.attrs.readonly}
                     onchange={this.updateOnCancelTask.bind(this, vnode)}
                     property={vnode.state.onCancelCheckbox}/>
      {onCancelView}
    </div>;
  }

  updateOnCancelTask(vnode: m.Vnode<Attrs, State>) {
    if (vnode.state.onCancelCheckbox()) {
      //clear the existing task type
      vnode.attrs.onCancel(undefined);
      const task = this.getTaskModal(vnode).getTask();
      vnode.attrs.onCancel(task);
    } else {
      vnode.attrs.onCancel(undefined);
    }
  }

  noOperation() {
    return Promise.resolve();
  }

  private getTaskModal(vnode: m.Vnode<Attrs, State>) {
    return TasksWidget.getTaskModal(vnode.state.selectedTaskTypeToAdd(),
                                    vnode.attrs.onCancel(),
                                    this.noOperation.bind(this),
                                    false,
                                    vnode.attrs.pluginInfos,
                                    this.noOperation.bind(this),
                                    this.noOperation.bind(this),
                                    vnode.attrs.readonly,
                                    Stream({}));
  }
}
