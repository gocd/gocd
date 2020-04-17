import {ApiRequestBuilder, ApiResult, ApiVersion, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import m from "mithril";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {SupportedTypes, TabHandler, TabLevel} from "views/pages/clicky_pipeline_config/tab_handler";
import {ArtifactsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/artifacts_tab_content";
import {CustomTabTabContent} from "views/pages/clicky_pipeline_config/tabs/job/custom_tab_tab_content";
import {JobEnvironmentVariablesTabContent} from "views/pages/clicky_pipeline_config/tabs/job/job_environment_variable_tab_content";
import {JobSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/job_settings_tab_content";
import {TasksTabContent} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";
import {StagesTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/stages_tab_content";
import {PipelineConfigSPARouteHelper} from "views/pages/clicky_pipeline_config/tabs/route_helper";
import {JobsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/jobs_tab_content";
import {PermissionsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/permissions_tab_content";
import {StageEnvironmentVariablesTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/stage_environment_variable_tab_content";
import {StageSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/stage_settings_tab_content";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {GeneralOptionsTabContent} from "views/pages/clicky_pipeline_config/tabs/template/general_tab_content";

export class TemplateConfigPage<T> extends TabHandler<T> {
  oninit(vnode: m.Vnode<null, T>) {
    super.oninit(vnode);
  }

  getEntity(): TemplateConfig {
    return this.entity! as TemplateConfig;
  }

  setEntity(entity: TemplateConfig) {
    this.entity = entity;
  }

  reset() {
    this.setEntity(TemplateConfig.fromJSON(this.originalJSON));
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

  protected hasEntityConfigChanged() {
    const newPayload = this.getEntity().toApiPayload();
    return !_.isEqual(newPayload, TemplateConfig.fromJSON(this.originalJSON).toApiPayload());
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

    return Promise.resolve(successResponse);
  }

  //todo: implement me ganeshpl
  save(): Promise<any> {
    return Promise.resolve();
  }

}
