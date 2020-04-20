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

import {ApiResult} from "helpers/api_request_builder";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {
  DependentPipeline,
  PipelineStructureWithAdditionalInfo
} from "models/internal_pipeline_structure/pipeline_structure";
import {PipelineStructureCRUD} from "models/internal_pipeline_structure/pipeline_structure_crud";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Template, TemplateCache} from "models/pipeline_configs/templates_cache";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {ConfigurationTypeWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/configuration_type_widget";
import {PipelineTemplateWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/pipeline_template_widget";
import {StagesWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/stages_widget";
import {PipelineConfigSPARouteHelper} from "views/pages/clicky_pipeline_config/tabs/route_helper";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";

export class StagesTabContent extends TabContent<PipelineConfig | TemplateConfig> {
  private isPipelineDefinedOriginallyFromTemplate: Stream<boolean> = Stream();
  private stageOrTemplateProperty: Stream<"template" | "stage">    = Stream();
  private dependentPipelines: Stream<DependentPipeline[]>          = Stream([] as DependentPipeline[]);

  private cache: TemplateCache          = new TemplateCache();
  private templates: Stream<Template[]> = Stream();

  constructor() {
    super();

    if (!this.isTemplateView()) {
      this.initializePipelineRelatedInformation();
    }
  }

  static tabName(): string {
    return "Stages";
  }

  public shouldShowSaveAndResetButtons(): boolean {
    return this.stageOrTemplateProperty() === "template" && (this.templates() && this.templates().length > 0);
  }

  protected selectedEntity(pipelineConfig: PipelineConfig | TemplateConfig, routeParams: PipelineConfigRouteParams) {
    //initialize only once
    if (this.isPipelineDefinedOriginallyFromTemplate() === undefined) {
      const val = this.isTemplateView() ? false : (pipelineConfig as PipelineConfig).isUsingTemplate();
      this.isPipelineDefinedOriginallyFromTemplate(val);
      m.redraw();
    }

    if (!this.stageOrTemplateProperty()) {
      const val                    = this.isTemplateView() ? false : (pipelineConfig as PipelineConfig).isUsingTemplate();
      this.stageOrTemplateProperty = Stream(val ? "template" : "stage");
      m.redraw();
    }

    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig | TemplateConfig,
                     templateConfig: TemplateConfig,
                     flashMessage: FlashMessageModelWithTimeout,
                     save: () => any,
                     reset: () => any) {

    return <StagesOrTemplatesWidget entity={entity}
                                    isTemplateView={this.isTemplateView()}
                                    templateConfig={templateConfig}
                                    isPipelineDefinedOriginallyFromTemplate={this.isPipelineDefinedOriginallyFromTemplate}
                                    stageOrTemplateProperty={this.stageOrTemplateProperty}
                                    flashMessage={flashMessage}
                                    dependentPipelines={this.dependentPipelines}
                                    templates={this.templates}
                                    save={save} reset={reset}/>;
  }

  private initializePipelineRelatedInformation() {
    this.fetchStageDependencyInformation(PipelineConfigSPARouteHelper.routeInfo().params.pipeline_name);

    const self = this;
    self.cache.prime(() => {
      self.templates(self.cache.contents() as Template[]);
    }, () => {
      self.templates([] as Template[]);
    });
  }

  private fetchStageDependencyInformation(pipelineName: string) {
    this.pageLoading();
    PipelineStructureCRUD.allPipelines("administer", "view")
                         .then((pipelineGroups: ApiResult<PipelineStructureWithAdditionalInfo>) => {
                           pipelineGroups.do((successResponse) => {
                             const pipeline          = successResponse.body.pipelineStructure.findPipeline(pipelineName)!;
                             this.dependentPipelines = pipeline.dependantPipelines;
                             this.pageLoaded();
                           });

                         }, this.pageLoadFailure.bind(this));
  }
}

interface StagesOrTemplatesAttrs {
  entity: PipelineConfig | TemplateConfig;
  templateConfig: TemplateConfig;
  flashMessage: FlashMessageModelWithTimeout;
  save: () => any;
  reset: () => any;
  dependentPipelines: Stream<DependentPipeline[]>;
  stageOrTemplateProperty: Stream<"template" | "stage">;
  isPipelineDefinedOriginallyFromTemplate: Stream<boolean>;
  templates: Stream<Template[]>;
  isTemplateView: boolean;
}

interface StagesOrTemplatesState {
  isUsingTemplate: () => boolean;
  stageOrTemplatePropertyStream: (value?: string) => string;
}

export class StagesOrTemplatesWidget extends MithrilComponent<StagesOrTemplatesAttrs, StagesOrTemplatesState> {
  oninit(vnode: m.Vnode<StagesOrTemplatesAttrs, StagesOrTemplatesState>) {

    vnode.state.stageOrTemplatePropertyStream = (value?: string) => {
      if (value) {
        vnode.attrs.reset();
        vnode.attrs.stageOrTemplateProperty((value === "template") ? "template" : "stage");
      }

      return vnode.attrs.stageOrTemplateProperty();
    };

    vnode.state.isUsingTemplate = () => vnode.attrs.stageOrTemplateProperty() === "template";
  }

  view(vnode: m.Vnode<StagesOrTemplatesAttrs, StagesOrTemplatesState>) {
    let entity = vnode.attrs.entity;

    if (vnode.attrs.isTemplateView) {
      return <StagesWidget stages={entity.stages}
                           dependentPipelines={vnode.attrs.dependentPipelines}
                           isUsingTemplate={false}
                           flashMessage={vnode.attrs.flashMessage}
                           pipelineConfigSave={vnode.attrs.save}
                           pipelineConfigReset={vnode.attrs.reset}
                           isEditable={true}/>;
    }

    let stagesOrTemplatesView: m.Children;
    if (vnode.state.isUsingTemplate()) {
      stagesOrTemplatesView = <PipelineTemplateWidget pipelineConfig={entity as PipelineConfig}
                                                      templates={vnode.attrs.templates}/>;
    } else {
      stagesOrTemplatesView = <StagesWidget stages={entity.stages}
                                            dependentPipelines={vnode.attrs.dependentPipelines}
                                            isUsingTemplate={(entity as PipelineConfig).isUsingTemplate()}
                                            flashMessage={vnode.attrs.flashMessage}
                                            pipelineConfigSave={vnode.attrs.save}
                                            pipelineConfigReset={vnode.attrs.reset}
                                            isEditable={!(entity as PipelineConfig).origin().isDefinedInConfigRepo()}/>;
    }

    return [
      <ConfigurationTypeWidget pipelineConfig={entity}
                               isPipelineDefinedOriginallyFromTemplate={vnode.attrs.isPipelineDefinedOriginallyFromTemplate}
                               property={vnode.state.stageOrTemplatePropertyStream}/>,
      stagesOrTemplatesView
    ];
  }
}
