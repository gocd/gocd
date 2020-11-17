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
import * as Buttons from "views/components/buttons";
import {ButtonGroup} from "views/components/buttons";
import {FlashMessageModel, MessageType} from "views/components/flash_message";
import {Warning} from "views/components/icons";
import testConnectionStyles from "views/components/materials/test_connection.scss";
import {Size} from "views/components/modal";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {EntityModal} from "views/components/modal/entity_modal";
import styles from "./index.scss";
import {PluggableScmModalBody} from "./pluggable_scm_modal_body";

abstract class PluggableScmModal extends EntityModal<Scm> {
  private static readonly TEST_CONNECTION_TEXT = "Check Connection";
  protected message: FlashMessageModel         = new FlashMessageModel();
  protected readonly originalEntityId: string;
  protected readonly originalEntityName: string;
  private readonly disableId: boolean;

  private readonly disablePluginId: boolean;
  private testConnectionButtonText: string = PluggableScmModal.TEST_CONNECTION_TEXT;
  private testConnectionButtonIcon: string | undefined;

  constructor(entity: Scm,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any,
              disableId: boolean       = false,
              disablePluginId: boolean = true,
              size: Size               = Size.large) {
    super(entity, pluginInfos, onSuccessfulSave, size);
    this.disableId          = disableId;
    this.disablePluginId    = disablePluginId;
    this.originalEntityId   = entity.id();
    this.originalEntityName = entity.name();
  }

  operationError(errorResponse: any, statusCode: number) {
    if (errorResponse.body) {
      this.errorMessage(JSON.parse(errorResponse.body).message);
    } else if (errorResponse.data) {
      this.entity(Scm.fromJSON(errorResponse.data));
    }
    this.errorMessage(errorResponse.message);
  }

  buttons(): any[] {
    return [
      <ButtonGroup>
        <Buttons.Primary data-test-id="button-check-connection"
                         ajaxOperationMonitor={this.ajaxOperationMonitor}
                         ajaxOperation={this.performCheckConnection.bind(this)}>
          <span className={this.testConnectionButtonIcon} data-test-id="test-connection-icon"/>
          {this.testConnectionButtonText}
        </Buttons.Primary>
        <Buttons.Primary data-test-id="button-save"
                         disabled={this.isLoading()}
                         ajaxOperationMonitor={this.ajaxOperationMonitor}
                         ajaxOperation={this.performOperation.bind(this)}>Save</Buttons.Primary>
        {this.saveFailureIdentifier}
      </ButtonGroup>,

      <div className={styles.alignLeft}>
        <Buttons.Cancel data-test-id="button-cancel" onclick={(e: MouseEvent) => this.close()}
                        ajaxOperationMonitor={this.ajaxOperationMonitor}>Cancel</Buttons.Cancel>
      </div>
    ];
  }

  protected modalBody(): m.Children {
    return <PluggableScmModalBody pluginInfos={this.pluginInfos} scm={this.entity()}
                                  disableId={this.disableId} disablePluginId={this.disablePluginId}
                                  pluginIdProxy={this.pluginIdProxy.bind(this)} message={this.message}/>;
  }

  protected onPluginChange(entity: Stream<Scm>, pluginInfo: PluginInfo): void {
    const pluginMetadata = entity().pluginMetadata();
    pluginMetadata.id(pluginInfo.id);
    entity(new Scm(entity().id(), entity().name(), entity().autoUpdate(), entity().origin(), pluginMetadata, entity().configuration()));
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

  protected performCheckConnection(): Promise<any> {
    if (!this.entity().isValid()) {
      return Promise.reject();
    }

    this.testConnectionInProgress();

    return PluggableScmCRUD.checkConnection(this.entity())
                           .then((success) => {
                             success.do((successResponse) => {
                               this.entity(Scm.fromJSON(successResponse.body.scm));
                               if (successResponse.body.status === "success") {
                                 this.testConnectionSuccessful();
                               } else {
                                 this.testConnectionFailed(successResponse.body.messages);
                               }
                             }, (errorResponse) => {
                               this.testConnectionButtonIcon = testConnectionStyles.testConnectionFailure;
                               this.message.setMessage(MessageType.alert, errorResponse.message);
                               if (errorResponse.body) {
                                 this.message.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                               }
                             });
                           })
                           .finally(() => this.testConnectionButtonText = PluggableScmModal.TEST_CONNECTION_TEXT);
  }

  private testConnectionInProgress() {
    this.testConnectionButtonIcon = undefined;
    this.testConnectionButtonText = "Testing Connection...";
  }

  private testConnectionSuccessful() {
    this.testConnectionButtonIcon = testConnectionStyles.testConnectionSuccess;
    this.message.setMessage(MessageType.success, "Connection Ok!");
  }

  private testConnectionFailed(messages: string[]) {
    this.testConnectionButtonIcon = testConnectionStyles.testConnectionFailure;
    const response                = <div>Check connection failed with the following errors: <br/>
      {messages.map((msg: string) => {
        return <span>&emsp; - {msg}</span>;
      })}</div>;
    this.message.setMessage(MessageType.alert, response);
  }
}

export class CreatePluggableScmModal extends PluggableScmModal {

  constructor(entity: Scm,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entity, pluginInfos, onSuccessfulSave, false, false);
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
    this.entity().id("");
    this.entity().name("");
  }
}

export class DeletePluggableScmModal extends DeleteConfirmModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;

  constructor(pkgRepo: Scm,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(DeletePluggableScmModal.deleteConfirmationMessage(pkgRepo),
          () => this.delete(pkgRepo), "Are you sure?");
    this.onSuccessfulSave = onSuccessfulSave;
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
          () => {
            this.onSuccessfulSave(
              <span>The scm <em>{obj.name()}</em> was deleted successfully!</span>
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
