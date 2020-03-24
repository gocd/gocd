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
import {Material} from "models/materials/types";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import s from "underscore.string";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {AddOrEditEntityModal} from "./add_or_edit_modal";

export class MaterialModal extends AddOrEditEntityModal<Material> {
  private readonly pluginInfos: Stream<PluginInfos>      = Stream();
  private readonly packages: Stream<PackageRepositories> = Stream();

  constructor(title: string, entity: Stream<Material>, packages: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>, onSuccessfulAdd: (entity: Material) => void) {
    super(title, entity, onSuccessfulAdd);
    this.packages    = packages;
    this.pluginInfos = pluginInfos;
  }

  static forAdd(packages: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>, onSuccessfulAdd: (material: Material) => void) {
    return new MaterialModal("Add material", Stream(new Material("git")), packages, pluginInfos, onSuccessfulAdd);
  }

  static forEdit(material: Material, packages: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>, onSuccessfulAdd: (material: Material) => void) {
    const title          = `Edit material - ${s.capitalize(material.type()!)}`;
    const copyOfMaterial = Stream(new Material(material.type(), material.attributes()));
    return new MaterialModal(title, copyOfMaterial, packages, pluginInfos, onSuccessfulAdd);
  }

  body(): m.Children {
    return <MaterialEditor material={this.entity()} showExtraMaterials={true}
                           packages={this.packages()} pluginInfos={this.pluginInfos()}/>;
  }
}
