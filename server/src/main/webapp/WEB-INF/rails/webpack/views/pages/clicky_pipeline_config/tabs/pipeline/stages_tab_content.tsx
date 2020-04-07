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

import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {
  DependentPipeline,
  PipelineStructureWithAdditionalInfo
} from "models/internal_pipeline_structure/pipeline_structure";
import {PipelineStructureCRUD} from "models/internal_pipeline_structure/pipeline_structure_crud";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import s from "underscore.string";
import {Secondary} from "views/components/buttons";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import {Delete} from "views/components/icons";
import {Table} from "views/components/table";
import {PipelineConfigPage, PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {EntityReOrderHandler} from "views/pages/clicky_pipeline_config/tabs/common/re_order_entity_widget";
import {AddStageModal} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/add_stage_modal";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {TemplateEditor} from "views/pages/pipelines/template_editor";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";

export class StagesTabContent extends TabContent<PipelineConfig> {
  private dependentPipelines: Stream<DependentPipeline[]> = Stream([] as DependentPipeline[]);

  constructor() {
    super();
    this.fetchStageDependencyInformation(PipelineConfigPage.routeInfo().params.pipeline_name);
  }

  static tabName(): string {
    return "Stages";
  }

  public shouldShowSaveAndResetButtons(): boolean {
    return false;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig,
                     templateConfig: TemplateConfig,
                     flashMessage: FlashMessageModelWithTimeout,
                     save: () => any,
                     reset: () => any) {
    return [
      <TemplateEditor pipelineConfig={entity} isUsingTemplate={entity.isUsingTemplate()}
                      paramList={entity.parameters}/>,
      <StagesWidget stages={entity.stages}
                    dependentPipelines={this.dependentPipelines}
                    isUsingTemplate={entity.isUsingTemplate()}
                    flashMessage={flashMessage}
                    pipelineConfigSave={save}
                    pipelineConfigReset={reset}
                    isEditable={!entity.origin().isDefinedInConfigRepo()}/>
    ];
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

export interface Attrs {
  stages: Stream<NameableSet<Stage>>;
  isUsingTemplate: Stream<boolean>;
  isEditable: boolean;
  flashMessage: FlashMessageModelWithTimeout;
  pipelineConfigSave: () => any;
  pipelineConfigReset: () => any;
  dependentPipelines: Stream<DependentPipeline[]>;
}

export interface State {
  entityReOrderHandler: EntityReOrderHandler;
  getModal: () => AddStageModal;
}

export class StagesWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.entityReOrderHandler = new EntityReOrderHandler("stage",
                                                                vnode.attrs.flashMessage,
                                                                vnode.attrs.pipelineConfigSave,
                                                                vnode.attrs.pipelineConfigReset);

    vnode.state.getModal = () => new AddStageModal(vnode.attrs.stages(), vnode.attrs.pipelineConfigSave);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.isUsingTemplate()) {
      return;
    }

    return <div data-test-id={"stages-container"}>
      {vnode.state.entityReOrderHandler.getReOrderConfirmationView()}
      <Table headers={StagesWidget.getTableHeaders(vnode.attrs.isEditable)}
             data={this.getTableData(vnode)}
             draggable={vnode.attrs.isEditable}
             dragEnd={vnode.state.entityReOrderHandler.onReOder.bind(vnode.state.entityReOrderHandler)}
             dragHandler={StagesWidget.reArrange.bind(this, vnode.attrs.stages)}/>
      <Secondary disabled={!vnode.attrs.isEditable}
                 dataTestId={"add-stage-button"}
                 onclick={() => vnode.state.getModal().render()}>Add new stage</Secondary>
    </div>;
  }

  private static getTableHeaders(isEditable: boolean) {
    const headers = ["Stage Name", "Trigger Type", "Jobs"];
    if (isEditable) {
      headers.push("Remove");
    }
    return headers;
  }

  private static reArrange(stages: Stream<NameableSet<Stage>>, oldIndex: number, newIndex: number) {
    const array = Array.from(stages().values());
    array.splice(newIndex, 0, array.splice(oldIndex, 1)[0]);
    stages(new NameableSet(array));
  }

  private getTableData(vnode: m.Vnode<Attrs, State>): m.Child[][] {
    const stages     = Array.from(vnode.attrs.stages().values());
    const isEditable = vnode.attrs.isEditable;

    return stages.map((stage: Stage) => {
      let deleteDisabledMessage: string | undefined;

      if (Array.from(stages.values()).length === 1) {
        deleteDisabledMessage = "Can not delete the only stage from the pipeline.";
      }

      const stageDependentPipelines = vnode.attrs.dependentPipelines().reduce((dependent, ele) => {
        if (ele.depends_on_stage === stage.name()) {
          dependent.push(ele.dependent_pipeline_name);
        }
        return dependent;
      }, [] as string[]);

      if (stageDependentPipelines.length > 0) {
        deleteDisabledMessage = `Can not delete stage '${stage.name()}' as pipeline(s) '${stageDependentPipelines}' depends on it.`;
      }

      const cells: m.Child[] = [stage.name(), stage.approval().typeAsString(), stage.jobs().length];
      if (isEditable) {
        cells.push(<Delete iconOnly={true}
                           title={deleteDisabledMessage}
                           onclick={this.deleteStage.bind(this, vnode, stage)}
                           disabled={!!deleteDisabledMessage}
                           data-test-id={`${s.slugify(stage.name())}-delete-icon`}/>);
      }
      return cells;
    });
  }

  private deleteStage(vnode: m.Vnode<Attrs, State>, stageToDelete: Stage) {
    new ConfirmationDialog(
      "Delete Stage",
      <div>Do you want to delete the stage '<em>{stageToDelete.name()}</em>'?</div>,
      this.onDelete.bind(this, vnode, stageToDelete)
    ).render();
  }

  private onDelete(vnode: m.Vnode<Attrs, State>, stageToDelete: Stage) {
    vnode.attrs.stages().delete(stageToDelete);
    return vnode.attrs.pipelineConfigSave().then(() => {
      vnode.attrs.flashMessage.setMessage(MessageType.success, `Stage '${stageToDelete.name()}' deleted successfully.`);
    }).catch((errorResponse: ErrorResponse) => {
      vnode.attrs.stages().add(stageToDelete);
      vnode.attrs.flashMessage.consumeErrorResponse(errorResponse);
    }).finally(m.redraw.sync);
  }
}
