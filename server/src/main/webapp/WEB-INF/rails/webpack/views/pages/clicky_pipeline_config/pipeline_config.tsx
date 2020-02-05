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

import {ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import _ from 'lodash';
import m from "mithril";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Primary, Reset} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Tabs} from "views/components/tab";
import {TabWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/tab_widget";
import {NavigationWidget} from "views/pages/clicky_pipeline_config/widgets/navigation_widget";
import {StepsWidget} from "views/pages/clicky_pipeline_config/widgets/steps_widget";
import {Page, PageState} from "views/pages/page";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";
import styles from "./index.scss";

interface PageMeta {
  pipelineName: string;
}

export interface RouteInfo<T> {
  route: string;
  params: T;
}

type SupportedTypes = PipelineConfig | Stage | Job;

export interface PipelineConfigRouteParams {
  pipeline_name: string;
  job_name?: string;
  stage_name?: string;
  tab_name: string;
}

export interface ChangeRouteEvent {
  newRoute: string;
}

export class PipelineConfigPage<T> extends Page<null, T> {
  private templateConfig?: TemplateConfig;
  private pipelineConfig?: PipelineConfig;
  private originalJSON: any;
  private readonly tabs = [] as Array<TabWidget<SupportedTypes>>;

  constructor(...tabs: Array<TabWidget<SupportedTypes>>) {
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
    this.pipelineConfig?.update().then((result) => result.do(this.onSuccess.bind(this), this.onFailure.bind(this)));
  }

  reset() {
    this.pipelineConfig = PipelineConfig.fromJSON(this.originalJSON);
  }

  changeRoute(event: ChangeRouteEvent, success: () => void): void {
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
    return (
      <div class={styles.mainContainer}>
        <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type}/>
        <div class={styles.navigation}>
          <NavigationWidget pipelineConfig={this.pipelineConfig!}
                            routeInfo={PipelineConfigPage.routeInfo()}
                            changeRoute={this.changeRoute.bind(this)}/>
        </div>

        <div class={styles.entityConfigContainer}>
          <StepsWidget routeInfo={PipelineConfigPage.routeInfo()}/>,
          <Tabs initialSelection={this.selectedTabIndex()}
                tabs={this.tabs.map((eachTab: TabWidget<SupportedTypes>) => eachTab.name())}
                contents={this.tabs.map((eachTab: TabWidget<SupportedTypes>) => eachTab.content(this.pipelineConfig!, this.templateConfig!, PipelineConfigPage.routeInfo().params, PipelineConfigPage.isSelectedTab(eachTab)))}
                beforeChange={this.onTabChange.bind(this, vnode)}/>
          <div>
            <Reset onclick={this.reset.bind(this)}>RESET</Reset>
            <Primary onclick={this.save.bind(this)}>SAVE</Primary>
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
      .then((result) => result.do(this.onSuccess.bind(this), this.onFailure.bind(this)))
      .finally(() => {
        if (this.pipelineConfig!.template()) {
          this.pageState = PageState.LOADING;
          TemplateConfig.getTemplate(this.pipelineConfig!.template()!, (result: TemplateConfig) => {
            this.templateConfig = result;
            this.pageState      = PageState.OK;
          });
        }
      });
  }

  protected getMeta(): PageMeta {
    return super.getMeta() as PageMeta;
  }

  private static isSelectedTab(eachTab: TabWidget<SupportedTypes>) {
    return _.snakeCase(eachTab.name()) === m.route.param().tab_name;
  }

  private static routeInfo(): RouteInfo<PipelineConfigRouteParams> {
    return {route: m.route.get(), params: m.route.param()};
  }

  private onTabChange(vnode: m.Vnode<null, T>, index: number, callback: () => void) {
    const route = `${this.pipelineConfig!.name()}/${_.snakeCase(this.tabs[index].name())}`;
    this.changeRoute({newRoute: route}, () => {
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

  private onSuccess(successResponse: SuccessResponse<string>) {
    this.originalJSON   = JSON.parse(successResponse.body);
    this.pipelineConfig = PipelineConfig.fromJSON(this.originalJSON);
    this.pageState      = PageState.OK;
  }

  private onFailure(errorResponse: ErrorResponse) {
    this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
    this.pageState = PageState.FAILED;
  }

  private validateRoute() {
    const routeInfo      = PipelineConfigPage.routeInfo();
    const invalidTabName = !this.tabs.find((tab) => tab.name() === routeInfo.params.tab_name);
    if (invalidTabName) {
      const parts = routeInfo.route.split("/");
      parts.pop();
      parts.push(_.snakeCase(this.tabs[0].name()));
      m.route.set(`${parts.join("/")}`);
    }
  }
}
