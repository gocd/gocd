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
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {TabWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/tab_widget";
import {StagesWidget} from "views/pages/clicky_pipeline_config/widgets/stages_widget";
import {TemplateEditor} from "views/pages/pipelines/template_editor";

export class StagesTab extends TabWidget {
  name() {
    return "Stages";
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig) {
    return [
      <TemplateEditor pipelineConfig={entity} isUsingTemplate={entity.isUsingTemplate()}
                      paramList={entity.parameters}/>,
      <StagesWidget stages={entity.stages} isUsingTemplate={entity.isUsingTemplate()}
                    isEditable={!entity.origin().isDefinedInConfigRepo()}/>
    ];
  }
}
