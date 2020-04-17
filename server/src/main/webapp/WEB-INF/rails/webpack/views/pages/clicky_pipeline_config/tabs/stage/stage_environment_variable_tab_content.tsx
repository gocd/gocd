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

import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";;
import {EnvironmentVariablesTabContent} from "views/pages/clicky_pipeline_config/tabs/common/environment_variables_tab_content";

export class StageEnvironmentVariablesTabContent extends EnvironmentVariablesTabContent {
  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Stage {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!;
  }
}
