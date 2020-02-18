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
import {ArtifactStore, ArtifactStoreJSON} from "models/artifact_stores/artifact_stores";
import {ArtifactStoresCRUD} from "models/artifact_stores/artifact_stores_crud";
import {Configurations} from "models/shared/configuration";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {MessageType} from "views/components/flash_message";
import {Size} from "views/components/modal";
import {EntityModal} from "views/components/modal/entity_modal";
import {ArtifactStoreModalBody} from "views/pages/artifact_stores/artifact_store_modal_body";

abstract class ArtifactStoreModal extends EntityModal<ArtifactStore> {
  private readonly disableId: boolean;
  protected readonly originalEntityId: string;

  protected constructor(entity: ArtifactStore,
                        pluginInfos: PluginInfos,
                        onSuccessfulSave: (msg: m.Children) => any,
                        disableId: boolean = false,
                        size: Size         = Size.large) {
    super(entity, pluginInfos, onSuccessfulSave, size);
    this.disableId        = disableId;
    this.originalEntityId = entity.id();
  }

  onPluginChange(entity: Stream<ArtifactStore>, pluginInfo: PluginInfo): void {
    entity(new ArtifactStore(entity().id(), pluginInfo!.id, new Configurations([])));
  }

  protected performFetch(entity: ArtifactStore): Promise<any> {
    return ArtifactStoresCRUD.get(this.originalEntityId);
  }

  protected parseJsonToEntity(json: object) {
    return ArtifactStore.fromJSON(json as ArtifactStoreJSON);
  }

  protected modalBody(): m.Children {
    return <ArtifactStoreModalBody
      pluginInfos={this.pluginInfos}
      artifactStore={this.entity()}
      disableId={this.disableId}
      pluginIdProxy={this.pluginIdProxy.bind(this)}
    />;
  }
}

export class CreateArtifactStoreModal extends ArtifactStoreModal {
  constructor(entity: ArtifactStore,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave);
    this.isStale(false);
  }

  title(): string {
    return "Create a new artifact store";
  }

  operationPromise(): Promise<any> {
    return ArtifactStoresCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The artifact store <em>{this.entity().id()}</em> was created successfully!</span>;
  }
}

export class EditArtifactStoreModal extends ArtifactStoreModal {
  constructor(entity: ArtifactStore, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, true);
  }

  title(): string {
    return `Edit artifact store ${this.entity().id()}`;
  }

  operationPromise(): Promise<any> {
    return ArtifactStoresCRUD.update(this.entity(), this.etag());
  }

  successMessage(): m.Children {
    return <span>The artifact store <em>{this.entity().id()}</em> was updated successfully!</span>;
  }
}

export class CloneArtifactStoreModal extends ArtifactStoreModal {
  constructor(entity: ArtifactStore, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, false);
  }

  title(): string {
    return `Clone artifact store ${this.originalEntityId}`;
  }

  operationPromise(): Promise<any> {
    return ArtifactStoresCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The artifact store <em>{this.entity().id()}</em> was created successfully!</span>;
  }

  fetchCompleted() {
    this.entity().id("");
  }
}

export class DeleteArtifactStoreModal extends ArtifactStoreModal {
  protected readonly setMessage: (type: MessageType, msg: m.Children) => void;

  constructor(entity: ArtifactStore, pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any,
              setMessage: (type: MessageType, msg: m.Children) => void) {
    super(entity, pluginInfos, onSuccessfulSave, true, Size.small);
    this.setMessage = setMessage;
    this.isStale(false);
  }

  title(): string {
    return "Are you sure?";
  }

  buttons(): any[] {
    return [
      <Buttons.Danger data-test-id="button-delete" onclick={this.performOperation.bind(this)}>Yes
        Delete</Buttons.Danger>,
      <Buttons.Cancel data-test-id="button-no-delete" onclick={this.close.bind(this)}>No</Buttons.Cancel>
    ];
  }

  operationError(errorResponse: any, statusCode: number) {
    const json = JSON.parse(errorResponse.body);
    this.setMessage(MessageType.alert, json.message);
    this.close();
  }

  protected modalBody(): m.Children {
    return (
      <span>
      Are you sure you want to delete the authorization configuration <strong>{this.entity().id()}</strong>?
      </span>
    );
  }

  protected operationPromise(): Promise<any> {
    return ArtifactStoresCRUD.delete(this.originalEntityId);
  }

  protected successMessage(): m.Children {
    return <span>The artifact store <em>{this.entity().id()}</em> was deleted successfully!</span>;
  }
}
