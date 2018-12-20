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

import {bind} from "classnames/bind";
import {ApiResult, ErrorResponse, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import _ = require("lodash");
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ArtifactStoresCRUD} from "models/artifact_stores/artifact_stores_crud";
import {ArtifactStore, ArtifactStoreJSON} from "models/artifact_stores/artifact_stores_new";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Configurations} from "models/shared/configuration";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {ButtonGroup} from "views/components/buttons";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import {ArtifactStoreModalBody} from "views/pages/artifact_stores/artifact_store_modal_body";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const foundationClassNames = bind(foundationStyles);

abstract class BaseModal<T extends ValidatableMixin> extends Modal {
  protected entity: Stream<T>;
  protected readonly pluginInfos: Array<PluginInfo<Extension>>;
  protected readonly errorMessage: Stream<string> = stream();
  protected readonly onSuccessfulSave: (msg: m.Children) => any;
  protected readonly isStale: Stream<boolean>     = stream(true);
  protected readonly etag: Stream<string>         = stream();

  constructor(entity: T, pluginInfos: Array<PluginInfo<any>>, onSuccessfulSave: (msg: m.Children) => any) {
    super(Size.large);
    this.entity           = stream(entity);
    this.pluginInfos      = pluginInfos;
    this.onSuccessfulSave = onSuccessfulSave;
  }

  render(): void {
    super.render();

    if (this.isStale()) {
      this.performFetch(this.entity()).then(this.onFetchResult);
    }
  }

  validateAndSave() {
    if (this.isStale()) {
      //TODO: Update error, Disable save button
      return;
    }

    if (!this.entity().isValid()) {
      return;
    }

    this.savePromise().then(this.onSaveResult);
  }

  body(): JSX.Element {
    if (this.errorMessage()) {
      return (<FlashMessage type={MessageType.alert} message={this.errorMessage()}/>);
    }

    if (!this.entity || this.isStale()) {
      return <Spinner/>;
    }

    return (
      <div className={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        {this.modalBody()}
      </div>
    );
  }

  buttons() {
    return [
      <ButtonGroup>
        <Buttons.Cancel data-test-id="button-cancel" onclick={(e) => this.close()}>Cancel</Buttons.Cancel>
        <Buttons.Primary data-test-id="button-save"
                         disabled={this.isStale()}
                         onclick={this.validateAndSave.bind(this)}>Save</Buttons.Primary>
      </ButtonGroup>
    ];
  }

  protected abstract onPluginChange(entity: Stream<T>, pluginInfo: PluginInfo<any>): void;

  protected abstract savePromise(): Promise<any>;

  protected abstract successMessage(): m.Children;

  protected abstract performFetch(entity: T): Promise<any>;

  protected abstract modalBody(): m.Children;

  protected abstract parseJsonToEntity(json: string): T;

  protected pluginIdProxy(newPluginId?: string) {
    if (!newPluginId) {
      return newPluginId;
    }

    //@ts-ignore
    if (newPluginId !== this.entity().pluginId()) {
      const pluginInfo = _.find(this.pluginInfos, (pluginInfo) => pluginInfo.id === newPluginId) as PluginInfo<any>;
      this.onPluginChange(this.entity, pluginInfo);
    }
    return newPluginId;
  }

  // noinspection TsLint
  private onFetchResult = (result: ApiResult<ObjectWithEtag<T>>) => {
    result.do(this.onSuccessfulFetch, (e) => this.onError(e, result.getStatusCode()));
  };

  // noinspection TsLint
  private onSuccessfulFetch = (successResponse: SuccessResponse<ObjectWithEtag<T>>) => {
    this.entity(successResponse.body.object);
    this.etag(successResponse.body.etag);
    this.isStale(false);
  };

  // noinspection TsLint
  private onSaveResult = (result: ApiResult<ObjectWithEtag<T>>) => {
    result.do(this.onSuccess, (e) => this.onError(e, result.getStatusCode()));
  };

  // noinspection TsLint
  private onSuccess = (successResponse: SuccessResponse<ObjectWithEtag<T>>) => {
    this.onSuccessfulSave(this.successMessage());
    this.close();
  };

  // noinspection TsLint
  private onError = (errorResponse: ErrorResponse, statusCode: number) => {
    this.errorMessage(errorResponse.message);
    if (422 === statusCode && errorResponse.body) {
      this.entity(this.parseJsonToEntity(errorResponse.body));
    }
  };
}

abstract class ArtifactStoreModal extends BaseModal<ArtifactStore> {
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
