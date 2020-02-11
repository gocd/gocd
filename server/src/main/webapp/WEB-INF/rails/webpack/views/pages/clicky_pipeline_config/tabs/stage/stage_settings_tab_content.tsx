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
import {Form} from "views/components/forms/form";
import {CheckboxField, TextField} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch";
import {Help} from "views/components/tooltip";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {StageEditor} from "views/pages/clicky_pipeline_config/widgets/stage_editor_widget";

export class StageSettingsTabContent extends TabContent<Stage> {
  name(): string {
    return "Stage Settings";
  }

  protected renderer(stage: Stage, templateConfig: TemplateConfig) {
    return <StageSettingsWidget stage={stage}/>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Stage {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!;
  }
}

interface Attrs {
  stage: Stage;
}

export class StageSettingsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stage;
    return <Form compactForm={true}>
      <TextField label={"Stage name"}
                 required={true}
                 dataTestId={"stage-name-input"}
                 errorText={stage.errors().errorsForDisplay("name")}
                 property={stage.name}/>

      <SwitchBtn label={["Trigger completion of previous stage:", <Help content={StageEditor.APPROVAL_TYPE_HELP}/>]}
                 field={stage.approval().typeAsStream()}
                 small={true}
                 onclick={StageSettingsWidget.approvalChange.bind(this, stage)}/>

      <CheckboxField label={["Allow only on success", <Help content={StageEditor.ALLOW_ONLY_ON_SUCCESS_HELP}/>]}
                     dataTestId={"allow-only-on-success-checkbox"}
                     property={stage.approval().allowOnlyOnSuccess}/>

      <CheckboxField label={["Fetch materials", <Help content={"Perform material updates or checkouts"}/>]}
                     dataTestId={"fetch-materials-checkbox"}
                     property={stage.fetchMaterials}/>
      <CheckboxField label={["Never cleanup artifacts", <Help
        content={"Never cleanup artifacts for this stage, if purging artifacts is configured at the Server Level"}/>]}
                     dataTestId={"never-cleanup-artifacts-checkbox"}
                     property={stage.neverCleanupArtifacts}/>

      <CheckboxField label={["Never cleanup artifacts",
        <Help content={"Remove all files/directories in the working directory on the agent"}/>]}
                     dataTestId={"clean-working-directory-checkbox"}
                     property={stage.cleanWorkingDirectory}/>
    </Form>;
  }

  private static approvalChange(stage: Stage, e: MouseEvent) {
    const checkbox = e.currentTarget as HTMLInputElement;
    stage.approval().typeAsStream()(checkbox.checked);
  }
}
