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
import {CheckboxField, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {TasksWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";

export interface State {
  onCancelCheckbox: Stream<boolean>;
  selectedTaskTypeToAdd: Stream<string>;
  allTaskTypes: string[];
}

export interface Attrs {
  onCancel: Stream<Task | undefined>;
}

export class OnCancelTaskWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.onCancelCheckbox = Stream();
    vnode.state.onCancelCheckbox(!!vnode.attrs.onCancel());

    vnode.state.allTaskTypes          = TasksWidget.getTaskTypes();
    vnode.state.selectedTaskTypeToAdd = Stream(vnode.state.allTaskTypes[0]);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    let onCancelView: m.Child | undefined;

    if (vnode.state.onCancelCheckbox()) {
      onCancelView = <div data-test-id="on-cancel-body">
        <SelectField property={vnode.state.selectedTaskTypeToAdd}>
          <SelectFieldOptions selected={vnode.state.selectedTaskTypeToAdd()}
                              items={vnode.state.allTaskTypes}/>
        </SelectField>
        {TasksWidget.getTaskModal(vnode.state.selectedTaskTypeToAdd(),
                                  vnode.attrs.onCancel(),
                                  this.noOperation,
                                  false)!.body()}
      </div>;
    }

    return <div data-test-id="on-cancel-view">
      <CheckboxField label="On Cancel Task"
                     property={vnode.state.onCancelCheckbox}/>
      {onCancelView}
    </div>;
  }

  noOperation() {
    //do nothing
  }
}
