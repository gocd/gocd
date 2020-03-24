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
import {AntTask, AntTaskAttributes, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TextField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_view";

export class AntTaskModal extends AbstractTaskModal {
  private readonly task: AntTask;
  private readonly showOnCancel: boolean;
  private readonly pluginInfos: PluginInfos;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => void, pluginInfos: PluginInfos) {
    super(onAdd);
    this.showOnCancel = showOnCancel;
    this.pluginInfos  = pluginInfos;
    this.task         = task ? task : new AntTask(undefined, undefined, undefined, [], undefined);
  }

  body(): m.Children {
    const attributes = this.task.attributes() as AntTaskAttributes;

    return <div data-test-id="ant-task-modal">
      {this.renderFlashMessage()}
      <h3>Basic Settings</h3>
      <TextField helpText="Path to Ant build file. If not specified, the path defaults to 'build.xml'."
                 label="Build File"
                 placeholder="build.xml"
                 errorText={attributes.errors().errorsForDisplay("buildFile")}
                 property={attributes.buildFile}/>
      <TextField helpText="Ant target(s) to run. If not specified, the target defaults to 'default'."
                 label="Target"
                 placeholder="default"
                 errorText={attributes.errors().errorsForDisplay("target")}
                 property={attributes.target}/>
      <TextField helpText="The directory from where ant is invoked."
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
    return "Ant Task";
  }

  getTask(): Task {
    return this.task;
  }
}
