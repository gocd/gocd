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
import {CheckboxField, HelpText, TextField} from "views/components/forms/input_fields";
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
                 helpText={<span data-test-id="help-stage-name">No spaces. Only letters, numbers, hyphens, underscores and period. Max 255 chars</span>}
                 placeholder="e.g. build-stage"
                 property={stage.name}/>
      <SwitchBtn label={<div>Automatically run this stage on upstream changes: </div>}
                 field={stage.approval().state} small={true}/>
      <HelpText helpTextId="help-stage-approval"
                helpText="If unchecked, this stage will only schedule in response to a Manual/API/Timer trigger."/>
      <div class={styles.checkboxGroup}>
        <CheckboxField label="Fetch Materials"
                       helpText={<span data-test-id="help-fetch-materials">Allow the agent to fetch/checkout/clone the material during job run. This option can be turned off in cases where job execution is not dependent on the material at all.</span>}
                       property={stage.fetchMaterials}/>
        <CheckboxField label="Never Cleanup Artifacts"
                       helpText={<span data-test-id="help-never-cleanup-artifacts">Exclude artifacts from the current stage during artifacts cleanup. This option can be set for stages that are important so that artifacts for the stage are preserved.</span>}
                       property={stage.neverCleanupArtifacts}/>
        <CheckboxField label="Clean Working Directory"
                       helpText={<span data-test-id="help-clean-working-directory">Allow the agent to delete files/directories and reset the file changes under version control which are created during the previous build.</span>}
                       property={stage.cleanWorkingDirectory}/>
      </div>
    </div>;
  }
}
