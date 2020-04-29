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
import {FlashMessage, FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import {Spinner} from "views/components/spinner";
import {Tabs} from "views/components/tab";
import styles from "views/pages/clicky_pipeline_config/index.scss";
import {PipelineConfigSPARouteHelper} from "views/pages/clicky_pipeline_config/tabs/route_helper";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {NavigationWidget} from "views/pages/clicky_pipeline_config/widgets/navigation_widget";
import {StepsWidget} from "views/pages/clicky_pipeline_config/widgets/steps_widget";
import {Page, PageState} from "views/pages/page";
import {OperationState} from "views/pages/page_operations";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";

export type SupportedTypes = TemplateConfig | PipelineConfig | Stage | Job;

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

export type TabLevel = "pipeline" | "stage" | "job";

// flashMessage needs to be of type FlashMessageModelWithTimeout because of Page defining that field.
// And interval can not be set to Infinity as when Infinity is specified to settimeout function, the settimeout evaluates the function immediately
// Hence, set interval to a really long number, one day.
const interval = 1000 * 60 * 60 * 24;

export abstract class TabHandler<T> extends Page<null, T> {
  protected flashMessage                                                  = new FlashMessageModelWithTimeout(interval);
  protected ajaxOperationMonitor                                         = Stream<OperationState>(OperationState.UNKNOWN);
  protected etag: Stream<string>                                         = Stream();
  protected readonly tab: Stream<TabContent<SupportedTypes>>             = Stream();
  protected readonly cachedTabs: Map<string, TabContent<SupportedTypes>> = new Map();
  protected readonly onSuccessfulPipelineSave: Map<string, (() => any)>  = new Map();
  protected originalJSON: any;

  protected entity?: PipelineConfig | TemplateConfig;

  oninit(vnode: m.Vnode<null, T>) {
    this.initializeTab();
    super.oninit(vnode);
    window.addEventListener("beforeunload", (e) => {
      if (this.hasEntityConfigChanged()) {
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

  abstract pageName(): string;

  abstract fetchData(vnode: m.Vnode<null, T>): Promise<any>;

  abstract getEntity(): PipelineConfig | TemplateConfig;

  abstract getOriginalEntity(): PipelineConfig | TemplateConfig;

  abstract setEntity(entity: PipelineConfig | TemplateConfig): void;

  abstract getAllTabsInformation(): Map<string, Map<string, new() => TabContent<SupportedTypes>>>;

  changeRoute(newRoute: string, success: () => void): void {
    if (this.hasEntityConfigChanged()) {
      new ConfirmationDialog("Unsaved changes",
                             "There are unsaved changes on your form. 'Proceed' will discard these changes",
                             () => {
                               this.reset();
                               this.flashMessage.clear();
                               return Promise.resolve(success());
                             }
      ).render();
    } else {
      success();
    }
  }

  abstract save(): Promise<any>;

  abstract reset(): any;

  abstract onSuccess(result: ApiResult<any>, successResponse: SuccessResponse<string>): Promise<any>;

  abstract shouldShowSpinner(): boolean;

  onSuccessfulPipelineSaveCallback() {
    Array.from(this.onSuccessfulPipelineSave.values()).forEach(fn => fn());
  }

  componentToDisplay(vnode: m.Vnode<null, T>): m.Children {
    if (this.shouldShowSpinner()) {
      return <Spinner/>;
    }

    let configRepoPipelineMessage: m.Children;
    if (this.isPipelineConfigPage()) {
      const entity = (this.getEntity() as PipelineConfig);
      if (entity.origin().isDefinedInConfigRepo()) {
        const message = <div>
          Can not edit pipeline '{entity.name()}' as it is defined in <a href={`/go/admin/config_repos#!${entity.origin().id()}`} target="_blank">{entity.origin().id()}</a> Config Repository!
        </div>;

        configRepoPipelineMessage = <FlashMessage message={message} type={MessageType.warning}/>;
      }
    }

    let flashMessage;
    if (this.flashMessage.hasMessage()) {
      flashMessage = <FlashMessage message={this.flashMessage.message}
                                   type={this.flashMessage.type}
                                   dismissible={true} onDismiss={this.flashMessage.clear.bind(this.flashMessage)}/>;
    }

    return [
      <div key={m.route.param().tab_name}>
        {flashMessage}
        {configRepoPipelineMessage}
        <div className={styles.mainContainer}>
          <div className={styles.navigation}>
            <NavigationWidget config={this.getOriginalEntity()}
                              isTemplateConfig={!this.isPipelineConfigPage()}
                              templateConfig={this.getAssociatedTemplateWithPipeline()}
                              routeInfo={PipelineConfigSPARouteHelper.routeInfo()}
                              changeRoute={this.changeRoute.bind(this)}/>
          </div>
          <div className={styles.entityConfigContainer}>
            <StepsWidget routeInfo={PipelineConfigSPARouteHelper.routeInfo()}/>
            <Tabs tabs={Array.from(this.currentSelectionTabs().keys()).map((name) => name.replace("_", " "))}
                  initialSelection={this.getIndexOfCurrentSelection()}
                  contents={
                    Array.from(this.currentSelectionTabs().keys()).map((k) => {
                      if (m.route.param().tab_name === k) {
                        return this.tab().content(this.getEntity(),
                                                  this.getAssociatedTemplateWithPipeline()!,
                                                  PipelineConfigSPARouteHelper.routeInfo().params,
                                                  this.ajaxOperationMonitor,
                                                  this.flashMessage,
                                                  this.save.bind(this),
                                                  this.reset.bind(this));
                      }
                      return <Spinner/>;
                    })
                  }
                  beforeChange={this.onTabChange.bind(this, vnode)}/>
            {this.getSaveAndResetButtons()}
          </div>
        </div>
      </div>
    ];
  }

  abstract getAssociatedTemplateWithPipeline(): TemplateConfig | undefined;

  abstract shouldShowSaveAndResetButtons(entity: PipelineConfig | TemplateConfig): boolean;

  protected currentSelectionTabs() {
    return this.getAllTabsInformation().get(this.getTabFor())!;
  }

  protected routeForTabName(route: string, tabName: string): string {
    const parts = route.split("/");
    parts.pop();
    parts.push(_.snakeCase(tabName));
    return parts.join("/");
  }

  protected abstract hasEntityConfigChanged(): boolean;

  protected onFailure(errorResponse: ErrorResponse) {
    const parsed = JSON.parse(errorResponse.body!);
    this.flashMessage.consumeErrorResponse(errorResponse);

    try {
      if (parsed.data) {
        this.getEntity().consumeErrorsResponse(parsed.data);
      }
    } catch (e) {
      this.pageState = PageState.FAILED;
    }

    return Promise.reject(errorResponse);
  }

  protected getSaveAndResetButtons(): m.Children {
    let saveSuccessOrFailureIdentifier;
    if (this.flashMessage.hasMessage()) {
      saveSuccessOrFailureIdentifier = this.flashMessage.type === MessageType.success
        ? <span className={styles.iconCheck} data-test-id="update-successful"/>
        : <span className={styles.iconError} data-test-id="update-failure"/>;
    }

    let saveAndResetButtons: m.Children;
    if (this.shouldShowSaveAndResetButtons(this.getEntity()) && this.tab().shouldShowSaveAndResetButtons()) {
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
            {saveSuccessOrFailureIdentifier}
            SAVE
          </Primary>
        </div>
      );
    }

    return saveAndResetButtons;
  }

  private initializeTab() {
    const routeInfo = PipelineConfigSPARouteHelper.routeInfo();
    const tabList   = this.getAllTabsInformation();

    const tabName = routeInfo.params.tab_name;
    let tab       = this.cachedTabs.get(tabName);

    if (tab) {
      return this.tab(tab);
    }

    tab = new (tabList.get(this.getTabFor())!.get(tabName)!)();
    this.onSuccessfulPipelineSave.set(tabName, tab.onSuccessfulPipelineConfigSave.bind(tab));
    this.cachedTabs.set(tabName, tab);
    this.tab(tab);
  }

  private getTabFor(): TabLevel {
    const params = PipelineConfigSPARouteHelper.routeInfo().params;
    if (!params.stage_name) {
      return "pipeline";
    }

    if (params.job_name) {
      return "job";
    }

    return "stage";
  }

  private getIndexOfCurrentSelection(): number {
    return Array.from(this.currentSelectionTabs().keys()).indexOf(m.route.param().tab_name);
  }

  private onTabChange(vnode: m.Vnode<null, T>, index: number, callback: () => void) {
    const tab   = Array.from(this.currentSelectionTabs().keys())[index];
    const route = this.routeForTabName(PipelineConfigSPARouteHelper.routeInfo().route, tab);

    this.changeRoute(route, () => {
      callback();
      if (m.route.get() !== route) {
        m.route.set(route);
      }
    });
  }

  private isPipelineConfigPage() {
    return !!(super.getMeta() ? super.getMeta() : {}).pipelineName;
  }
}
