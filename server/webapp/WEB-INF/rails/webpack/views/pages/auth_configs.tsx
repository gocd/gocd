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

import {ErrorResponse} from "helpers/api_request_builder";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {AuthConfigsCRUD} from "models/auth_configs/auth_configs_crud";
import {AuthConfig, AuthConfigs} from "models/auth_configs/auth_configs_new";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {AuthConfigsWidget} from "views/pages/auth_configs/auth_configs_widget";
import {
  CloneAuthConfigModal,
  DeleteAuthConfigConfirmModal,
  EditAuthConfigModal,
  NewAuthConfigModal
} from "views/pages/auth_configs/modals";
import {Page, PageState} from "views/pages/page";
import {
  AddOperation,
  CloneOperation,
  DeleteOperation,
  EditOperation, HasMessage,
  RequiresPluginInfos, SaveOperation
} from "views/pages/page_operations";

interface State extends AddOperation<AuthConfig>, RequiresPluginInfos, EditOperation<AuthConfig>, CloneOperation<AuthConfig>, DeleteOperation<AuthConfig>, SaveOperation, HasMessage {
  authConfigs: AuthConfigs;
}

export class AuthConfigsPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    let timeoutID: number;
    vnode.state.pluginInfos = stream();

    const setMessage       = (msg: m.Children, type: MessageType) => {
      vnode.state.message     = msg;
      vnode.state.messageType = type;
      timeoutID               = window.setTimeout(vnode.state.clearMessage.bind(vnode.state), 10000);
    };
    const onOperationError = (errorResponse: ErrorResponse) => {
      vnode.state.onError(errorResponse.message);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      setMessage(msg, MessageType.success);
      this.fetchData(vnode);
    };

    vnode.state.clearMessage = () => {
      vnode.state.message = null;
    };

    vnode.state.onAdd = (e: Event) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      new NewAuthConfigModal(vnode.state.pluginInfos(), vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onEdit = (obj: AuthConfig, e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      AuthConfigsCRUD
        .get(obj.id())
        .then((result) => {
          result.do(
            (successResponse) => {
              new EditAuthConfigModal(successResponse.body,
                                      vnode.state.pluginInfos(),
                                      vnode.state.onSuccessfulSave).render();
            },
            onOperationError
          );
        });
    };

    vnode.state.onClone = (obj: AuthConfig, e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      AuthConfigsCRUD
        .get(obj.id())
        .then((result) => {
          result.do(
            (successResponse) => {
              new CloneAuthConfigModal(successResponse.body.object,
                                       vnode.state.pluginInfos(),
                                       vnode.state.onSuccessfulSave).render();
            },
            onOperationError
          );
        });
    };

    vnode.state.onDelete = (obj: AuthConfig, e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      new DeleteAuthConfigConfirmModal(obj, vnode.state.onSuccessfulSave, onOperationError).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <div>
      <FlashMessage type={vnode.state.messageType} message={vnode.state.message}/>
      <AuthConfigsWidget authConfigs={vnode.state.authConfigs}
                         pluginInfos={vnode.state.pluginInfos}
                         onEdit={vnode.state.onEdit.bind(vnode.state)}
                         onClone={vnode.state.onClone.bind(vnode.state)}
                         onDelete={vnode.state.onDelete.bind(vnode.state)}/>
    </div>;
  }

  pageName(): string {
    return "Authorization Configurations";
  }

  headerPanel(vnode: m.Vnode<null, State>): any {
    const disabled      = !vnode.state.pluginInfos || vnode.state.pluginInfos().length === 0;
    const headerButtons = [
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}
                       data-test-id="add-auth-config-button"
                       disabled={disabled}>Add</Buttons.Primary>
    ];
    return <HeaderPanel title={this.pageName()} buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PluginInfoCRUD.all({type: ExtensionType.AUTHORIZATION}), AuthConfigsCRUD.all()])
                  .then((results) => {
                    results[0].do((successResponse) => {
                      this.pageState = PageState.OK;
                      vnode.state.pluginInfos(successResponse.body);
                    }, () => this.setErrorState());

                    results[1].do((success) => {
                      this.pageState          = PageState.OK;
                      vnode.state.authConfigs = success.body;
                    }, () => this.setErrorState());
                  });
  }
}
