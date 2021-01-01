/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {Materials} from "models/pipeline_configs/pipeline_config";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import s from "underscore.string";
import {Cancel, Primary} from "views/components/buttons";
import {FlashMessage, FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import styles from "./material_modal.scss";

export class MaterialModal extends Modal {
  private readonly materials: Stream<Materials>;
  protected readonly originalEntity: Stream<Material>;
  protected readonly entity: Stream<Material>;
  private readonly __title: string;
  private readonly errorMessage: Stream<m.Children>;
  private readonly pluginInfos: Stream<PluginInfos>;
  private readonly packageRepositories: Stream<PackageRepositories>;
  private readonly pluggableScms: Stream<Scms>;
  private readonly pipelineConfigSave: () => Promise<any>;
  private readonly isNew: boolean;
  private readonly readonly: boolean;
  private readonly parentPipelineName: string;
  private readonly pipelineGroupName: string;
  private parentFlashMessage: FlashMessageModelWithTimeout;

  constructor(title: string, entity: Stream<Material>, pipelineGroupName: string, parentPipelineName: string, materials: Stream<Materials>, scms: Stream<Scms>,
              packages: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>,
              pipelineConfigSave: () => Promise<any>, parentFlashMessage: FlashMessageModelWithTimeout, isNew: boolean, readonly: boolean) {
    super(Size.medium);
    this.__title                  = title;
    this.originalEntity           = entity;
    this.entity                   = Stream(entity().clone());
    this.pluggableScms            = scms;
    this.materials                = materials;
    this.packageRepositories      = packages;
    this.pluginInfos              = pluginInfos;
    this.pipelineConfigSave       = pipelineConfigSave;
    this.isNew                    = isNew;
    this.errorMessage             = Stream();
    this.closeModalOnOverlayClick = false;
    this.readonly                 = readonly;
    this.parentPipelineName       = parentPipelineName;
    this.pipelineGroupName        = pipelineGroupName;
    this.parentFlashMessage       = parentFlashMessage;
  }

  static forAdd(pipelineGroupName: string, parentPipelineName: string, materials: Stream<Materials>, scms: Stream<Scms>, packageRepositories: Stream<PackageRepositories>,
                pluginInfos: Stream<PluginInfos>, onSuccessfulAdd: () => Promise<any>, parentFlashMessage: FlashMessageModelWithTimeout) {
    const materialType = materials().scmMaterialsHaveDestination() ? "git" : "dependency";
    return new MaterialModal("Add material", Stream(new Material(materialType)), pipelineGroupName, parentPipelineName, materials, scms, packageRepositories, pluginInfos, onSuccessfulAdd, parentFlashMessage, true, false);
  }

  static forEdit(material: Material, pipelineGroupName: string, parentPipelineName: string, materials: Stream<Materials>, scms: Stream<Scms>,
                 packageRepositories: Stream<PackageRepositories>, pluginInfos: Stream<PluginInfos>,
                 pipelineConfigSave: () => Promise<any>, parentFlashMessage: FlashMessageModelWithTimeout, readonly: boolean) {
    const title = `Edit material - ${s.capitalize(material.type()!)}`;
    return new MaterialModal(title, Stream(material), pipelineGroupName, parentPipelineName, materials, scms, packageRepositories, pluginInfos, pipelineConfigSave, parentFlashMessage, false, readonly);
  }

  title(): string {
    return this.__title;
  }

  body(): m.Children {
    const allScmMaterialsHaveDestination = this.materials().scmMaterialsHaveDestination();
    return <div>
      <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>
      <div className={styles.materialInputWrapper}>
        <MaterialEditor material={this.entity()} showExtraMaterials={true}
                        disableScmMaterials={this.isNew && !allScmMaterialsHaveDestination} readonly={this.readonly}
                        parentPipelineName={this.parentPipelineName} disabledMaterialTypeSelection={!this.isNew}
                        pipelineGroupName={this.pipelineGroupName} pluggableScms={this.pluggableScms()}
                        packageRepositories={this.packageRepositories()} pluginInfos={this.pluginInfos()}/>
      </div>
    </div>;
  }

  buttons(): m.ChildArray {
    if (this.readonly) {
      return [];
    }

    return [
      <Primary data-test-id="button-save" onclick={this.addOrUpdateEntity.bind(this)}>Save</Primary>,
      <Cancel data-test-id="button-cancel" onclick={this.close.bind(this)}>Cancel</Cancel>
    ];
  }

  addOrUpdateEntity(): void {
    if (this.entity().isValid()) {
      if (this.isNew) {
        this.materials().push(this.entity());
      } else {
        //remove the original one and save the updated one
        this.materials().splice(this.materials().indexOf(this.originalEntity()), 1, this.entity());
      }

      this.pipelineConfigSave()
          .then(() => this.close())
          .catch((errorResponse?: string) => {
            if (errorResponse) {
              const parse            = JSON.parse(JSON.parse(errorResponse).body);
              const unconsumedErrors = this.entity().consumeErrorsResponse(parse.data);
              this.errorMessage(<span>{parse.message}<br/> {unconsumedErrors.allErrorsForDisplay()}</span>);
              this.parentFlashMessage.clear();
            }

            if (this.isNew) {
              this.materials().delete(this.entity());
            } else {
              this.materials().splice(this.materials().indexOf(this.entity()), 1, this.originalEntity());
            }
          });
    }
  }

  close() {
    this.entity().resetPasswordIfAny();
    super.close();
  }
}
