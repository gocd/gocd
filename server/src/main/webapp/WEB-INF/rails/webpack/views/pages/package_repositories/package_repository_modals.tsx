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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PackageRepository} from "models/package_repositories/package_repositories";
import {PackageRepositoriesCRUD} from "models/package_repositories/package_repositories_crud";
import {PackageRepositoryJSON} from "models/package_repositories/package_repositories_json";
import {Configurations} from "models/shared/configuration";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {Size} from "views/components/modal";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {EntityModalWithCheckConnection} from "views/components/modal/entity_modal";
import {PackageRepositoryModalBody} from "./package_repository_modal_body";

abstract class PackageRepositoryModal extends EntityModalWithCheckConnection<PackageRepository> {
  protected readonly originalEntityId: string;
  protected readonly originalEntityName: string;
  private disableId: boolean;
  private disablePluginField: boolean;

  constructor(entity: PackageRepository,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any,
              disableId: boolean          = false,
              disablePluginField: boolean = true,
              size: Size                  = Size.large) {
    super(entity, pluginInfos, onSuccessfulSave, size);
    this.disableId          = disableId;
    this.disablePluginField = disablePluginField;
    this.originalEntityId   = entity.repoId();
    this.originalEntityName = entity.name();
    this.needsFoundationStyles(false);
  }

  operationError(errorResponse: any, statusCode: number) {
    if (errorResponse.data) {
      this.entity(this.parseJsonToEntity(errorResponse.data));
    } else {
      this.errorMessage(errorResponse.message);
    }
  }

  protected modalBody(): m.Children {
    return <PackageRepositoryModalBody pluginInfos={this.pluginInfos} packageRepo={this.entity()}
                                       disableId={this.disableId} disablePluginField={this.disablePluginField}
                                       pluginIdProxy={this.pluginIdProxy.bind(this)}/>;
  }

  protected onPluginChange(entity: Stream<PackageRepository>, pluginInfo: PluginInfo): void {
    const pluginMetadata = entity().pluginMetadata();
    if (pluginMetadata.id() !== pluginInfo.id) {
      pluginMetadata.id(pluginInfo.id);
      entity(new PackageRepository(entity().repoId(), entity().name(), pluginMetadata, new Configurations([]), entity().packages()));
    }
  }

  protected parseJsonToEntity(json: object): PackageRepository {
    return PackageRepository.fromJSON(json as PackageRepositoryJSON);
  }

  protected performFetch(entity: PackageRepository): Promise<any> {
    return PackageRepositoriesCRUD.get(this.originalEntityId);
  }

  protected pluginIdProxy(newPluginId?: string): any {
    if (!newPluginId) {
      return this.entity().pluginMetadata().id();
    }

    if (newPluginId !== this.entity().pluginMetadata().id()) {
      const pluginInfo = _.find(this.pluginInfos, (pluginInfo: PluginInfo) => pluginInfo.id === newPluginId) as PluginInfo;
      this.onPluginChange(this.entity, pluginInfo);
    }

    return newPluginId;
  }

  protected verifyConnectionOperationPromise(): Promise<any> {
    return PackageRepositoriesCRUD.verifyConnection(this.entity());
  }
}

export class CreatePackageRepositoryModal extends PackageRepositoryModal {

  constructor(entity: PackageRepository,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, false, false);
    this.isStale(false);
  }

  title(): string {
    return "Create a package repository";
  }

  protected operationPromise(): Promise<any> {
    return PackageRepositoriesCRUD.create(this.entity());
  }

  protected successMessage(): m.Children {
    return <span>The package repository <em>{this.entity().name()}</em> was created successfully!</span>;
  }
}

export class EditPackageRepositoryModal extends PackageRepositoryModal {
  constructor(entity: PackageRepository, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, true);
  }

  title(): string {
    return `Edit package repository ${this.entity().name()}`;
  }

  operationPromise(): Promise<any> {
    return PackageRepositoriesCRUD.update(this.entity(), this.etag());
  }

  successMessage(): m.Children {
    return <span>The package repository <em>{this.entity().name()}</em> was updated successfully!</span>;
  }
}

export class ClonePackageRepositoryModal extends PackageRepositoryModal {
  constructor(entity: PackageRepository, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave);
  }

  title(): string {
    return `Clone package repository ${this.originalEntityName}`;
  }

  operationPromise(): Promise<any> {
    return PackageRepositoriesCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The package repository <em>{this.entity().name()}</em> was created successfully!</span>;
  }

  fetchCompleted() {
    this.entity().repoId("");
    this.entity().name("");
  }
}

export class DeletePackageRepositoryModal extends DeleteConfirmModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;

  constructor(pkgRepo: PackageRepository, onSuccessfulSave: (msg: m.Children) => any) {
    super(DeletePackageRepositoryModal.deleteConfirmationMessage(pkgRepo),
          () => this.delete(pkgRepo), "Are you sure?");
    this.onSuccessfulSave = onSuccessfulSave;
  }

  private static deleteConfirmationMessage(pkgRepo: PackageRepository) {
    return <span>
          Are you sure you want to delete the package repository <strong>{pkgRepo.name()}</strong>?
        </span>;
  }

  private delete(obj: PackageRepository) {
    return PackageRepositoriesCRUD
      .delete(obj.repoId())
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(
              <span>The package repository <em>{obj.name()}</em> was deleted successfully!</span>
            );
            this.close();
          },
          (errorResponse: ErrorResponse) => {
            this.errorMessage = errorResponse.message;
            if (errorResponse.body) {
              this.errorMessage = JSON.parse(errorResponse.body).message;
            }
          }
        );
      });
  }
}
