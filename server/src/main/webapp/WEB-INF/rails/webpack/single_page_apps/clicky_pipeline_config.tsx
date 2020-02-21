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
import m from "mithril";
import {PipelineConfigPage} from "views/pages/clicky_pipeline_config/pipeline_config";
import {EnvironmentVariablesTabContent} from "views/pages/clicky_pipeline_config/tabs/common/environment_variables_tab_content";
import {JobSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/job_settings_tab_content";
import {GeneralOptionsTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/general_options_tab";
import {MaterialsTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/materials_tab_content";
import {ParametersTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/parameters_tab_content";
import {ProjectManagementTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/project_management_tab_content";
import {StagesTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/stages_tab_content";
import {JobsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/jobs_tab_content";
import {PermissionsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/permissions_tab_content";
import {StageSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/stage_settings_tab_content";

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
              new GeneralOptionsTabContent(),
              new ProjectManagementTabContent(),
              new MaterialsTabContent(),
              new StagesTabContent(),
              new EnvironmentVariablesTabContent(),
              new ParametersTabContent()
            ),
            "/:pipeline_name/:stage_name/:tab_name": new PipelineConfigPage(
              new StageSettingsTabContent(),
              new JobsTabContent(),
              new EnvironmentVariablesTabContent(),
              new PermissionsTabContent()
            ),
            "/:pipeline_name/:stage_name/:job_name/:tab_name": new PipelineConfigPage(
              new JobSettingsTabContent(),
              new EnvironmentVariablesTabContent()
            )
          });
  }
}

//tslint:disable-next-line
new PipelineConfigSPA();
