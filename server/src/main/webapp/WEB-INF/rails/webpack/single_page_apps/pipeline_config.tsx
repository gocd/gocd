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

import {RoutedSinglePageApp} from "helpers/spa_base";
import m from 'mithril';
import {PipelineConfigPage} from "views/pages/clicky_pipeline_config/pipeline_config";
import {EnvironmentVariablesTab} from "views/pages/clicky_pipeline_config/tabs/environment_variables_tab";
import {GeneralOptionsTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/general_options_tab";
import {MaterialsTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/materials_tab";
import {ParametersTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/parameters_tab";
import {ProjectManagementTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/project_management_tab";
import {StagesTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/stages_tab";
import {StageSettingsTab} from "views/pages/clicky_pipeline_config/tabs/stage/settings";

class RedirectToGeneralTab extends PipelineConfigPage<any> {

  oninit(vnode: m.Vnode<any, any>) {
    const pipelineName = this.getMeta().pipelineName;
    m.route.set("/" + pipelineName + "/general");
  }
}

export class PipelineConfigSPA extends RoutedSinglePageApp {
  constructor() {
    super({
      "/": new RedirectToGeneralTab(),
      "/:pipeline_name/:tab_name": new PipelineConfigPage(
        new GeneralOptionsTab(),
        new EnvironmentVariablesTab(),
        new ProjectManagementTab(),
        new MaterialsTab(),
        new StagesTab(),
        new ParametersTab()
      ),
      "/:pipeline_name/:stage_name/:tab_name": new PipelineConfigPage(
        new StageSettingsTab()
      ),
      "/:pipeline_name/:stage_name/:job_name/:tab_name": new PipelineConfigPage()
    });
  }
}

//tslint:disable-next-line
new PipelineConfigSPA();
