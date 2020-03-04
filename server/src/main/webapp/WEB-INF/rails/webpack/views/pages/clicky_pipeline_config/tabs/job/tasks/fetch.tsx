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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {FetchArtifactTask, FetchTaskAttributes, Task} from "models/pipeline_configs/task";
import {Configurations} from "models/shared/configuration";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {RadioField} from "views/components/forms/input_fields";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {OnCancelView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_view";
import {BuiltInArtifactView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/built_in";
import styles from "./fetch.scss";

interface Attrs {
  attributes: FetchTaskAttributes;
}

export class ExternalArtifactView extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div data-test-id="external-artifact-view">
      external artifact view
    </div>;
  }
}

export class FetchArtifactTaskModal extends AbstractTaskModal {
  private readonly task: FetchArtifactTask;
  private readonly showOnCancel: boolean;
  private readonly pluginInfos: PluginInfos;
  private autoSuggestions: Stream<any>;

  constructor(task: Task | undefined,
              showOnCancel: boolean,
              onAdd: (t: Task) => void,
              pluginInfos: PluginInfos,
              autoSuggestions: Stream<any>) {
    super(onAdd);
    this.showOnCancel    = showOnCancel;
    this.pluginInfos     = pluginInfos;
    this.autoSuggestions = autoSuggestions;

    this.task = task ? task : new FetchArtifactTask(
      "gocd", undefined, "", "", false,
      undefined, undefined,
      undefined, new Configurations([]), []);
  }

  body(): m.Children {
    const attributes = this.task.attributes() as FetchTaskAttributes;

    let content: m.Children;
    if (attributes.isBuiltInArtifact()) {
      content = <BuiltInArtifactView attributes={attributes} autoSuggestions={this.autoSuggestions}/>;
    } else {
      content = <ExternalArtifactView attributes={attributes}/>;
    }

    return <div data-test-id="fetch-artifact-task-modal">
      <div class={styles.radioWrapper}>
        <RadioField label="Type of Fetch Artifact"
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
