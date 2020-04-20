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

import {ErrorResponse} from "helpers/api_request_builder";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Scms} from "models/materials/pluggable_scm";
import {Material, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {Materials} from "models/pipeline_configs/pipeline_config";
import {ScmExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {Secondary} from "views/components/buttons";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import {Delete, Edit, IconGroup} from "views/components/icons";
import {Table} from "views/components/table";
import style from "views/pages/clicky_pipeline_config/index.scss";
import {MaterialModal} from "views/pages/clicky_pipeline_config/modal/material_modal";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";

interface MaterialsAttrs {
  materials: Stream<Materials>;
  pluginInfos: Stream<PluginInfos>;
  packageRepositories: Stream<PackageRepositories>;
  packages: Stream<Packages>;
  scmMaterials: Stream<Scms>;
  pipelineConfigSave: () => Promise<any>;
  flashMessage: FlashMessageModelWithTimeout;
}

export class MaterialsWidget extends MithrilViewComponent<MaterialsAttrs> {
  view(vnode: m.Vnode<MaterialsAttrs, this>): m.Children | void | null {
    return <div class={style.materialTab}>
      <Table headers={["Material Name", "Type", "Url/Description", ""]} data={this.tableData(vnode)}/>
      <Secondary dataTestId={"add-material-button"} onclick={this.addMaterial.bind(this, vnode)}>
        Add Material
      </Secondary>
    </div>;
  }

  private addMaterial(vnode: m.Vnode<MaterialsAttrs, this>, e: MouseEvent) {
    e.stopPropagation();
    MaterialModal.forAdd(vnode.attrs.materials, vnode.attrs.scmMaterials, vnode.attrs.packageRepositories, vnode.attrs.pluginInfos, vnode.attrs.pipelineConfigSave).render();
  }

  private updateMaterial(vnode: m.Vnode<MaterialsAttrs, this>, materialToUpdate: Material, e: MouseEvent) {
    e.stopPropagation();
    MaterialModal.forEdit(materialToUpdate, vnode.attrs.materials, vnode.attrs.scmMaterials, vnode.attrs.packageRepositories, vnode.attrs.pluginInfos, vnode.attrs.pipelineConfigSave).render();
  }

  private deleteMaterial(vnode: m.Vnode<MaterialsAttrs, this>, materialToRemove: Material, e: MouseEvent) {
    e.stopPropagation();
    const onDelete = () => {
      vnode.attrs.materials().delete(materialToRemove);
      return vnode.attrs.pipelineConfigSave()
                  .then(() => vnode.attrs.flashMessage.setMessage(MessageType.success, `Material '${materialToRemove.name()}' deleted successfully.`))
                  .catch((errorResponse: ErrorResponse) => {
                    vnode.attrs.materials().push(materialToRemove);
                    vnode.attrs.flashMessage.consumeErrorResponse(errorResponse);
                  }).finally(m.redraw.sync);
    };
    new ConfirmationDialog(
      "Delete Material",
      <div>Do you want to delete the material '<em>{this.getMaterialDisplayInfo(materialToRemove, vnode).name}</em>'?</div>,
      onDelete
    ).render();
  }

  private tableData(vnode: m.Vnode<MaterialsAttrs, this>) {
    const deleteDisabled = vnode.attrs.materials().length === 1;
    const deleteTitle    = deleteDisabled
      ? "Cannot delete this material as pipeline should have at least one material"
      : "Remove this material";
    return Array.from(vnode.attrs.materials().values()).map((material: Material) => {
      const {name, type, urlOrDescription} = this.getMaterialDisplayInfo(material, vnode);
      return [
        name,
        type,
        urlOrDescription,
        <IconGroup>
          <Edit onclick={this.updateMaterial.bind(this, vnode, material)} data-test-id={"edit-material-button"}/>
          <Delete disabled={deleteDisabled} title={deleteTitle} onclick={this.deleteMaterial.bind(this, vnode, material)}
                  data-test-id={"delete-material-button"}/>
        </IconGroup>
      ];
    });
  }

  private getMaterialDisplayInfo(material: Material, vnode: m.Vnode<MaterialsAttrs, this>) {
    let typeForDisplay = material.typeForDisplay();
    let materialName;
    let urlOrDescription;
    switch (material.type()) {
      case "git":
      case "svn":
      case "hg":
      case "p4":
      case "tfs":
      case "dependency":
        materialName     = material.displayName();
        urlOrDescription = material.materialUrl();
        break;
      case "package":
        const pkgAttrs   = material.attributes() as PackageMaterialAttributes;
        const pkgInfo    = vnode.attrs.packages().find((pkg) => pkg.id() === pkgAttrs.ref())!;
        const pkgRepo    = vnode.attrs.packageRepositories().find((pkgRepo) => pkgRepo.repoId() === pkgInfo.packageRepo().id())!;
        const pkgPlugin  = vnode.attrs.pluginInfos().findByPluginId(pkgRepo.pluginMetadata().id());
        const pluginName = pkgPlugin === undefined
          ? <span className={style.missingPlugin}>Plugin '{pkgRepo.pluginMetadata().id()}' Missing!!!</span>
          : pkgPlugin.about.name;
        materialName     = `${pkgInfo.packageRepo().name()}_${pkgInfo.name()}`;
        urlOrDescription = <span>
          Plugin: {pluginName}<br/>
          Repository: {pkgInfo.packageRepo().name()} {pkgRepo.configuration().asString()}<br/>
          Package: {pkgInfo.name()} {pkgInfo.configuration().asString()}
          </span>;
        break;
      case "plugin":
        const pluginAttrs = material.attributes() as PluggableScmMaterialAttributes;
        const scmMaterial = vnode.attrs.scmMaterials().find((pkg) => pkg.id() === pluginAttrs.ref())!;
        const scmPlugin   = vnode.attrs.pluginInfos().findByPluginId(scmMaterial.pluginMetadata().id());

        materialName = scmMaterial.name();
        if (scmPlugin === undefined) {
          urlOrDescription = <span>
            <span className={style.missingPlugin}>Plugin '{scmMaterial.pluginMetadata().id()}' Missing!!!</span><br/>
            {scmMaterial.configuration().asString()}
            </span>;
        } else {
          typeForDisplay   = (scmPlugin.extensionOfType(ExtensionTypeString.SCM) as ScmExtension).displayName;
          urlOrDescription = scmMaterial.configuration().asString();
        }
        break;
    }
    return {
      name: materialName,
      type: typeForDisplay,
      urlOrDescription
    };
  }
}
