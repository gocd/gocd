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
import {Scms} from "models/materials/pluggable_scm";
import {Material, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {Materials} from "models/pipeline_configs/pipeline_config";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {Secondary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Delete, Edit, IconGroup} from "views/components/icons";
import {Table} from "views/components/table";
import style from "views/pages/clicky_pipeline_config/index.scss";
import {MaterialModal} from "views/pages/clicky_pipeline_config/modal/material_modal";

interface MaterialsAttrs {
  materials: Stream<Materials>;
  pluginInfos: Stream<PluginInfos>;
  packageRepositories: Stream<PackageRepositories>;
  packages: Stream<Packages>;
  scmMaterials: Stream<Scms>;
  onMaterialAdd: (material: Material) => Promise<any>;
}

export class MaterialsWidget extends MithrilViewComponent<MaterialsAttrs> {
  view(vnode: m.Vnode<MaterialsAttrs, this>): m.Children | void | null {
    const allErrors = vnode.attrs.materials()
                           .map((material) => material.allErrors())
                           .filter((errors) => errors.length > 0);
    const errorMsgs = allErrors.length === 0
      ? undefined
      : <FlashMessage type={MessageType.alert} message={allErrors}/>;
    return <div class={style.materialTab}>
      {errorMsgs}
      <Table headers={["Material Name", "Type", "Url", ""]} data={this.tableData(vnode)}/>
      <Secondary dataTestId={"add-material-button"} onclick={this.addMaterial.bind(this, vnode)}>
        Add Material
      </Secondary>
    </div>;
  }

  private addMaterial(vnode: m.Vnode<MaterialsAttrs, this>, e: MouseEvent) {
    e.stopPropagation();
    MaterialModal.forAdd(vnode.attrs.scmMaterials, vnode.attrs.packageRepositories, vnode.attrs.pluginInfos, vnode.attrs.onMaterialAdd).render();
  }

  private tableData(vnode: m.Vnode<MaterialsAttrs, this>) {
    return Array.from(vnode.attrs.materials().values()).map((material: Material) => {
      return [
        this.getMaterialDisplayName(material, vnode),
        material.typeForDisplay(),
        this.getMaterialUrlForDisplay(material, vnode),
        <IconGroup>
          <Edit data-test-id={"edit-material-button"}/>
          <Delete data-test-id={"delete-material-button"}/>
        </IconGroup>
      ];
    });
  }

  private getMaterialUrlForDisplay(material: Material, vnode: m.Vnode<MaterialsAttrs, this>) {
    const url = material.materialUrl();
    if (url.length === 0 && material.type() === "package") {
      const attrs   = material.attributes() as PackageMaterialAttributes;
      const pkgInfo = vnode.attrs.packages().find((pkg) => pkg.id() === attrs.ref())!;
      return `Repository: ${pkgInfo.packageRepo().name()} - Package: ${pkgInfo.name()} ${pkgInfo.configuration().asString()}`;
    }
    if (url.length === 0 && material.type() === "plugin") {
      const attrs       = material.attributes() as PluggableScmMaterialAttributes;
      const scmMaterial = vnode.attrs.scmMaterials().find((pkg) => pkg.id() === attrs.ref())!;
      return `${scmMaterial.name()}: ${scmMaterial.configuration().asString()}`;
    }
    return url;
  }

  private getMaterialDisplayName(material: Material, vnode: m.Vnode<MaterialsAttrs, this>) {
    const displayName = material.displayName();
    if (displayName.length === 0 && material.type() === "package") {
      const attrs   = material.attributes() as PackageMaterialAttributes;
      const pkgInfo = vnode.attrs.packages().find((pkg) => pkg.id() === attrs.ref())!;
      return `${pkgInfo.packageRepo().name()}_${pkgInfo.name()}`;
    }
    if (displayName.length === 0 && material.type() === "plugin") {
      const attrs       = material.attributes() as PluggableScmMaterialAttributes;
      const scmMaterial = vnode.attrs.scmMaterials().find((pkg) => pkg.id() === attrs.ref())!;
      return scmMaterial.name();
    }
    return displayName;
  }
}
