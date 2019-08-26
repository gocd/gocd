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

import m from "mithril";
import Stream from "mithril/stream";
import {StageConfig} from "models/new_pipeline_configs/stage_configuration";
import {Modal} from "views/components/modal";
import {Tabs} from "views/components/tab";
import {StagePermissionsTab} from "views/pages/pipeline_configs/stages/settings/tabs/permissions";
import {StageSettingsTab} from "views/pages/pipeline_configs/stages/settings/tabs/settings";
import styles from "./stage_settings.scss";

export class StageSettingsModal extends Modal {
  private readonly stageConfig: Stream<StageConfig>;

  constructor(stageConfig: Stream<StageConfig>) {
    super();
    this.stageConfig = stageConfig;
  }

  body(): m.Children {
    const stageSettingsWidget        = <div class={styles.stageSettingsTabContentContainer}><StageSettingsTab stageConfig={this.stageConfig}/></div>;
    const environmentVariablesWidget = <div className={styles.stageSettingsTabContentContainer}><div>This will render environment variables.</div></div>;
    const permissionsWidget          = <div className={styles.stageSettingsTabContentContainer}><StagePermissionsTab stageConfig={this.stageConfig} /></div>;

    return <div data-test-id="stage-settings-modal">
      <Tabs tabs={["Stage Settings", "Environment Variables", "Permissions"]}
            contents={[stageSettingsWidget, environmentVariablesWidget, permissionsWidget]}/>
    </div>;
  }

  title(): string {
    return "Stage Settings";
  }
}
