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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Scms} from "models/materials/pluggable_scm";
import {Material, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {Materials} from "models/pipeline_configs/pipeline_config";
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
      <Table headers={["Material Name", "Type", "Url", ""]} data={this.tableData(vnode)}/>
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
      <div>Do you want to delete the material '<em>{this.getMaterialDisplayName(materialToRemove, vnode)}</em>'?</div>,
      onDelete
    ).render();
  }

  private tableData(vnode: m.Vnode<MaterialsAttrs, this>) {
    const deleteDisabled = vnode.attrs.materials().length === 1;
    const deleteTitle    = deleteDisabled
      ? "Cannot delete this material as pipeline should have at least one material"
      : "Remove this material";
    return Array.from(vnode.attrs.materials().values()).map((material: Material) => {
      return [
        this.getMaterialDisplayName(material, vnode),
        material.typeForDisplay(),
        this.getMaterialUrlForDisplay(material, vnode),
        <IconGroup>
          <Edit onclick={this.updateMaterial.bind(this, vnode, material)} data-test-id={"edit-material-button"}/>
          <Delete disabled={deleteDisabled} title={deleteTitle} onclick={this.deleteMaterial.bind(this, vnode, material)}
                  data-test-id={"delete-material-button"}/>
        </IconGroup>
      ];
    });
  }

  private getMaterialUrlForDisplay(material: Material, vnode: m.Vnode<MaterialsAttrs, this>) {
    const url = material.materialUrl();
    if (url.length === 0 && material.type() === "package" && !_.isEmpty(vnode.attrs.packages())) {
      const attrs   = material.attributes() as PackageMaterialAttributes;
      const pkgInfo = vnode.attrs.packages().find((pkg) => pkg.id() === attrs.ref());
      return pkgInfo === undefined ? "" : `Repository: ${pkgInfo.packageRepo().name()} - Package: ${pkgInfo.name()} ${pkgInfo.configuration().asString()}`;
    }
    if (url.length === 0 && material.type() === "plugin" && !_.isEmpty(vnode.attrs.scmMaterials())) {
      const attrs       = material.attributes() as PluggableScmMaterialAttributes;
      const scmMaterial = vnode.attrs.scmMaterials().find((pkg) => pkg.id() === attrs.ref());
      return scmMaterial === undefined ? "" : `${scmMaterial.name()}: ${scmMaterial.configuration().asString()}`;
    }
    return url;
  }

  private getMaterialDisplayName(material: Material, vnode: m.Vnode<MaterialsAttrs, this>) {
    const displayName = material.displayName();
    if (displayName.length === 0 && material.type() === "package" && !_.isEmpty(vnode.attrs.packages())) {
      const attrs   = material.attributes() as PackageMaterialAttributes;
      const pkgInfo = vnode.attrs.packages().find((pkg) => pkg.id() === attrs.ref());
      return pkgInfo === undefined ? "" : `${pkgInfo.packageRepo().name()}_${pkgInfo.name()}`;
    }
    if (displayName.length === 0 && material.type() === "plugin" && !_.isEmpty(vnode.attrs.scmMaterials())) {
      const attrs       = material.attributes() as PluggableScmMaterialAttributes;
      const scmMaterial = vnode.attrs.scmMaterials().find((pkg) => pkg.id() === attrs.ref());
      return scmMaterial === undefined ? "" : scmMaterial.name();
    }
    return displayName;
  }
}
