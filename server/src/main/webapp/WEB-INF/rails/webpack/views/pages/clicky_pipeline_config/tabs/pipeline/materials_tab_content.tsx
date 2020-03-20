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
import {Scms} from "models/materials/pluggable_scm";
import {PluggableScmCRUD} from "models/materials/pluggable_scm_crud";
import {Material, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {PackagesCRUD} from "models/package_repositories/packages_crud";
import {Packages} from "models/package_repositories/package_repositories";
import {Materials, PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Secondary} from "views/components/buttons";
import {Delete, Edit, IconGroup} from "views/components/icons";
import {Table} from "views/components/table";
import style from "views/pages/clicky_pipeline_config/index.scss";
import {MaterialModal} from "views/pages/clicky_pipeline_config/modal/add_or_edit_modal";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";

export class MaterialsTabContent extends TabContent<PipelineConfig> {
  private readonly packages: Stream<Packages> = Stream();
  private readonly scmMaterials: Stream<Scms> = Stream();

  constructor() {
    super();
    this.fetchAllPackages();
    this.fetchScmMaterials();
  }

  static tabName(): string {
    return "Materials";
  }

  addNewMaterial(materials: Materials) {
    MaterialModal.forAdd((material: Material) => {
      materials.push(material);
    }).render();
  }

  updateMaterial(material: Material) {
    MaterialModal.forEdit(material, (updateMaterial: Material) => {
      material.type(updateMaterial.type());
      material.attributes(updateMaterial.attributes());
    }).render();
  }

  public shouldShowSaveAndResetButtons(): boolean {
    return false;
  }

  name(): string {
    return "Materials";
  }

  deleteMaterial(materials: Materials, material: Material, e: MouseEvent) {
    e.stopPropagation();
    materials.delete(material)
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig) {
    return <div class={style.materialTab}>
      <Table headers={["Material Name", "Type", "Url", ""]} data={this.tableData(entity.materials())}/>
      <Secondary dataTestId={"add-material-button"}
                 onclick={this.addNewMaterial.bind(this, entity.materials())}>
        Add Material
      </Secondary>
    </div>;
  }

  private tableData(materials: Materials) {
    return Array.from(materials.values()).map((material: Material) => {
      return [
        this.getMaterialDisplayName(material),
        material.typeForDisplay(),
        this.getMaterialUrlForDisplay(material),
        <IconGroup>
          <Edit onclick={this.updateMaterial.bind(this, material)} data-test-id={"edit-material-button"}/>
          <Delete onclick={this.deleteMaterial.bind(this, materials, material)}
                  data-test-id={"delete-material-button"}/>
        </IconGroup>
      ];
    });
  }

  private getMaterialUrlForDisplay(material: Material) {
    const url = material.materialUrl();
    if (url.length === 0 && material.type() === "package") {
      const attrs   = material.attributes() as PackageMaterialAttributes;
      const pkgInfo = this.packages().find((pkg) => pkg.id() === attrs.ref())!;
      return `Repository: ${pkgInfo.packageRepo().name()} - Package: ${pkgInfo.name()} ${pkgInfo.configuration().asString()}`;
    }
    if (url.length === 0 && material.type() === "plugin") {
      const attrs       = material.attributes() as PluggableScmMaterialAttributes;
      const scmMaterial = this.scmMaterials().find((pkg) => pkg.id() === attrs.ref())!;
      return `${scmMaterial.name()}: ${scmMaterial.configuration().asString()}`;
    }
    return url;
  }

  private getMaterialDisplayName(material: Material) {
    const displayName = material.displayName();
    if (displayName.length === 0 && material.type() === "package") {
      const attrs   = material.attributes() as PackageMaterialAttributes;
      const pkgInfo = this.packages().find((pkg) => pkg.id() === attrs.ref())!;
      return `${pkgInfo.packageRepo().name()}_${pkgInfo.name()}`;
    }
    if (displayName.length === 0 && material.type() === "plugin") {
      const attrs       = material.attributes() as PluggableScmMaterialAttributes;
      const scmMaterial = this.scmMaterials().find((pkg) => pkg.id() === attrs.ref())!;
      return scmMaterial.name();
    }
    return displayName;
  }

  private fetchAllPackages() {
    PackagesCRUD.all()
                .then((result) => {
                  result.do((successResponse) => {
                    this.packages(successResponse.body);
                    super.pageLoaded();
                  }, super.pageLoadFailure)
                });
  }

  private fetchScmMaterials() {
    PluggableScmCRUD.all()
                    .then((result) => {
                      result.do((successResponse) => {
                        this.scmMaterials(successResponse.body);
                      })
                    })
  }
}
