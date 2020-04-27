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
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TextField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_view";

export class NantTaskModal extends AbstractTaskModal {
  private readonly showOnCancel: boolean;
  private readonly task: Task;
  private readonly pluginInfos: PluginInfos;
  private readonly readonly: boolean;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => Promise<any>, pluginInfos: PluginInfos, readonly: boolean) {
    super(onAdd, readonly);
    this.showOnCancel = showOnCancel;
    this.readonly     = readonly;
    this.task         = task ? task : new NantTask(undefined, undefined, undefined, undefined, [], undefined);
    this.pluginInfos  = pluginInfos;
  }

  body(): m.Children {
    const attributes = this.task.attributes() as NantTaskAttributes;

    return <div data-test-id="nant-task-modal">
      {this.renderFlashMessage()}
      <h3>Basic Settings</h3>
      <TextField helpText="Relative path to a NAnt build file. If not specified, the path defaults to 'default.build'."
                 label="Build File"
                 readonly={this.readonly}
                 placeholder="default.build"
                 errorText={attributes.errors().errorsForDisplay("buildFile")}
                 property={attributes.buildFile}/>
      <TextField helpText="NAnt target(s) to run. If not specified, defaults to the default target of the build file."
                 label="Target"
                 readonly={this.readonly}
                 placeholder="default"
                 errorText={attributes.errors().errorsForDisplay("target")}
                 property={attributes.target}/>
      <TextField helpText="The directory from where nant is invoked."
                 label="Working Directory"
                 readonly={this.readonly}
                 errorText={attributes.errors().errorsForDisplay("workingDirectory")}
                 property={attributes.workingDirectory}/>
      <TextField helpText="Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path."
                 label="NAnt Path"
                 readonly={this.readonly}
                 errorText={attributes.errors().errorsForDisplay("nantPath")}
                 property={attributes.nantPath}/>

      <OnCancelView showOnCancel={this.showOnCancel}
                    onCancel={attributes.onCancel}
                    readonly={this.readonly}
                    pluginInfos={this.pluginInfos}
                    runIf={attributes.runIf}/>
    </div>;
  }

  title(): string {
    return "NAnt Task";
  }

  getTask(): Task {
    return this.task;
  }
}
