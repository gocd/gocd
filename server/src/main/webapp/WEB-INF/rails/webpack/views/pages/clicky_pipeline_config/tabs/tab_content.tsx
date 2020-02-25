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
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PageLoadError} from "views/components/page_load_error";
import {Spinner} from "views/components/spinner";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {PageState} from "views/pages/page";

export abstract class TabContent<T> {
  private pageState = PageState.OK;

  //render only selected tab
  public content(pipelineConfig: PipelineConfig,
                 templateConfig: TemplateConfig,
                 routeParams: PipelineConfigRouteParams,
                 isSelectedTab: boolean): m.Children {
    switch (this.pageState) {
      case PageState.FAILED:
        return <PageLoadError message={`There was a problem fetching ${this.name()} tab`}/>;
      case PageState.LOADING:
        return <Spinner/>;
      case PageState.OK:
        if (isSelectedTab) {
          const entity = this.selectedEntity(pipelineConfig, routeParams) as T;
          return this.renderer(entity, templateConfig);
        }
        return <div> Not a selected tab</div>;
    }
  }

  public abstract name(): string;

  public pageLoadFailure() {
    this.pageState = PageState.FAILED;
  }

  public pageLoading() {
    this.pageState = PageState.LOADING;
  }

  public pageLoaded() {
    this.pageState = PageState.OK;
  }

  protected abstract renderer(entity: T, templateConfig: TemplateConfig): m.Children;

  protected abstract selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): T;
}
