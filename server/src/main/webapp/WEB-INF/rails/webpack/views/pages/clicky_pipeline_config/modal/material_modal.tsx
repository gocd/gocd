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
import {Material} from "models/materials/types";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import s from "underscore.string";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {Cancel, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal";
import {ErrorResponse} from "helpers/api_request_builder";

export class MaterialModal extends Modal {
  protected readonly entity: Stream<Material>;
  private readonly __title: string;
  private readonly errorMessage: Stream<string>;
  private readonly pluginInfos: Stream<PluginInfos>;
  private readonly packageRepositories: Stream<PackageRepositories>;
  private readonly pluggableScms: Stream<Scms>;
  private readonly onSuccessfulAdd: (material: Material) => Promise<any>;

  constructor(title: string, entity: Stream<Material>, scms: Stream<Scms>,
              packages: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>,
              onSuccessfulAdd: (material: Material) => Promise<any>) {
    super(Size.large);
    this.__title             = title;
    this.entity              = entity;
    this.pluggableScms       = scms;
    this.packageRepositories = packages;
    this.pluginInfos         = pluginInfos;
    this.onSuccessfulAdd     = onSuccessfulAdd;
    this.errorMessage        = Stream();
  }

  static forAdd(scms: Stream<Scms>, packageRepositories: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>,
                onSuccessfulAdd: (material: Material) => Promise<any>) {
    return new MaterialModal("Add material", Stream(new Material("git")), scms, packageRepositories, pluginInfos, onSuccessfulAdd);
  }

  static forEdit(material: Material, scms: Stream<Scms>,
                 packageRepositories: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>,
                 onSuccessfulAdd: (material: Material) => Promise<any>) {
    const title          = `Edit material - ${s.capitalize(material.type()!)}`;
    const copyOfMaterial = Stream(new Material(material.type(), material.attributes()));
    return new MaterialModal(title, copyOfMaterial, scms, packageRepositories, pluginInfos, onSuccessfulAdd);
  }

  title(): string {
    return this.__title;
  }

  body(): m.Children {
    return <div>
      <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>
      <MaterialEditor material={this.entity()} showExtraMaterials={true} pluggableScms={this.pluggableScms()}
                      packageRepositories={this.packageRepositories()} pluginInfos={this.pluginInfos()}/>
    </div>;
  }

  buttons(): m.ChildArray {
    return [
      <Primary data-test-id="button-save" onclick={this.addOrUpdateEntity.bind(this)}>Save</Primary>,
      <Cancel data-test-id="button-cancel" onclick={this.close.bind(this)}>Cancel</Cancel>
    ]
  }

  addOrUpdateEntity(): void {
    if (this.entity().isValid()) {
      this.onSuccessfulAdd(this.entity())
          .then(() => this.close())
          .catch((errorResponse: ErrorResponse) => {
            const parse = JSON.parse(errorResponse.body!);
            this.entity().consumeErrorsResponse(parse.data);
            this.errorMessage(parse.message);
          });
    }
  }
}
