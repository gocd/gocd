/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {StageConfig} from "models/new_pipeline_configs/stage_configuration";
import {CheckboxField, TextField} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch";
import styles from "../stage_settings.scss";

interface Attrs {
  stageConfig: Stream<StageConfig>;
}

export class StageSettingsTab extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stageConfig();
    return <div data-test-id="stage-settings-tab">
      <TextField required={true}
                 label="Stage Name"
                 placeholder="e.g. build-stage"
                 property={stage.name}/>
      <SwitchBtn label={<div>Automatically run this stage on upstream changes: </div>}
                 field={stage.approval().state} small={true}/>
      <div class={styles.checkboxGroup}>
        <CheckboxField label="Fetch Materials"
                       property={stage.fetchMaterials}/>
        <CheckboxField label="Never Cleanup Artifacts"
                       property={stage.neverCleanupArtifacts}/>
        <CheckboxField label="Clean Working Directory"
                       property={stage.cleanWorkingDirectory}/>
      </div>
    </div>;
  }
}
