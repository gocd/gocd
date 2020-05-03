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
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import styles from "./environment_variables.scss";

export abstract class EnvironmentVariablesTabContent extends TabContent<PipelineConfig | Stage | Job> {
  private isDefinedInConfigRepository: Stream<boolean> = Stream<boolean>(false);

  static tabName(): string {
    return "Environment Variables";
  }

  protected renderer(entity: PipelineConfig | Stage | Job, templateConfig: TemplateConfig): m.Children {
    const variables = entity.environmentVariables();
    const readOnly  = this.isDefinedInConfigRepository();

    let readOnlyClass = '';
    if (readOnly) {
      variables.forEach(env => {env.isEditable(false);});
      readOnlyClass = styles.configRepoEnvironmentVariables;
    }

    return <div data-test-id={`${readOnly ? "readonly-" : ""}environment-variables`} class={readOnlyClass}>
      <EnvironmentVariablesWidget environmentVariables={variables}/>
    </div>;
  }

  protected selectedEntity(entity: PipelineConfig | TemplateConfig,
                           routeParams: PipelineConfigRouteParams): PipelineConfig | Stage | Job {
    this.isDefinedInConfigRepository(this.isPipelineConfigView() && (entity as PipelineConfig).isDefinedInConfigRepo());
    return this.getSelectedEntity(entity, routeParams);
  }

  protected abstract getSelectedEntity(pipelineConfig: PipelineConfig | TemplateConfig,
                                       routeParams: PipelineConfigRouteParams): PipelineConfig | Stage | Job;
}
