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

import {ErrorResponse} from "helpers/api_request_builder";
import * as _ from "lodash";
import * as m from "mithril";
import {AuthConfigsCRUD} from "models/auth_configs/auth_configs_crud";
import {AuthConfigs} from "models/auth_configs/auth_configs";
import {RolesCRUD} from "models/roles/roles_crud";
import {GoCDAttributes, GoCDRole, PluginRole, Roles} from "models/roles/roles";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {AuthorizationSettings, Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {Page, PageState} from "views/pages/page";
import {
  AddOperation,
  CloneOperation,
  DeleteOperation,
  EditOperation,
  SaveOperation
} from "views/pages/page_operations";
import {CloneRoleModal, DeleteRoleConfirmModal, EditRoleModal, NewRoleModal} from "views/pages/roles/modals";
import {RolesWidget} from "views/pages/roles/roles_widget";

export interface State extends AddOperation<GoCDRole | PluginRole>, EditOperation<GoCDRole | PluginRole>, CloneOperation<GoCDRole | PluginRole>, DeleteOperation<GoCDRole | PluginRole>, SaveOperation {
  pluginInfos: Array<PluginInfo<Extension>>;
  authConfigs: AuthConfigs;
  roles: Roles;
}

export class RolesPage extends Page<null, State> {

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    const onOperationError = (errorResponse: ErrorResponse) => {
      vnode.state.onError(errorResponse.message);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onAdd = (e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();
      const role = new GoCDRole("", new GoCDAttributes([]));
      new NewRoleModal(role,
                       vnode.state.pluginInfos,
                       vnode.state.authConfigs,
                       vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onEdit = (role: GoCDRole | PluginRole, e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new EditRoleModal(role,
                        vnode.state.pluginInfos,
                        vnode.state.authConfigs,
                        vnode.state.onSuccessfulSave).render();

    };

    vnode.state.onClone = (role: GoCDRole | PluginRole, e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new CloneRoleModal(role,
                         vnode.state.pluginInfos,
                         vnode.state.authConfigs,
                         vnode.state.onSuccessfulSave).render();

    };

    vnode.state.onDelete = (role: GoCDRole | PluginRole, e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new DeleteRoleConfirmModal(role, vnode.state.onSuccessfulSave, onOperationError).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    let mayBeNoPluginMessage;
    if (!vnode.state.pluginInfos || vnode.state.pluginInfos.length === 0) {
      mayBeNoPluginMessage = (<FlashMessage type={MessageType.info}
                                            message="None of the installed plugin supports role based authorization."/>);
    }

    return <div>
      {mayBeNoPluginMessage}
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <RolesWidget pluginInfos={(vnode.state.pluginInfos)}
                   roles={vnode.state.roles}
                   authConfigs={vnode.state.authConfigs}
                   onEdit={vnode.state.onEdit}
                   onClone={vnode.state.onClone} onDelete={vnode.state.onDelete}/>
    </div>;
  }

  pageName(): string {
    return "Server Roles";
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [(<Buttons.Primary data-test-id="role-add-button"
                                             onclick={vnode.state.onAdd.bind(vnode.state)}>Add</Buttons.Primary>)];

    return <HeaderPanel title={this.pageName()} buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PluginInfoCRUD.all({type: ExtensionType.AUTHORIZATION}), AuthConfigsCRUD.all(), RolesCRUD.all()])
                  .then((results) => {
                    results[0].do((successResponse) => {
                      this.pageState           = PageState.OK;
                      const allAuthPluginInfos = successResponse.body;
                      vnode.state.pluginInfos  = this.getPluginInfosWithAuthorizeCapabilities(allAuthPluginInfos);
                    }, () => this.setErrorState());

                    results[1].do((successResponse) => {
                      this.pageState          = PageState.OK;
                      vnode.state.authConfigs = (successResponse.body);
                    }, () => this.setErrorState());

                    results[2].do((successResponse) => {
                      this.pageState    = PageState.OK;
                      vnode.state.roles = successResponse.body;
                    }, () => this.setErrorState());
                  });
  }

  private getPluginInfosWithAuthorizeCapabilities(allAuthPluginInfos: Array<PluginInfo<Extension>>) {
    return _.filter(allAuthPluginInfos, (value: PluginInfo<Extension>) => {
      const authorizationSettings = value.extensionOfType(ExtensionType.AUTHORIZATION) as AuthorizationSettings;
      return authorizationSettings && authorizationSettings.capabilities.canAuthorize;
    });
  }
}
