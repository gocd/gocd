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
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {PackageRepositoriesCRUD} from "models/package_repositories/package_repositories_crud";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {Secondary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Delete, Edit, IconGroup} from "views/components/icons";
import {Table} from "views/components/table";
import style from "views/pages/clicky_pipeline_config/index.scss";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {MaterialModal} from "views/pages/clicky_pipeline_config/modal/material_modal";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";

export class MaterialsTabContent extends TabContent<PipelineConfig> {
  private readonly pluginInfos: Stream<PluginInfos>                 = Stream(new PluginInfos());
  private readonly packageRepositories: Stream<PackageRepositories> = Stream();
  private readonly packages: Stream<Packages>                       = Stream();
  private readonly scmMaterials: Stream<Scms>                       = Stream();

  constructor() {
    super();
    this.fetchRelatedPluginInfos();
    this.fetchAllPackageReposAndPackages();
    this.fetchScmMaterials();
  }

  static tabName(): string {
    return "Materials";
  }

  addNewMaterial(pipelineConfig: PipelineConfig) {
    MaterialModal.forAdd(this.packageRepositories, this.pluginInfos, (material: Material) => {
      pipelineConfig.materials().push(material);
    }).render();
  }

  updateMaterial(material: Material, pipelineConfig: PipelineConfig) {
    MaterialModal.forEdit(material, this.packageRepositories, this.pluginInfos, (updateMaterial: Material) => {
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

  deleteMaterial(pipelineConfig: PipelineConfig, material: Material, e: MouseEvent) {
    e.stopPropagation();
    pipelineConfig.materials().delete(material);
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig, pipelineConfigSave: () => any, pipelineConfigReset: () => any) {
    const allErrors = entity.materials()
                            .map((material) => material.allErrors())
                            .filter((errors) => errors.length > 0);
    const errorMsgs = allErrors.length === 0
      ? undefined
      : <FlashMessage type={MessageType.alert} message={allErrors}/>;
    return <div class={style.materialTab}>
      {errorMsgs}
      <Table headers={["Material Name", "Type", "Url", ""]} data={this.tableData(entity)}/>
      <Secondary dataTestId={"add-material-button"}
                 onclick={this.addNewMaterial.bind(this, entity)}>
        Add Material
      </Secondary>
    </div>;
  }

  private tableData(pipelineConfig: PipelineConfig) {
    return Array.from(pipelineConfig.materials().values()).map((material: Material) => {
      return [
        this.getMaterialDisplayName(material),
        material.typeForDisplay(),
        this.getMaterialUrlForDisplay(material),
        <IconGroup>
          <Edit onclick={this.updateMaterial.bind(this, material, pipelineConfig)}
                data-test-id={"edit-material-button"}/>
          <Delete onclick={this.deleteMaterial.bind(this, pipelineConfig, material)}
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

  private fetchAllPackageReposAndPackages() {
    Promise.all([PackageRepositoriesCRUD.all(), PackagesCRUD.all()])
           .then((result) => {
             result[0].do((successResponse) => {
               this.packageRepositories(successResponse.body);
             }, super.pageLoadFailure);
             result[1].do((successResponse) => {
               this.packages(successResponse.body);
             }, super.pageLoadFailure);
           })
           .finally(() => super.pageLoaded());
  }

  private fetchScmMaterials() {
    PluggableScmCRUD.all()
                    .then((result) => {
                      result.do((successResponse) => {
                        this.scmMaterials(successResponse.body);
                        super.pageLoaded();
                      }, super.pageLoadFailure)
                    })
  }

  private fetchRelatedPluginInfos() {
    Promise.all([PluginInfoCRUD.all({type: ExtensionTypeString.PACKAGE_REPO}), PluginInfoCRUD.all({type: ExtensionTypeString.SCM})])
           .then((result) => {
             result.forEach((apiResult) => apiResult.do((successResponse) => {
               this.pluginInfos().push(...successResponse.body);
               this.pageLoaded();
             }, super.pageLoadFailure));
           });
  }
}
