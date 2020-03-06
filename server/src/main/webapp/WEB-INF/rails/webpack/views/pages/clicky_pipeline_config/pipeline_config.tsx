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

export class PipelineConfigPage<T> extends Page<null, T> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);
  private etag: Stream<string> = Stream();
  private templateConfig?: TemplateConfig;
  private pipelineConfig?: PipelineConfig;
  private originalJSON: any;
  private readonly tabs        = [] as Array<TabContent<SupportedTypes>>;

  constructor(...tabs: Array<TabContent<SupportedTypes>>) {
    super();
    this.tabs = tabs;
  }

  oninit(vnode: m.Vnode<null, T>) {
    this.validateRoute();
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

  save() {
    this.flashMessage.clear();
    return this.pipelineConfig!.update(this.etag()).then((result) => {
      return result.do(() => {
        this.flashMessage.setMessage(MessageType.success, "Saved Successfully!");
        this.onSuccess.bind(this, result);
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

    return (
      <div>
        <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type}/>
        <div class={styles.mainContainer}>
          <div class={styles.navigation}>
            <NavigationWidget pipelineConfig={this.pipelineConfig!}
                              routeInfo={PipelineConfigPage.routeInfo()}
                              changeRoute={this.changeRoute.bind(this)}/>
          </div>

          <div class={styles.entityConfigContainer}>
            <StepsWidget routeInfo={PipelineConfigPage.routeInfo()}/>
            <Tabs initialSelection={this.selectedTabIndex()}
                  tabs={this.tabs.map((eachTab: TabContent<SupportedTypes>) => eachTab.name())}
                  contents={this.tabs.map((eachTab: TabContent<SupportedTypes>) => {
                    return eachTab.content(this.pipelineConfig!,
                                           this.templateConfig!,
                                           PipelineConfigPage.routeInfo().params,
                                           PipelineConfigPage.isSelectedTab(eachTab),
                                           this.ajaxOperationMonitor);
                  })}
                  beforeChange={this.onTabChange.bind(this, vnode)}/>
            <div class={styles.buttonContainer}>
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
          </div>
        </div>
      </div>
    );
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

  private static isSelectedTab(eachTab: TabContent<SupportedTypes>) {
    return _.snakeCase(eachTab.name()) === m.route.param().tab_name;
  }

  private static routeInfo(): RouteInfo<PipelineConfigRouteParams> {
    return {route: m.route.get(), params: m.route.param()};
  }

  private static routeForTabName(route: string, tabName: string): string {
    const parts = route.split("/");
    parts.pop();
    parts.push(_.snakeCase(tabName));
    return parts.join("/");
  }

  private onTabChange(vnode: m.Vnode<null, T>, index: number, callback: () => void) {
    const route = PipelineConfigPage.routeForTabName(PipelineConfigPage.routeInfo().route, this.tabs[index].name());
    this.changeRoute(route, () => {
      callback();
      if (m.route.get() !== route) {
        m.route.set(route);
      }
    });
  }

  private selectedTabIndex() {
    return this.tabs.findIndex((eachTab) => {
      return PipelineConfigPage.isSelectedTab(eachTab);
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
        this.pipelineConfig = PipelineConfig.fromJSON(parsed.data);
        this.pipelineConfig.consumeErrorsResponse(parsed.data);
      }
    } catch (e) {
      this.pageState = PageState.FAILED;
    }
  }

  private validateRoute() {
    const routeInfo      = PipelineConfigPage.routeInfo();
    const invalidTabName = !this.tabs.find((tab) => tab.name() === routeInfo.params.tab_name);
    if (invalidTabName) {
      m.route.set(PipelineConfigPage.routeForTabName(routeInfo.route, this.tabs[0].name()));
    }
  }
}
