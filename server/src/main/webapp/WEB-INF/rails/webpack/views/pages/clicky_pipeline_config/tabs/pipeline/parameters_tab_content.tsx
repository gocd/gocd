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
import Stream from "mithril/stream";
import {PipelineParameter} from "models/pipeline_configs/parameter";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {PipelineParametersEditor} from "views/pages/pipelines/parameters_editor";
import styles from "./parameters.scss";

export class ParametersTabContent extends TabContent<PipelineConfig> {
  readonly paramList = Stream([] as PipelineParameter[]);

  static tabName(): string {
    return "Parameters";
  }

  onSuccessfulPipelineConfigSave() {
    this.paramList().forEach(p => p.clearErrors());
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig) {
    return (<div class={styles.mainContainer}>
      <PipelineParametersEditor paramList={this.paramList}
                                readonly={this.isEntityDefinedInConfigRepository()}
                                parameters={entity.parameters}/>
    </div>);
  }
}
