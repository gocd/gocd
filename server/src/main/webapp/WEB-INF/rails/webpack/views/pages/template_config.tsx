/*
 * Copyright Thoughtworks, Inc.
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
import {ApiRequestBuilder, ApiResult, ApiVersion, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import m from "mithril";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {MessageType} from "views/components/flash_message";
import {ArtifactsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/artifacts_tab_content";
import {CustomTabTabContent} from "views/pages/clicky_pipeline_config/tabs/job/custom_tab_tab_content";
import {
  JobEnvironmentVariablesTabContent
} from "views/pages/clicky_pipeline_config/tabs/job/job_environment_variable_tab_content";
import {JobSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/job_settings_tab_content";
import {TasksTabContent} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";
import {StagesTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/stages_tab_content";
import {PipelineConfigSPARouteHelper} from "views/pages/clicky_pipeline_config/tabs/route_helper";
import {JobsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/jobs_tab_content";
import {PermissionsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/permissions_tab_content";
import {
  StageEnvironmentVariablesTabContent
} from "views/pages/clicky_pipeline_config/tabs/stage/stage_environment_variable_tab_content";
import {StageSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/stage_settings_tab_content";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {GeneralOptionsTabContent} from "views/pages/clicky_pipeline_config/tabs/template/general_tab_content";
import {SupportedTypes, TabHandler, TabLevel} from "views/pages/clicky_pipeline_config/tab_handler";

export class TemplateConfigPage<T> extends TabHandler<T> {
  oninit(vnode: m.Vnode<null, T>) {
    super.oninit(vnode);
  }

  getEntity(): TemplateConfig {
    return this.entity! as TemplateConfig;
  }

  getOriginalEntity(): TemplateConfig {
    return TemplateConfig.fromJSON(this.originalJSON)! as TemplateConfig;
  }

  setEntity(entity: TemplateConfig) {
    this.entity = entity;
  }

  reset() {
    this.setEntity(TemplateConfig.fromJSON(this.originalJSON));
    this.tab().onPipelineConfigReset();
  }

  //this method is only required for pipeline config.
  getAssociatedTemplateWithPipeline(): TemplateConfig | undefined {
    return;
  }

  shouldShowSpinner(): boolean {
    return !this.getEntity();
  }

  pageName(): string {
    return "Templates";
  }

  fetchData(vnode: m.Vnode<null, T>): Promise<any> {
    const templateName = PipelineConfigSPARouteHelper.routeInfo().params.pipeline_name;
    return ApiRequestBuilder.GET(SparkRoutes.templatesPath(templateName), ApiVersion.v7)
                            .then((result) => {
                              return result.do(this.onSuccess.bind(this, result), this.onFailure.bind(this));
                            });
  }

  getAllTabsInformation(): Map<string, Map<string, new() => TabContent<SupportedTypes>>> {
    const pipelineTabList: Map<string, new() => TabContent<SupportedTypes>> = new Map();
    const stageTabList: Map<string, new() => TabContent<SupportedTypes>>    = new Map();
    const jobTabList: Map<string, new() => TabContent<SupportedTypes>>      = new Map();

    [
      GeneralOptionsTabContent,
      StagesTabContent
    ].forEach(t => pipelineTabList.set(_.snakeCase(t.tabName()), t));

    [
      StageSettingsTabContent,
      JobsTabContent,
      StageEnvironmentVariablesTabContent,
      PermissionsTabContent
    ].forEach(t => stageTabList.set(_.snakeCase(t.tabName()), t));

    [
      JobSettingsTabContent,
      TasksTabContent,
      ArtifactsTabContent,
      JobEnvironmentVariablesTabContent,
      CustomTabTabContent
    ].forEach(t => jobTabList.set(_.snakeCase(t.tabName()), t));

    const tabList: Map<string, Map<string, new() => TabContent<SupportedTypes>>> = new Map();
    tabList.set("pipeline" as TabLevel, pipelineTabList);
    tabList.set("stage" as TabLevel, stageTabList);
    tabList.set("job" as TabLevel, jobTabList);

    return tabList;
  }

  onSuccess(result: ApiResult<any>, successResponse: SuccessResponse<string>): Promise<any> {
    this.etag(result.getEtag()!);

    this.originalJSON = JSON.parse(successResponse.body);
    this.setEntity(TemplateConfig.fromJSON(this.originalJSON));
    super.onSuccessfulPipelineSaveCallback();
    return Promise.resolve(successResponse);
  }

  shouldShowSaveAndResetButtons(entity: TemplateConfig): boolean {
    return true;
  }

  save(): Promise<any> {
    this.flashMessage.clear();
    this.clearConfigErrors();
    const isValid = this.getEntity().isValid();

    if (!isValid) {
      const msg = "Validation Failed! Please fix the below errors before submitting.";
      this.flashMessage.setMessage(MessageType.alert, msg);
      return Promise.reject();
    }

    const possibleRenames = PipelineConfigSPARouteHelper.getPossibleRenames(this.getEntity());
    return this.getEntity().update(this.etag()).then((result) => {
      return result.do((successResponse) => {
        this.flashMessage.setMessage(MessageType.success, "Saved Successfully!");
        PipelineConfigSPARouteHelper.updateRouteIfEntityRenamed(possibleRenames);
        this.onSuccess(result, successResponse);
      }, this.onFailure.bind(this));
    });
  }

  protected hasEntityConfigChanged() {
    const newPayload = this.getEntity().toApiPayload();
    return !_.isEqual(newPayload, TemplateConfig.fromJSON(this.originalJSON).toApiPayload());
  }

}
