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
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {Size, TextAreaField, TextField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_view";

export class ExecTaskModal extends AbstractTaskModal {
  private readonly task: ExecTask;
  private readonly args: Stream<string>;
  private readonly showOnCancel: boolean;
  private readonly pluginInfos: PluginInfos;
  private readonly readonly: boolean;


  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => Promise<any>, pluginInfos: PluginInfos, readonly: boolean) {
    super(onAdd);
    this.task = task ? task : new ExecTask("", [], undefined, [], undefined);
    this.args = Stream((this.task.attributes() as ExecTaskAttributes).arguments().join("\n"));

    this.pluginInfos  = pluginInfos;
    this.showOnCancel = showOnCancel;
    this.readonly     = readonly;
  }

  body(): m.Children {
    const attributes = this.task.attributes() as ExecTaskAttributes;

    return <div data-test-id="exec-task-modal">
      {this.renderFlashMessage()}
      <h3>Basic Settings</h3>
      <TextField helpText="The command or script to be executed, relative to the working directory"
                 errorText={attributes.errors().errorsForDisplay("command")}
                 required={true}
                 label="Command"
                 placeholder="ls"
                 property={attributes.command}/>
      <TextAreaField helpText="Enter each argument on a new line"
                     errorText={attributes.errors().errorsForDisplay("args")}
                     rows={5}
                     size={Size.MATCH_PARENT}
                     resizable={true}
                     label="Arguments"
                     property={this.args}/>
      <TextField helpText="The directory in which the script or command is to be executed. This is always relative to the directory where the agent checks out materials."
                 label="Working Directory"
                 errorText={attributes.errors().errorsForDisplay("workingDirectory")}
                 property={attributes.workingDirectory}/>
      <OnCancelView showOnCancel={this.showOnCancel}
                    onCancel={attributes.onCancel}
                    pluginInfos={this.pluginInfos}
                    runIf={attributes.runIf}/>
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
