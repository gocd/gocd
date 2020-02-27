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

import m from "mithril";
import {NantTask, NantTaskAttributes, Task} from "models/pipeline_configs/task";
import {TextField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelTaskWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_widget";
import {RunIfConditionWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/run_if_widget";

export class NantTaskModal extends AbstractTaskModal {
  private readonly showOnCancel: boolean;
  private readonly task: Task;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => void) {
    super(onAdd);
    this.showOnCancel = showOnCancel;
    this.task         = task ? task : new NantTask(undefined, undefined, undefined, undefined, [], undefined);
  }

  body(): m.Children {
    const attributes = this.task.attributes() as NantTaskAttributes;

    let onCancel: m.Child | undefined;

    if (this.showOnCancel) {
      onCancel = <div data-test-id="nant-on-cancel-view">
        <RunIfConditionWidget runIf={attributes.runIf}/>
        <h3>Advanced Option</h3>
        <OnCancelTaskWidget onCancel={attributes.onCancel}/>
      </div>;
    }

    return <div data-test-id="nant-task-modal">
      <h3>Basic Settings</h3>
      <TextField helpText="Relative path to a NAnt build file. If not specified, the path defaults to 'default.build'."
                 label="Build File"
                 placeholder="default.build"
                 property={attributes.buildFile}/>
      <TextField helpText="NAnt target(s) to run. If not specified, defaults to the default target of the build file."
                 label="Target"
                 placeholder="default"
                 property={attributes.target}/>
      <TextField helpText="The directory from where nant is invoked."
                 label="Working Directory"
                 property={attributes.workingDirectory}/>
      <TextField helpText="Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path."
                 label="NAnt Path"
                 property={attributes.nantPath}/>
      {onCancel}
    </div>;
  }

  title(): string {
    return "NAnt Task";
  }

  getTask(): Task {
    return this.task;
  }
}
