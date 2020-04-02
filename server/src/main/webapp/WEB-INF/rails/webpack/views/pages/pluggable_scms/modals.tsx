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
import {Scm, ScmJSON} from "models/materials/pluggable_scm";
import {PluggableScmCRUD} from "models/materials/pluggable_scm_crud";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {v4 as uuidv4} from 'uuid';
import {FlashMessageModel, MessageType} from "views/components/flash_message";
import {Warning} from "views/components/icons";
import {Size} from "views/components/modal";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {EntityModal} from "views/components/modal/entity_modal";
import {PluggableScmModalBody} from "./pluggable_scm_modal_body";

abstract class PluggableScmModal extends EntityModal<Scm> {
  protected readonly originalEntityId: string;
  protected readonly originalEntityName: string;
  protected message?: FlashMessageModel;
  private disableId: boolean;

  constructor(entity: Scm,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any,
              disableId: boolean = false,
              size: Size         = Size.large) {
    super(entity, pluginInfos, onSuccessfulSave, size);
    this.disableId          = disableId;
    this.originalEntityId   = entity.id();
    this.originalEntityName = entity.name();
  }

  operationError(errorResponse: any, statusCode: number) {
    this.errorMessage(errorResponse.message);
    if (errorResponse.body) {
      this.errorMessage(JSON.parse(errorResponse.body).message);
    }
  }

  protected modalBody(): m.Children {
    return <PluggableScmModalBody pluginInfos={this.pluginInfos}
                                  scm={this.entity()}
                                  disableId={this.disableId}
                                  pluginIdProxy={this.pluginIdProxy.bind(this)} message={this.message}/>;
  }

  protected onPluginChange(entity: Stream<Scm>, pluginInfo: PluginInfo): void {
    const pluginMetadata = entity().pluginMetadata();
    pluginMetadata.id(pluginInfo.id);
    entity(new Scm(entity().id(), entity().name(), entity().autoUpdate(), pluginMetadata, entity().configuration()));
  }

  protected parseJsonToEntity(json: object): Scm {
    return Scm.fromJSON(json as ScmJSON);
  }

  protected performFetch(entity: Scm): Promise<any> {
    return PluggableScmCRUD.get(this.originalEntityName);
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
}

export class CreatePluggableScmModal extends PluggableScmModal {

  constructor(entity: Scm,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave);
    this.isStale(false);
  }

  title(): string {
    return "Create a scm";
  }

  protected operationPromise(): Promise<any> {
    return PluggableScmCRUD.create(this.entity());
  }

  protected successMessage(): m.Children {
    return <span>The scm <em>{this.entity().name()}</em> was created successfully!</span>;
  }
}

export class EditPluggableScmModal extends PluggableScmModal {
  private readonly warningMsg = <span>
    <Warning iconOnly={true}/>This is a global copy. All pipelines using this SCM will be affected.
  </span>;

  constructor(entity: Scm, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, true);
    this.message = new FlashMessageModel(MessageType.warning, this.warningMsg);
  }

  title(): string {
    return `Edit scm ${this.entity().name()}`;
  }

  operationPromise(): Promise<any> {
    return PluggableScmCRUD.update(this.entity(), this.etag());
  }

  successMessage(): m.Children {
    return <span>The scm <em>{this.entity().name()}</em> was updated successfully!</span>;
  }
}

export class ClonePluggableScmModal extends PluggableScmModal {
  constructor(entity: Scm, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, false);
  }

  title(): string {
    return `Clone scm ${this.originalEntityName}`;
  }

  operationPromise(): Promise<any> {
    return PluggableScmCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The scm <em>{this.entity().name()}</em> was created successfully!</span>;
  }

  fetchCompleted() {
    this.entity().id(uuidv4());
    this.entity().name("");
  }
}

export class DeletePluggableScmModal extends DeleteConfirmModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly onOperationError: (errorResponse: ErrorResponse) => any;

  constructor(pkgRepo: Scm,
              onSuccessfulSave: (msg: m.Children) => any,
              onOperationError: (errorResponse: ErrorResponse) => any) {
    super(DeletePluggableScmModal.deleteConfirmationMessage(pkgRepo),
          () => this.delete(pkgRepo), "Are you sure?");
    this.onSuccessfulSave = onSuccessfulSave;
    this.onOperationError = onOperationError;
  }

  private static deleteConfirmationMessage(scm: Scm) {
    return <span>
          Are you sure you want to delete the scm <strong>{scm.name()}</strong>?
        </span>;
  }

  private delete(obj: Scm) {
    return PluggableScmCRUD
      .delete(obj.name())
      .then((result) => {
        result.do(
          () => this.onSuccessfulSave(
            <span>The scm <em>{obj.name()}</em> was deleted successfully!</span>
          ),
          this.onOperationError
        );
      })
      .finally(this.close.bind(this));
  }
}
