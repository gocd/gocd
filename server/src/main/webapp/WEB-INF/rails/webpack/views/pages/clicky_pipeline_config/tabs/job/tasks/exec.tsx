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
 * WITHOUT WARREXECIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import m from "mithril";
import Stream from "mithril/stream";
import {ExecTask, ExecTaskAttributes, Task} from "models/pipeline_configs/task";
import {Size, TextAreaField, TextField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelTaskWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_widget";
import {RunIfConditionWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/run_if_widget";

export class ExecTaskModal extends AbstractTaskModal {
  private readonly task: ExecTask;
  private readonly args: Stream<string>;
  private readonly showOnCancel: boolean;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => void) {
    super(onAdd);
    this.task = task ? task : new ExecTask("", [], undefined, [], undefined);
    this.args = Stream((this.task.attributes() as ExecTaskAttributes).arguments().join("\n"));

    this.showOnCancel = showOnCancel;
  }

  body(): m.Children {
    const attributes = this.task.attributes() as ExecTaskAttributes;

    let onCancel: m.Child | undefined;
    if (this.showOnCancel) {
      onCancel = <div data-test-id="exec-on-cancel-view">
        <RunIfConditionWidget runIf={attributes.runIf}/>
        <h3>Advanced Option</h3>
        <OnCancelTaskWidget onCancel={attributes.onCancel}/>
      </div>;
    }

    return <div data-test-id="exec-task-modal">
      <h3>Basic Settings</h3>
      <TextField helpText="The command or script to be executed, relative to the working directory"
                 required={true}
                 label="Command"
                 property={attributes.command}/>
      <TextAreaField helpText="Enter each argument on a new line"
                     rows={5}
                     size={Size.MATCH_PARENT}
                     resizable={true}
                     label="Arguments"
                     property={this.args}/>
      <TextField
        helpText="The directory in which the script or command is to be executed. This is always relative to the directory where the agent checks out materials."
        label="Working Directory"
        property={attributes.workingDirectory}/>
      {onCancel}
    </div>;
  }

  title(): string {
    return "Exec Task";
  }

  getTask(): Task {
    const args = this.args().split("\n").map((a) => a.trim());
    (this.task.attributes() as ExecTaskAttributes).arguments(args);

    return this.task;
  }
}
