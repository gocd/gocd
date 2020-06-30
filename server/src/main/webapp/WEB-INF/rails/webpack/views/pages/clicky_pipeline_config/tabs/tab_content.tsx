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
import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {PageLoadError} from "views/components/page_load_error";
import {Spinner} from "views/components/spinner";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {PageState} from "views/pages/page";
import {OperationState} from "views/pages/page_operations";
import styles from "./tab_content.scss";

export abstract class TabContent<T> {
  private entity: PipelineConfig | TemplateConfig | undefined;
  private pageState = PageState.OK;

  //render only selected tab
  public content(pipelineConfig: PipelineConfig | TemplateConfig,
                 templateConfig: TemplateConfig,
                 routeParams: PipelineConfigRouteParams,
                 ajaxOperationMonitor: Stream<OperationState>,
                 flashMessage: FlashMessageModelWithTimeout,
                 pipelineConfigSave: () => Promise<any>,
                 pipelineConfigReset: () => void): m.Children {
    this.entity = pipelineConfig;
    switch (this.pageState) {
      case PageState.FAILED:
        return <PageLoadError message={`There was a problem fetching current tab`}/>;
      case PageState.LOADING:
        return <Spinner/>;
      case PageState.OK:
        const entity         = this.selectedEntity(pipelineConfig, routeParams) as T;
        const saveInProgress = ajaxOperationMonitor() === OperationState.IN_PROGRESS;

        return <div class={saveInProgress ? styles.blur : ""}>
          {saveInProgress ? <Spinner/> : undefined}
          {this.renderer(entity, templateConfig, flashMessage, pipelineConfigSave, pipelineConfigReset)}
        </div>;
    }
  }

  public pageLoadFailure() {
    this.pageState = PageState.FAILED;
  }

  public pageLoading() {
    this.pageState = PageState.LOADING;
  }

  public pageLoaded() {
    this.pageState = PageState.OK;
  }

  public isEntityDefinedInConfigRepository(): boolean {
    return this.isPipelineConfigView() && !!this.entity && (this.entity as PipelineConfig).isDefinedInConfigRepo();
  }

  public shouldShowSaveAndResetButtons(): boolean {
    return true;
  }

  public isPipelineConfigView() {
    const meta = document.body.getAttribute("data-meta");

    if (meta && JSON.parse(meta!)) {
      return !!JSON.parse(meta!)!.pipelineName;
    }

    return false;
  }

  public isTemplateView() {
    return !this.isPipelineConfigView();
  }

  public onSuccessfulPipelineConfigSave() {
    //do nothing
  }

  public onPipelineConfigReset() {
    //do nothing
  }

  protected abstract renderer(entity: T,
                              templateConfig: TemplateConfig,
                              flashMessage: FlashMessageModelWithTimeout,
                              pipelineConfigSave: () => any,
                              pipelineConfigReset: () => any): m.Children;

  protected abstract selectedEntity(pipelineConfig: PipelineConfig | TemplateConfig,
                                    routeParams: PipelineConfigRouteParams): T;
}
