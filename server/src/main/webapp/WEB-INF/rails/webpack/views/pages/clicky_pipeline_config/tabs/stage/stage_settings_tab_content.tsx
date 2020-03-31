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
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {TextField} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {StageEditor} from "views/pages/clicky_pipeline_config/widgets/stage_editor_widget";
import styles from "./stage_settings.scss";

export class StageSettingsTabContent extends TabContent<Stage> {
  static tabName(): string {
    return "Stage Settings";
  }

  protected renderer(stage: Stage, templateConfig: TemplateConfig) {
    return <StageSettingsWidget stage={stage}/>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Stage {
    return Array.from(pipelineConfig.stages()).find(s => s.getOriginalName() === routeParams.stage_name!)!;
  }
}

interface Attrs {
  stage: Stage;
  //to reuse this view while creating a new stage. Based on this field, the view will not render less-important fields in create new stage view.
  isForAddStagePopup?: boolean;
}

export class StageSettingsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stage;

    let additionalStageSettings: m.Children;
    if (!vnode.attrs.isForAddStagePopup) {
      additionalStageSettings = <div data-test-id="additional-stage-settings">
        <div className={styles.switchWrapper}>
          <SwitchBtn label="Fetch materials"
                     helpText="Perform material updates or checkouts."
                     dataTestId="fetch-materials-checkbox"
                     small={true}
                     field={stage.fetchMaterials}/>
        </div>
        <div className={styles.switchWrapper}>
          <SwitchBtn label="Never cleanup artifacts"
                     helpText="Never cleanup artifacts for this stage, if purging artifacts is configured at the Server Level."
                     dataTestId="never-cleanup-artifacts-checkbox"
                     small={true}
                     field={stage.neverCleanupArtifacts}/>
        </div>
        <div className={styles.switchWrapper}>
          <SwitchBtn label="Never cleanup artifacts"
                     helpText="Remove all files/directories in the working directory on the agent."
                     dataTestId="clean-working-directory-checkbox"
                     small={true}
                     field={stage.cleanWorkingDirectory}/>
        </div>
      </div>;
    }

    return <div data-test-id="stage-settings">
      <TextField label="Stage name"
                 required={true}
                 dataTestId="stage-name-input"
                 errorText={stage.errors().errorsForDisplay("name")}
                 property={stage.name}/>
      <div class={styles.switchWrapper}>
        <SwitchBtn label="Trigger completion of previous stage:"
                   helpText={StageEditor.APPROVAL_TYPE_HELP}
                   field={stage.approval().typeAsStream()}
                   small={true}
                   dataTestId="approval-checkbox"
                   onclick={StageSettingsWidget.approvalChange.bind(this, stage)}/>
      </div>
      <div class={styles.switchWrapper}>
        <SwitchBtn label="Allow only on success"
                   helpText={StageEditor.ALLOW_ONLY_ON_SUCCESS_HELP}
                   small={true}
                   dataTestId="allow-only-on-success-checkbox"
                   field={stage.approval().allowOnlyOnSuccess}/>
      </div>
      {additionalStageSettings}
    </div>;
  }

  private static approvalChange(stage: Stage, e: MouseEvent) {
    const checkbox = e.currentTarget as HTMLInputElement;
    stage.approval().typeAsStream()(checkbox.checked);
  }
}
