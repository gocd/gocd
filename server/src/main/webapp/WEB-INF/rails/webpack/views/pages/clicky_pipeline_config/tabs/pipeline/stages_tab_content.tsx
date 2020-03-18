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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import s from "underscore.string";
import {Secondary} from "views/components/buttons";
import {Delete} from "views/components/icons";
import {Table} from "views/components/table";
import {StageModal} from "views/pages/clicky_pipeline_config/modal/add_or_edit_modal";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {TemplateEditor} from "views/pages/pipelines/template_editor";

export class StagesTabContent extends TabContent<PipelineConfig> {
  static tabName(): string {
    return "Stages";
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig) {
    return [
      <TemplateEditor pipelineConfig={entity} isUsingTemplate={entity.isUsingTemplate()}
                      paramList={entity.parameters}/>,
      <StagesWidget stages={entity.stages} isUsingTemplate={entity.isUsingTemplate()}
                    isEditable={!entity.origin().isDefinedInConfigRepo()}/>
    ];
  }
}

export interface Attrs {
  stages: Stream<NameableSet<Stage>>;
  isUsingTemplate: Stream<boolean>;
  isEditable: boolean;
}

export class StagesWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.isUsingTemplate()) {
      return;
    }
    return <div data-test-id={"stages-container"}>
      <Table headers={StagesWidget.getTableHeaders(vnode.attrs.isEditable)}
             data={StagesWidget.getTableData(vnode.attrs.stages(), vnode.attrs.isEditable)}
             draggable={vnode.attrs.isEditable}
             dragHandler={StagesWidget.reArrange.bind(this, vnode.attrs.stages)}/>
      <Secondary disabled={!vnode.attrs.isEditable}
                 dataTestId={"add-stage-button"}
                 onclick={StagesWidget.showAddStageModal.bind(this, vnode.attrs.stages)}>Add new stage</Secondary>
    </div>;
  }

  private static getTableHeaders(isEditable: boolean) {
    const headers = ["Stage Name", "Trigger Type", "Jobs"];
    if (isEditable) {
      headers.push("Remove");
    }
    return headers;
  }

  private static getTableData(stages: NameableSet<Stage>, isEditable: boolean): m.Child[][] {
    return Array.from(stages.values()).map((stage: Stage) => {
      const cells: m.Child[] = [stage.name(), stage.approval().typeAsString(), stage.jobs().length];
      if (isEditable) {
        cells.push(<Delete iconOnly={true} onclick={() => stages.delete(stage)}
                           data-test-id={`${s.slugify(stage.name())}-delete-icon`}/>);
      }
      return cells;
    });
  }

  private static reArrange(stages: Stream<NameableSet<Stage>>, oldIndex: number, newIndex: number) {
    const array = Array.from(stages().values());
    array.splice(newIndex, 0, array.splice(oldIndex, 1)[0]);
    stages(new NameableSet(array));
  }

  private static showAddStageModal(stages: Stream<NameableSet<Stage>>) {
    return StageModal.forAdd((updatedStage: Stage) => {
      stages().add(updatedStage);
    }).render();
  }
}
