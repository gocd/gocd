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

import {ApiResult, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Primary, Reset} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Spinner} from "views/components/spinner";
import {Tabs} from "views/components/tab";
import {EnvironmentVariablesTabContent} from "views/pages/clicky_pipeline_config/tabs/common/environment_variables_tab_content";
import {ArtifactsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/artifacts_tab_content";
import {CustomTabTabContent} from "views/pages/clicky_pipeline_config/tabs/job/custom_tab_tab_content";
import {JobSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/job_settings_tab_content";
import {TasksTabContent} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";
import {GeneralOptionsTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/general_options_tab";
import {MaterialsTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/materials_tab_content";
import {ParametersTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/parameters_tab_content";
import {ProjectManagementTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/project_management_tab_content";
import {StagesTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/stages_tab_content";
import {JobsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/jobs_tab_content";
import {PermissionsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/permissions_tab_content";
import {StageSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/stage_settings_tab_content";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {NavigationWidget} from "views/pages/clicky_pipeline_config/widgets/navigation_widget";
import {StepsWidget} from "views/pages/clicky_pipeline_config/widgets/steps_widget";
import {Page, PageState} from "views/pages/page";
import {OperationState} from "views/pages/page_operations";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";
import styles from "./index.scss";

type SupportedTypes = PipelineConfig | Stage | Job;

interface PageMeta {
  pipelineName: string;
}

export interface RouteInfo<T> {
  route: string;
  params: T;
}

export interface PipelineConfigRouteParams {
  pipeline_name: string;
  job_name?: string;
  stage_name?: string;
  tab_name: string;
}

type TabLevel = "pipeline" | "stage" | "job";

export class PipelineConfigPage<T> extends Page<null, T> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);
  private etag: Stream<string> = Stream();
  private templateConfig?: TemplateConfig;
  private pipelineConfig?: PipelineConfig;
  private originalJSON: any;

  private tabFor: Stream<TabLevel> = Stream();

  private readonly tab: Stream<TabContent<SupportedTypes>>             = Stream();
  private readonly cachedTabs: Map<string, TabContent<SupportedTypes>> = new Map();

  constructor() {
    super();
  }

  oninit(vnode: m.Vnode<null, T>) {
    this.initializeTab();
    super.oninit(vnode);
    window.addEventListener("beforeunload", (e) => {
      if (this.isPipelineConfigChanged()) {
        e.returnValue = "";
        e.preventDefault();
        return false;
      }
      return true;
    });
  }

  onbeforeupdate(vnode: m.Vnode<null, T>, old: m.VnodeDOM<null, T>): boolean | void {
    this.initializeTab();
  }

  save(): Promise<any> {
    this.flashMessage.clear();
    const isValid = this.pipelineConfig!.isValid();

    if (!isValid) {
      const msg = "Validation Failed! Please fix the below errors before submitting.";
      this.flashMessage.setMessage(MessageType.alert, msg);
      return Promise.reject();
    }

    return this.pipelineConfig!.update(this.etag()).then((result) => {
      return result.do((successResponse) => {
        this.flashMessage.setMessage(MessageType.success, "Saved Successfully!");
        this.onSuccess(result, successResponse);
      }, this.onFailure.bind(this));
    });
  }

  reset() {
    this.pipelineConfig = PipelineConfig.fromJSON(this.originalJSON);
  }

  changeRoute(newRoute: string, success: () => void): void {
    if (this.isPipelineConfigChanged()) {
      new ConfirmationDialog("Unsaved changes",
                             "There are unsaved changes on your form. 'Proceed' will discard these changes",
                             () => {
                               this.reset();
                               return Promise.resolve(success());
                             }
      ).render();
    } else {
      success();
    }
  }

  isPipelineConfigChanged() {
    const newPayload = this.pipelineConfig?.toApiPayload();
    return !_.isEqual(newPayload, PipelineConfig.fromJSON(this.originalJSON).toApiPayload());
  }

  componentToDisplay(vnode: m.Vnode<null, T>): m.Children {
    const doesPipelineExists = !!this.pipelineConfig;
    const doesTemplateExists = this.pipelineConfig!.isUsingTemplate()() ? !!this.templateConfig : true;

    if (!doesPipelineExists || !doesTemplateExists) {
      return <Spinner/>;
    }

    let saveAndResetButtons: m.Children;
    if (this.tab().shouldShowSaveAndResetButtons()) {
      saveAndResetButtons = (
        <div className={styles.buttonContainer}>
          <Reset data-test-id={"cancel"}
                 ajaxOperationMonitor={this.ajaxOperationMonitor}
                 onclick={this.reset.bind(this)}>
            RESET
          </Reset>
          <Primary data-test-id={"save"}
                   ajaxOperationMonitor={this.ajaxOperationMonitor}
                   ajaxOperation={this.save.bind(this)}>
            SAVE
          </Primary>
        </div>
      );
    }

    return [
      <div key={m.route.param().tab_name}>
        <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type}/>
        <div className={styles.mainContainer}>
          <div className={styles.navigation}>
            <NavigationWidget pipelineConfig={this.pipelineConfig!}
                              routeInfo={PipelineConfigPage.routeInfo()}
                              changeRoute={this.changeRoute.bind(this)}/>
          </div>

          <div className={styles.entityConfigContainer}>
            <StepsWidget routeInfo={PipelineConfigPage.routeInfo()}/>
            <Tabs tabs={Array.from(this.currentSelectionTabs().keys()).map((name) => name.replace("_", " "))}
                  initialSelection={this.getIndexOfCurrentSelection()}
                  contents={
                    Array.from(this.currentSelectionTabs().keys()).map((k) => {
                      if (m.route.param().tab_name === k) {
                        return this.tab().content(this.pipelineConfig!,
                                                  this.templateConfig!,
                                                  PipelineConfigPage.routeInfo().params,
                                                  this.ajaxOperationMonitor,
                                                  this.save.bind(this),
                                                  this.reset.bind(this));
                      }
                      return <Spinner/>;
                    })
                  }
                  beforeChange={this.onTabChange.bind(this, vnode)}/>
            {saveAndResetButtons}
          </div>
        </div>
      </div>
    ];
  }

  pageName(): string {
    return "Pipelines";
  }

  fetchData(vnode: m.Vnode<null, T>): Promise<any> {
    return PipelineConfig.get(this.getMeta().pipelineName)
                         .then((result) => {
                           return result.do(this.onSuccess.bind(this, result), this.onFailure.bind(this));
                         });
  }

  protected getMeta(): PageMeta {
    return super.getMeta() as PageMeta;
  }

  private static routeInfo(): RouteInfo<PipelineConfigRouteParams> {
    return {route: m.route.get(), params: m.route.param()};
  }

  private static routeForTabName(route: string, tabName: string): string {
    const parts = route.split("/");
    parts.pop();
    parts.push(getTabName(tabName));
    return parts.join("/");
  }

  private currentSelectionTabs() {
    return this.getAllTabsInformation().get(this.tabFor())!;
  }

  private getIndexOfCurrentSelection(): number {
    return Array.from(this.currentSelectionTabs().keys()).indexOf(m.route.param().tab_name);
  }

  private onTabChange(vnode: m.Vnode<null, T>, index: number, callback: () => void) {
    const tab   = Array.from(this.currentSelectionTabs().keys())[index];
    const route = PipelineConfigPage.routeForTabName(PipelineConfigPage.routeInfo().route, tab);

    this.changeRoute(route, () => {
      callback();
      if (m.route.get() !== route) {
        m.route.set(route);
      }
    });
  }

  private onSuccess(result: ApiResult<any>, successResponse: SuccessResponse<string>) {
    this.etag(result.getEtag()!);

    this.originalJSON   = JSON.parse(successResponse.body);
    this.pipelineConfig = PipelineConfig.fromJSON(this.originalJSON);

    if (this.pipelineConfig!.template()) {
      return TemplateConfig.getTemplate(this.pipelineConfig!.template()!, (result: TemplateConfig) => {
        this.templateConfig = result;
        this.pageState      = PageState.OK;
      });
    }

    return Promise.resolve(successResponse);
  }

  private onFailure(errorResponse: ErrorResponse) {
    const parsed = JSON.parse(errorResponse.body!);
    this.flashMessage.setMessage(MessageType.alert, parsed.message);

    try {
      if (parsed.data) {
        this.pipelineConfig!.consumeErrorsResponse(parsed.data);
      }
    } catch (e) {
      this.pageState = PageState.FAILED;
    }

    return Promise.reject(errorResponse);
  }

  private initializeTab() {
    const routeInfo = PipelineConfigPage.routeInfo();
    const tabList   = this.getAllTabsInformation();

    const tabName = routeInfo.params.tab_name;
    let tab       = this.cachedTabs.get(tabName);

    if (tab) {
      return this.tab(tab);
    }

    const tabFor: TabLevel = _.includes(Array.from(tabList.get("pipeline")!.keys()), tabName) ? "pipeline"
      : _.includes(Array.from(tabList.get("stage")!.keys()), tabName) ? "stage" : "job";

    tab = new (tabList.get(tabFor)!.get(tabName)!)();
    this.tabFor(tabFor);
    this.cachedTabs.set(tabName, tab);
    this.tab(tab);
  }

  private getAllTabsInformation() {
    const pipelineTabList: Map<string, new() => TabContent<SupportedTypes>> = new Map();
    const stageTabList: Map<string, new() => TabContent<SupportedTypes>>    = new Map();
    const jobTabList: Map<string, new() => TabContent<SupportedTypes>>      = new Map();

    [
      GeneralOptionsTabContent,
      ProjectManagementTabContent,
      MaterialsTabContent,
      StagesTabContent,
      EnvironmentVariablesTabContent,
      ParametersTabContent
    ].forEach(t => pipelineTabList.set(_.snakeCase(t.tabName()), t));

    [
      StageSettingsTabContent,
      JobsTabContent,
      EnvironmentVariablesTabContent,
      PermissionsTabContent
    ].forEach(t => stageTabList.set(_.snakeCase(t.tabName()), t));

    [
      JobSettingsTabContent,
      TasksTabContent,
      ArtifactsTabContent,
      EnvironmentVariablesTabContent,
      CustomTabTabContent
    ].forEach(t => jobTabList.set(_.snakeCase(t.tabName()), t));

    const tabList: Map<string, Map<string, new() => TabContent<SupportedTypes>>> = new Map();
    tabList.set("pipeline" as TabLevel, pipelineTabList);
    tabList.set("stage" as TabLevel, stageTabList);
    tabList.set("job" as TabLevel, jobTabList);

    return tabList;
  }
}

function getTabName(name: string) {
  return _.snakeCase(name);
}
