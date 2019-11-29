/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {ApiResult} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {AuthConfig, AuthConfigJSON} from "models/auth_configs/auth_configs";
import {AuthConfigsCRUD} from "models/auth_configs/auth_configs_crud";
import {Configurations} from "models/shared/configuration";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {ButtonGroup} from "views/components/buttons";
import {MessageType} from "views/components/flash_message";
import {Size} from "views/components/modal";
import {EntityModal} from "views/components/modal/entity_modal";
import {AuthConfigModalBody} from "views/pages/auth_configs/auth_config_modal_body";
import {Message} from "views/pages/maintenance_mode";

abstract class AuthConfigModal extends EntityModal<AuthConfig> {
  protected readonly originalEntityId: string;
  private disableId: boolean;
  private message: Stream<Message> = Stream();

  constructor(entity: AuthConfig,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any,
              disableId: boolean = false,
              size: Size         = Size.large) {
    super(entity, pluginInfos, onSuccessfulSave, size);
    this.disableId        = disableId;
    this.originalEntityId = entity.id()!;
  }

  performCheckConnection() {
    if (!this.entity().isValid()) {
      return Promise.resolve();
    }

    return AuthConfigsCRUD.verifyConnection(this.entity()).then(this.onVerifyConnectionResult.bind(this));
  }

  onPluginChange(entity: Stream<AuthConfig>, pluginInfo: PluginInfo): void {
    entity(new AuthConfig(entity().id(),
                          pluginInfo!.id,
                          entity().allowOnlyKnownUsersToLogin(),
                          new Configurations([])));
  }

  buttons() {
    return [
      <ButtonGroup>
        <Buttons.Primary data-test-id="button-check-connection"
                         ajaxOperationMonitor={this.ajaxOperationMonitor}
                         ajaxOperation={this.performCheckConnection.bind(this)}>Check connection</Buttons.Primary>
        <Buttons.Primary data-test-id="button-save"
                         disabled={this.isStale()}
                         ajaxOperationMonitor={this.ajaxOperationMonitor}
                         ajaxOperation={this.performOperation.bind(this)}>Save</Buttons.Primary>
      </ButtonGroup>
    ];
  }

  protected performFetch(entity: AuthConfig): Promise<any> {
    return AuthConfigsCRUD.get(this.originalEntityId);
  }

  protected parseJsonToEntity(json: object) {
    return AuthConfig.fromJSON(json as AuthConfigJSON);
  }

  protected modalBody(): m.Children {
    return (<div>
      <AuthConfigModalBody
        message={this.message()}
        pluginInfos={this.pluginInfos}
        authConfig={this.entity()}
        disableId={this.disableId}
        pluginIdProxy={this.pluginIdProxy.bind(this)}/>
    </div>);
  }

  private onVerifyConnectionResult(result: ApiResult<any>) {
    result.do(this.onVerifyConnectionSuccess.bind(this),
              (e) => this.onVerifyConnectionError(e, result.getStatusCode()));
  }

  private onVerifyConnectionSuccess(successResponse: any) {
    this.message(new Message(MessageType.success, successResponse.body.message));
    this.entity(this.parseJsonToEntity(successResponse.body.auth_config));
  }

  private onVerifyConnectionError(errorResponse: any, statusCode: number) {
    if (422 === statusCode && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      this.message(new Message(MessageType.alert, json.message));
      this.entity(this.parseJsonToEntity(json.auth_config));
    } else {
      this.errorMessage(JSON.parse(errorResponse.body!).message);
    }
  }
}

export class CreateAuthConfigModal extends AuthConfigModal {
  constructor(entity: AuthConfig,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave);
    this.isStale(false);
  }

  title(): string {
    return "Create a new authorization configuration";
  }

  operationPromise(): Promise<any> {
    return AuthConfigsCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The authorization configuration <em>{this.entity().id()}</em> was created successfully!</span>;
  }
}

export class EditAuthConfigModal extends AuthConfigModal {
  constructor(entity: AuthConfig, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, true);
  }

  title(): string {
    return `Edit authorization configuration ${this.entity().id()}`;
  }

  operationPromise(): Promise<any> {
    return AuthConfigsCRUD.update(this.entity(), this.etag());
  }

  successMessage(): m.Children {
    return <span>The authorization configuration <em>{this.entity().id()}</em> was updated successfully!</span>;
  }
}

export class CloneAuthConfigModal extends AuthConfigModal {
  constructor(entity: AuthConfig, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, false);
  }

  title(): string {
    return `Clone authorization configuration ${this.originalEntityId}`;
  }

  operationPromise(): Promise<any> {
    return AuthConfigsCRUD.create(this.entity());
  }

  successMessage(): m.Children {
    return <span>The authorization configuration <em>{this.entity().id()}</em> was created successfully!</span>;
  }

  fetchCompleted() {
    this.entity().id("");
  }
}

export class DeleteAuthConfigModal extends AuthConfigModal {
  private setMessage: (msg: m.Children, type: MessageType) => void;

  constructor(entity: AuthConfig, pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any,
              setMessage: (msg: m.Children, type: MessageType) => void) {
    super(entity, pluginInfos, onSuccessfulSave, true, Size.small);
    this.setMessage = setMessage;
    this.isStale(false);
  }

  title(): string {
    return "Are you sure?";
  }

  buttons(): any[] {
    return [
      <Buttons.Danger data-test-id="button-delete"
                      ajaxOperationMonitor={this.ajaxOperationMonitor}
                      ajaxOperation={this.performOperation.bind(this)}>Yes
        Delete</Buttons.Danger>,
      <Buttons.Cancel data-test-id="button-no-delete"
                      ajaxOperationMonitor={this.ajaxOperationMonitor}
                      onclick={this.close.bind(this)}>No</Buttons.Cancel>
    ];
  }

  operationError(errorResponse: any, statusCode: number) {
    const json = (errorResponse.body) ? JSON.parse(errorResponse.body) : errorResponse;
    this.setMessage(json.message, MessageType.alert);
    this.close();
  }

  protected modalBody(): m.Children {
    return (
      <span>
      Are you sure you want to delete the authorization configuration <strong>{this.originalEntityId}</strong>?
        </span>
    );
  }

  protected operationPromise(): Promise<any> {
    return AuthConfigsCRUD.delete(this.originalEntityId);
  }

  protected successMessage(): m.Children {
    return <span>The authorization configuration <em>{this.originalEntityId}</em> was deleted successfully!</span>;
  }
}
