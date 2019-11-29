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

import {ApiResult, ErrorResponse, ObjectWithEtag} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {AuthConfigs} from "models/auth_configs/auth_configs";
import {GoCDAttributes, GoCDRole, PluginAttributes, PluginRole, Role, RoleType} from "models/roles/roles";
import {RolesCRUD} from "models/roles/roles_crud";
import {Configurations} from "models/shared/configuration";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {ButtonGroup} from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {OperationState} from "views/pages/page_operations";
import {Action, RoleModalBody} from "views/pages/roles/role_modal_body";

abstract class BaseRoleModal extends Modal {
  protected role: Stream<GoCDRole | PluginRole>;
  protected readonly pluginInfos: PluginInfos;
  protected readonly authConfigs: AuthConfigs;
  protected readonly onSuccessfulSave: (msg: m.Children) => any;
  protected readonly errorMessage: Stream<string> = Stream();
  protected resourceAutocompleteHelper: Map<string, string[]>;
  protected ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  constructor(role: GoCDRole | PluginRole,
              pluginInfos: PluginInfos,
              authConfigsOfInstalledPlugin: AuthConfigs,
              resourceAutocompleteHelper: Map<string, string[]>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(Size.large);
    this.role                       = Stream(role);
    this.pluginInfos                = pluginInfos;
    this.authConfigs                = authConfigsOfInstalledPlugin;
    this.resourceAutocompleteHelper = resourceAutocompleteHelper;
    this.onSuccessfulSave           = onSuccessfulSave;
  }

  abstract performSave(): Promise<any>;

  abstract modalTitle(role: GoCDRole | PluginRole): string;

  hasAuthConfigs() {
    return this.authConfigs.length === 0;
  }

  changeRoleType(roleType: RoleType) {
    let role: any;
    switch (roleType) {
      case RoleType.plugin:
        role = new PluginRole("", new PluginAttributes("", new Configurations([])), []);
        break;
      case RoleType.gocd:
        role = new GoCDRole("", new GoCDAttributes([]), []);
        break;
    }
    this.role = Stream(role);
  }

  validateAndPerformSave() {
    if (!this.role().isValid()) {
      return Promise.resolve();
    }
    return this.performSave()
        .then((result) => {
          result.do(
            () => {
              this.onSuccessfulSave(this.successMessage());
              this.close();
            },
            (errorResponse: any) => {
              this.showErrors(result, errorResponse);
            }
          );
        });
  }

  showErrors(apiResult: ApiResult<ObjectWithEtag<GoCDRole | PluginRole>>, errorResponse: ErrorResponse) {
    if (apiResult.getStatusCode() === 422 && errorResponse.body) {
      this.role(Role.fromJSON(JSON.parse(errorResponse.body).data));
    } else {
      this.errorMessage(JSON.parse(errorResponse.body!).message);
    }
  }

  title() {
    return this.modalTitle(this.role());
  }

  protected abstract successMessage(): m.Children;
}

export class NewRoleModal extends BaseRoleModal {
  constructor(role: GoCDRole | PluginRole,
              pluginInfos: PluginInfos,
              authConfigsOfInstalledPlugin: AuthConfigs,
              resourceAutocompleteHelper: Map<string, string[]>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(role,
          pluginInfos,
          authConfigsOfInstalledPlugin,
          resourceAutocompleteHelper,
          onSuccessfulSave);
  }

  modalTitle(role: GoCDRole | PluginRole): string {
    return "Add a new role";
  }

  performSave(): Promise<any> {
    return RolesCRUD.create(this.role());
  }

  body() {
    return <RoleModalBody role={this.role}
                          action={Action.NEW}
                          message={this.errorMessage()}
                          pluginInfos={this.pluginInfos}
                          authConfigs={this.authConfigs}
                          changeRoleType={this.changeRoleType.bind(this)}
                          resourceAutocompleteHelper={this.resourceAutocompleteHelper}/>;
  }

  buttons() {
    return [<ButtonGroup>
      <Buttons.Cancel data-test-id="button-cancel" ajaxOperationMonitor={this.ajaxOperationMonitor}
                      onclick={(e) => this.close()}>Cancel</Buttons.Cancel>
      <Buttons.Primary data-test-id="button-save"
                       ajaxOperationMonitor={this.ajaxOperationMonitor}
                       ajaxOperation={this.validateAndPerformSave.bind(this)}>Save</Buttons.Primary>
    </ButtonGroup>];
  }

  protected successMessage() {
    return <span>The role <em>{this.role().name()}</em> was created successfully!</span>;
  }

}

abstract class ModalWithFetch extends BaseRoleModal {
  protected etag: Stream<string> = Stream();
  protected isStale              = Stream(true);

  constructor(role: GoCDRole | PluginRole,
              pluginInfos: PluginInfos,
              authConfigsOfInstalledPlugin: AuthConfigs,
              resourceAutocompleteHelper: Map<string, string[]>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(role, pluginInfos, authConfigsOfInstalledPlugin, resourceAutocompleteHelper, onSuccessfulSave);
  }

  render() {
    super.render();
    this.performFetch(this.role().name())
        .then((result) => {
          result.do((successResponse: any) => {
                      this.role(successResponse.body.object);
                      this.etag(successResponse.body.etag);
                      this.isStale(false);
                      this.fetchCompleted();
                    },
                    (errorResponse: any) => {
                      this.showErrors(result, errorResponse);
                    }
          );
        });
  }

  buttons() {
    return [<ButtonGroup>
      <Buttons.Cancel data-test-id="button-cancel" ajaxOperationMonitor={this.ajaxOperationMonitor}
                      onclick={(e) => this.close()}>Cancel</Buttons.Cancel>
      <Buttons.Primary data-test-id="button-save"
                       ajaxOperationMonitor={this.ajaxOperationMonitor}
                       ajaxOperation={this.validateAndPerformSave.bind(this)}
                       disabled={this.isStale()}>Save</Buttons.Primary>
    </ButtonGroup>];
  }

  protected abstract performFetch(entityId: string): Promise<any>;

  protected fetchCompleted() {
    //implement when needed
  }
}

export class EditRoleModal extends ModalWithFetch {
  constructor(role: GoCDRole | PluginRole,
              pluginInfos: PluginInfos,
              authConfigsOfInstalledPlugin: AuthConfigs,
              resourceAutocompleteHelper: Map<string, string[]>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(role, pluginInfos, authConfigsOfInstalledPlugin, resourceAutocompleteHelper, onSuccessfulSave);
  }

  modalTitle(role: GoCDRole | PluginRole): string {
    return `Edit role ${role.name()}`;
  }

  performSave(): Promise<any> {
    return RolesCRUD.update(this.role(), this.etag());
  }

  body() {
    return <RoleModalBody role={this.role}
                          action={Action.EDIT}
                          message={this.errorMessage()}
                          pluginInfos={this.pluginInfos}
                          authConfigs={this.authConfigs}
                          isStale={this.isStale}
                          resourceAutocompleteHelper={this.resourceAutocompleteHelper}/>;
  }

  protected successMessage(): m.Children {
    return <span>The role <em>{this.role().name()}</em> was updated successfully!</span>;
  }

  protected performFetch(entityId: string): Promise<any> {
    return RolesCRUD.get(entityId);
  }

}

export class CloneRoleModal extends ModalWithFetch {
  private readonly originalRoleName: string;

  constructor(role: GoCDRole | PluginRole,
              pluginInfos: PluginInfos,
              authConfigsOfInstalledPlugin: AuthConfigs,
              resourceAutocompleteHelper: Map<string, string[]>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(role, pluginInfos, authConfigsOfInstalledPlugin, resourceAutocompleteHelper, onSuccessfulSave);
    this.originalRoleName = role.name();
  }

  modalTitle(role: GoCDRole | PluginRole): string {
    return `Clone role ${this.originalRoleName}`;
  }

  performSave(): Promise<any> {
    return RolesCRUD.create(this.role());
  }

  body() {
    return <RoleModalBody role={this.role}
                          action={Action.CLONE}
                          message={this.errorMessage()}
                          pluginInfos={this.pluginInfos}
                          authConfigs={this.authConfigs}
                          isStale={this.isStale}
                          resourceAutocompleteHelper={this.resourceAutocompleteHelper}/>;
  }

  protected fetchCompleted() {
    this.role().name("");
  }

  protected successMessage(): m.Children {
    return <span>The role <em>{this.role().name()}</em> was created successfully!</span>;
  }

  protected performFetch(entityId: string): Promise<any> {
    return RolesCRUD.get(entityId);
  }
}

export class DeleteRoleConfirmModal extends DeleteConfirmModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly onOperationError: (errorResponse: ErrorResponse) => any;

  constructor(role: GoCDRole | PluginRole,
              onSuccessfulSave: (msg: m.Children) => any,
              onOperationError: (errorResponse: ErrorResponse) => any) {
    super(DeleteRoleConfirmModal.deleteConfirmationMessage(role),
          () => this.delete(role), "Are you sure?");
    this.onSuccessfulSave = onSuccessfulSave;
    this.onOperationError = onOperationError;
  }

  private static deleteConfirmationMessage(role: GoCDRole | PluginRole) {
    return <span>
          Are you sure you want to delete the role <strong>{role.name()}</strong>?
        </span>;
  }

  private delete(obj: GoCDRole | PluginRole) {
    return RolesCRUD
      .delete(obj.name())
      .then((result) => {
        result.do(
          () => this.onSuccessfulSave(
            <span>The role <em>{obj.name()}</em> was deleted successfully!</span>
          ),
          this.onOperationError
        );
      })
      .finally(this.close.bind(this));
  }
}
