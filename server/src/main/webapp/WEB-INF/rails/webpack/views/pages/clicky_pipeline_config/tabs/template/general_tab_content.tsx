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
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";

export class GeneralOptionsTabContent extends TabContent<TemplateConfig> {
  static tabName(): string {
    return "General";
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams) {
    return pipelineConfig;
  }

  //todo: Implement me Ganeshpl
  protected renderer(entity: TemplateConfig,
                     templateConfig: TemplateConfig,
                     flashMessage: FlashMessageModelWithTimeout,
                     pipelineConfigSave: () => any,
                     pipelineConfigReset: () => any): m.Children {
    return <div>
      this will render templates
    </div>;
  }
}
