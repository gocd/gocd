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
import {Material} from "models/materials/types";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Secondary} from "views/components/buttons";
import {Delete, Edit, IconGroup} from "views/components/icons";
import {Table} from "views/components/table";
import style from "views/pages/clicky_pipeline_config/index.scss";
import {MaterialModal} from "views/pages/clicky_pipeline_config/modal/add_or_edit_modal";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/tab_widget";

export class MaterialsTab extends TabWidget<PipelineConfig> {

  addNewMaterial(materials: NameableSet<Material>) {
    MaterialModal.forAdd((material: Material) => {
      materials.add(material);
    }).render();
  }

  updateMaterial(material: Material) {
    MaterialModal.forEdit(material, (updateMaterial: Material) => {
      material.type(updateMaterial.type());
      material.attributes(updateMaterial.attributes());
    }).render();
  }

  name(): string {
    return "Materials";
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig) {
    return <div class={style.materialTab}>
      <Table headers={["Type", "Material Name", "Url", ""]} data={this.tableData(entity.materials())}/>
      <Secondary dataTestId={"add-material-button"}
                 onclick={this.addNewMaterial.bind(this, entity.materials())}>
        Add Material
      </Secondary>
    </div>;
  }

  private tableData(materials: NameableSet<Material>) {
    return Array.from(materials.values()).map((material) => {
      return [
        material.type(),
        material.name(),
        material.materialUrl(),
        <IconGroup>
          <Edit onclick={this.updateMaterial.bind(this, material)} data-test-id={"edit-material-button"}/>
          <Delete onclick={() => materials.delete(material)} data-test-id={"delete-material-button"}/>
        </IconGroup>
      ];
    });
  }
}
