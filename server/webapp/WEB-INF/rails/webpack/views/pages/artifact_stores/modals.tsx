/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ArtifactStoresCRUD} from "models/artifact_stores/artifact_stores_crud";
import {ArtifactStore, ArtifactStoreJSON} from "models/artifact_stores/artifact_stores_new";
import {Configurations} from "models/shared/configuration";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {EntityModal} from "views/components/modal/entity_modal";
import {ArtifactStoreModalBody} from "views/pages/artifact_stores/artifact_store_modal_body";

abstract class ArtifactStoreModal extends EntityModal<ArtifactStore> {
  private disableId: boolean;

  constructor(entity: ArtifactStore,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any,
              disableId: boolean = false) {
    super(entity, pluginInfos, onSuccessfulSave);
    this.disableId = disableId;
  }

  onPluginChange(entity: Stream<ArtifactStore>, pluginInfo: PluginInfo<any>): void {
    entity(new ArtifactStore(entity().id(), pluginInfo!.id, new Configurations([])));
  }

  protected performFetch(entity: ArtifactStore): Promise<any> {
    return ArtifactStoresCRUD.get(entity.id());
  }

  protected parseJsonToEntity(json: string) {
    const artifactStoreJSON = JSON.parse(json) as ArtifactStoreJSON;
    return ArtifactStore.fromJSON(artifactStoreJSON);
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
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave);
    this.isStale(false);
  }

  title(): string {
    return "Create a new artifact store";
  }

  savePromise(): Promise<any> {
    return ArtifactStoresCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The artifact store <em>{this.entity().id()}</em> was created successfully!</span>;
  }
}

export class EditArtifactStoreModal extends ArtifactStoreModal {
  constructor(entity: ArtifactStore, pluginInfos: Array<PluginInfo<any>>, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, true);
  }

  title(): string {
    return `Edit artifact store ${this.entity().id()}`;
  }

  savePromise(): Promise<any> {
    return ArtifactStoresCRUD.update(this.entity(), this.etag());
  }

  successMessage(): m.Children {
    return <span>The artifact store <em>{this.entity().id()}</em> was updated successfully!</span>;
  }
}

export class CloneArtifactStoreModal extends ArtifactStoreModal {
  private readonly entityId: string;

  constructor(entity: ArtifactStore, pluginInfos: Array<PluginInfo<any>>, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, true);
    this.entityId = entity.id();
  }

  title(): string {
    return `Clone artifact store ${this.entityId}`;
  }

  savePromise(): Promise<any> {
    return ArtifactStoresCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The artifact store <em>{this.entity().id()}</em> was created successfully!</span>;
  }
}
