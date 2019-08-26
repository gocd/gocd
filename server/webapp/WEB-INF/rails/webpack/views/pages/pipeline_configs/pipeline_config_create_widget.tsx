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
import {PipelineConfig} from "models/new_pipeline_configs/pipeline_config";
import {StageConfig} from "models/new_pipeline_configs/stage_configuration";
import {IdentifierInputField} from "views/components/forms/common_validating_inputs";
import {StageSettingsWidget} from "views/pages/pipeline_configs/stages/settings/stage_settings_widget";
import styles from "./index.scss";

interface Attrs {
  pipelineConfig: PipelineConfig;
}

export class PipelineConfigCreateWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    /*todo: @Ganeshpl will move this stage settings to appropriate place*/
    return <div class={styles.pipelineDetailsContainer} data-test-id="pipeline-details-container">
      <IdentifierInputField dataTestId="pipeline-name-input"
                            required={true}
                            helpText="No spaces. Only letters, numbers, hyphens, underscores and period. Max 255 chars"
                            label="Pipeline name"
                            placeholder="e.g. build-and-deploy"
                            property={vnode.attrs.pipelineConfig.name}/>
      <StageSettingsWidget stageConfig={Stream(new StageConfig("up42_stage"))}/>
    </div>;
  }
}
