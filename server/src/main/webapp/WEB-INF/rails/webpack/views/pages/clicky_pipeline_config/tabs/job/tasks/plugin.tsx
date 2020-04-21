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

import {bind} from "classnames/bind";
import m from "mithril";
import Stream from "mithril/stream";
import {PluginConfiguration} from "models/admin_templates/templates";

import {PluggableTask, PluggableTaskAttributes, Task} from "models/pipeline_configs/task";
import {Configurations} from "models/shared/configuration";
import {TaskExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_view";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const AngularPluginNew     = require("views/shared/angular_plugin_new").AngularPluginNew;
const foundationClassNames = bind(foundationStyles);

export class PluggableTaskModal extends AbstractTaskModal {
  private readonly task: PluggableTask;
  private readonly showOnCancel: boolean;
  private readonly pluginInfos: PluginInfos;
  private readonly selectedPluginId: Stream<string>;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => Promise<any>, pluginInfos: PluginInfos) {
    super(onAdd);
    this.pluginInfos  = pluginInfos;
    this.showOnCancel = showOnCancel;

    if (task) {
      this.task = task;
    } else {
      const configurations = this.allTaskPluginConfiguration();
      const config         = (configurations.length > 0) ? configurations[0] : {} as PluginConfiguration;
      this.task            = new PluggableTask(config, new Configurations([]), []);
    }

    this.selectedPluginId = Stream((this.task.attributes() as PluggableTaskAttributes).pluginConfiguration().id);
  }

  getTask(): Task {
    return this.task;
  }

  body(): m.Children {
    if (this.allTaskPluginConfiguration().length === 0) {
      return <FlashMessage type={MessageType.info}
                           message={"Can not define plugin task as no task plugins are installed!"}/>;

    }

    const attributes = this.task.attributes() as PluggableTaskAttributes;
    return <div data-test-id="task-plugin-modal">
      {this.renderFlashMessage()}
      <SelectField property={this.selectedPluginId}
                   label="Select Task Plugin:"
                   onchange={this.setPluginConfigOnTask.bind(this)}
                   required={true}>
        <SelectFieldOptions selected={this.selectedPluginId()}
                            items={this.allTaskPluginConfiguration().map((p) => p.id)}/>
      </SelectField>

      <h3>Basic Settings</h3>
      <div className={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        <AngularPluginNew pluginInfoSettings={Stream(this.findPluginSettings())}
                          key={this.selectedPluginId()}
                          configuration={attributes.configuration()}/>
      </div>

      <OnCancelView showOnCancel={this.showOnCancel}
                    onCancel={attributes.onCancel}
                    pluginInfos={this.pluginInfos}
                    runIf={attributes.runIf}/>
    </div>;
  }

  title(): string {
    return "Plugin Task";
  }

  private setPluginConfigOnTask(e: Event) {
    const pluginId = (e.target as HTMLSelectElement).value;
    (this.task.attributes() as PluggableTaskAttributes)
      .pluginConfiguration(this.allTaskPluginConfiguration().find((c) => c.id === pluginId)!);
  }

  private findPluginSettings() {
    return (this.pluginInfos.findByPluginId(this.selectedPluginId())!
      .extensionOfType(ExtensionTypeString.TASK) as TaskExtension).taskSettings;
  }

  private allTaskPluginConfiguration(): PluginConfiguration[] {
    return this.pluginInfos.filter((p) => p.extensionOfType(ExtensionTypeString.TASK))
               .map((p) => ({
                 id: p.id,
                 version: p.about.version
               }));
  }
}
