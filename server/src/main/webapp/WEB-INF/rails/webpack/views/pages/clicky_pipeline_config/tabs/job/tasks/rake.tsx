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
import {RakeTask, RakeTaskAttributes, Task} from "models/pipeline_configs/task";
import {TextField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelTaskWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_widget";
import {RunIfConditionWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/run_if_widget";

export class RakeTaskModal extends AbstractTaskModal {
  private readonly task: RakeTask;
  private readonly showOnCancel: boolean;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => void) {
    super(onAdd);
    this.showOnCancel = showOnCancel;
    this.task         = task ? task : new RakeTask(undefined, undefined, undefined, [], undefined);
  }

  body(): m.Children {
    const attributes = this.task.attributes() as RakeTaskAttributes;

    let onCancel: m.Child | undefined;
    if (this.showOnCancel) {
      onCancel = <div data-test-id="rake-on-cancel-view">
        <RunIfConditionWidget runIf={attributes.runIf}/>
        <h3>Advanced Option</h3>
        <OnCancelTaskWidget onCancel={attributes.onCancel}/>
      </div>;
    }

    return <div data-test-id="rake-task-modal">
      <h3>Basic Settings</h3>
      <TextField helpText="Path to Rake file. If not specified, the path defaults to 'rakefile'."
                 label="Build File"
                 placeholder="rakefile"
                 property={attributes.buildFile}/>
      <TextField helpText="Rake target(s) to run."
                 label="Target"
                 property={attributes.target}/>
      <TextField helpText="The directory from where rake is invoked."
                 label="Working Directory"
                 property={attributes.workingDirectory}/>
      {onCancel}
    </div>;
  }

  title(): string {
    return "Rake Task";
  }

  getTask(): Task {
    return this.task;
  }
}
