/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import Stream from "mithril/stream";
import {FetchArtifactTask, FetchTaskAttributes, Task} from "models/pipeline_configs/task";
import {Configurations} from "models/shared/configuration";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {RadioField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_view";
import {BuiltInFetchArtifactView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/built_in";
import {ExternalFetchArtifactView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/external";
import styles from "./fetch.scss";

export class FetchArtifactTaskModal extends AbstractTaskModal {
  private readonly task: FetchArtifactTask;
  private readonly showOnCancel: boolean;
  private readonly pluginInfos: PluginInfos;
  private autoSuggestions: Stream<any>;
  private readonly readonly: boolean;

  constructor(task: Task | undefined,
              showOnCancel: boolean,
              onAdd: (t: Task) => Promise<any>,
              pluginInfos: PluginInfos,
              readonly: boolean,
              autoSuggestions: Stream<any>) {
    super(onAdd, readonly);
    this.showOnCancel    = showOnCancel;
    this.pluginInfos     = pluginInfos;
    this.autoSuggestions = autoSuggestions;
    this.readonly        = readonly;

    this.task = task ? task : new FetchArtifactTask(
      "gocd", undefined, "", "", false,
      undefined, undefined,
      undefined, new Configurations([]), []);
  }

  body(): m.Children {
    const attributes = this.task.attributes() as FetchTaskAttributes;

    let content: m.Children;
    if (attributes.isBuiltInArtifact()) {
      content = <BuiltInFetchArtifactView attributes={attributes}
                                          readonly={this.readonly}
                                          autoSuggestions={this.autoSuggestions}/>;
    } else {
      content = <ExternalFetchArtifactView attributes={attributes}
                                           readonly={this.readonly}
                                           artifactPluginInfos={this.pluginInfos.filterForExtension(ExtensionTypeString.ARTIFACT)}
                                           autoSuggestions={this.autoSuggestions}/>;
    }

    return <div data-test-id="fetch-artifact-task-modal">
      {this.renderFlashMessage()}
      <div class={styles.radioWrapper}>
        <RadioField label="Type of Fetch Artifact"
                    readonly={this.readonly}
                    inline={true}
                    property={attributes.artifactOrigin}
                    required={true}
                    possibleValues={[
                      {label: "GoCD", value: "gocd"},
                      {label: "External", value: "external"}
                    ]}>
        </RadioField>
      </div>
      {content}
      <OnCancelView showOnCancel={this.showOnCancel}
                    readonly={this.readonly}
                    onCancel={attributes.onCancel}
                    pluginInfos={this.pluginInfos}
                    runIf={attributes.runIf}/>
    </div>;
  }

  title(): string {
    return "Fetch Artifact Task";
  }

  getTask(): Task {
    return this.task;
  }
}
