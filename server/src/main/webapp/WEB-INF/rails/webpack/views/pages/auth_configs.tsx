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

import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {AuthConfig, AuthConfigs} from "models/auth_configs/auth_configs";
import {AuthConfigsCRUD} from "models/auth_configs/auth_configs_crud";
import {Configurations} from "models/shared/configuration";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {Link} from "views/components/link";
import {AuthConfigsWidget} from "views/pages/auth_configs/auth_configs_widget";
import {CloneAuthConfigModal, CreateAuthConfigModal, DeleteAuthConfigModal, EditAuthConfigModal} from "views/pages/auth_configs/modals";
import {Page, PageState} from "views/pages/page";
import {AddOperation, CloneOperation, DeleteOperation, EditOperation, RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";

interface State extends AddOperation<AuthConfig>, RequiresPluginInfos, EditOperation<AuthConfig>, CloneOperation<AuthConfig>, DeleteOperation<AuthConfig>, SaveOperation {
  authConfigs: AuthConfigs;
}

export class AuthConfigsPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.pluginInfos = Stream();

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onAdd = (e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();

      const pluginId      = vnode.state.pluginInfos()[0].id;
      const newAuthConfig = new AuthConfig("", pluginId, false, new Configurations([]));
      new CreateAuthConfigModal(newAuthConfig, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onEdit = (obj: AuthConfig, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new EditAuthConfigModal(obj, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onClone = (obj: AuthConfig, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new CloneAuthConfigModal(obj, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave).render();

    };

    vnode.state.onDelete = (obj: AuthConfig, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new DeleteAuthConfigModal(obj, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave, (msg: m.Children,
                                                                                               type: MessageType) => {
        this.flashMessage.setMessage(type, msg);
      }).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <div>
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
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
    return <HeaderPanel title={this.pageName()} buttons={headerButtons} help={this.helpText()}/>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PluginInfoCRUD.all({type: ExtensionTypeString.AUTHORIZATION}), AuthConfigsCRUD.all()])
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

  helpText(): m.Children {
    return AuthConfigsWidget.helpText();
  }
}
