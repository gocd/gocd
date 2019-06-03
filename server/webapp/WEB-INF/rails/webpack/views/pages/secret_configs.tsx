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

import {ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {PipelineGroupCRUD} from "models/pipeline_configs/pipeline_groups_cache";
import {Rule, Rules} from "models/secret_configs/rules";
import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {SecretConfigsCRUD} from "models/secret_configs/secret_configs_crud";
import {Configurations} from "models/shared/configuration";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons/index";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {Page, PageState} from "views/pages/page";
import {
  AddOperation,
  CloneOperation,
  DeleteOperation,
  EditOperation,
  RequiresPluginInfos, SaveOperation
} from "views/pages/page_operations";
import {
  CloneSecretConfigModal,
  CreateSecretConfigModal,
  EditSecretConfigModal
} from "views/pages/secret_configs/modals";
import {SecretConfigsWidget} from "views/pages/secret_configs/secret_configs_widget";

interface State extends RequiresPluginInfos, AddOperation<SecretConfig>, EditOperation<SecretConfig>, CloneOperation<SecretConfig>, DeleteOperation<SecretConfig>, SaveOperation {
  secretConfigs: Stream<SecretConfigs>;
  resourceAutocompleteHelper: Stream<Map<string, string[]>>;
}

export class SecretConfigsPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.resourceAutocompleteHelper = stream(new Map());

    vnode.state.onAdd = (e) => {
      e.stopPropagation();
      const id   = vnode.state.pluginInfos()[0].id;
      new CreateSecretConfigModal(vnode.state.secretConfigs,
                                  new SecretConfig("",
                                                   "",
                                                   id,
                                                   new Configurations([]),
                                                   new Rules(stream(new Rule("deny", "refer", "pipeline_group", "")))),
                                  vnode.state.pluginInfos(),
                                  vnode.state.onSuccessfulSave, vnode.state.resourceAutocompleteHelper()).render();
    };

    vnode.state.onEdit = (obj, e) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new EditSecretConfigModal(vnode.state.secretConfigs,
                                obj,
                                vnode.state.pluginInfos(),
                                vnode.state.onSuccessfulSave, vnode.state.resourceAutocompleteHelper()).render();
    };

    vnode.state.onClone = (obj, e) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new CloneSecretConfigModal(vnode.state.secretConfigs,
                                 obj,
                                 vnode.state.pluginInfos(),
                                 vnode.state.onSuccessfulSave, vnode.state.resourceAutocompleteHelper()).render();
    };

    vnode.state.onDelete = (obj, e) => {
      e.stopPropagation();
      const deleteModal = new DeleteConfirmModal(`Are you sure you want to delete '${obj.id()}' secret configuration?`
        , () => {
          SecretConfigsCRUD
            .delete(obj)
            .then((response) => {
              response.do(
                (successResponse: SuccessResponse<any>) => {
                  vnode.state.onSuccessfulSave(successResponse.body.message);
                  const filteredEntities = vnode.state.secretConfigs().filter((entity) => {
                    return entity().id() !== obj.id();
                  });
                  vnode.state.secretConfigs(filteredEntities);
                },
                (errorResponse: ErrorResponse) => vnode.state.onError(errorResponse.message));
            })
            .then(deleteModal.close.bind(deleteModal));
        }
        , "Delete Secret Configuration");

      deleteModal.render();
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const flashMessage = this.flashMessage.hasMessage() ?
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      : null;

    return [flashMessage,
      <SecretConfigsWidget secretConfigs={vnode.state.secretConfigs}
                           pluginInfos={vnode.state.pluginInfos}
                           onEdit={vnode.state.onEdit}
                           onClone={vnode.state.onClone}
                           onDelete={vnode.state.onDelete}/>];
  }

  pageName(): string {
    return "Secret Configs";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PluginInfoCRUD.all({type: ExtensionType.SECRETS}), SecretConfigsCRUD.all()])
    return Promise.all([PluginInfoCRUD.all({type: ExtensionType.SECRETS}), SecretConfigsCRUD.all(), PipelineGroupCRUD.all()])
                  .then((results) => {
                    results[0].do((successResponse) => {
                      vnode.state.pluginInfos = stream(successResponse.body);
                      this.pageState          = PageState.OK;
                    }, () => this.setErrorState());

                    results[1].do((successResponse) => {
                      this.pageState            = PageState.OK;
                      vnode.state.secretConfigs = stream(successResponse.body);
                    }, () => this.setErrorState());

                    results[2].do((successResponse) => {
                      vnode.state.resourceAutocompleteHelper()
                           .set("pipeline_group", successResponse.body.map((group) => group.name));
                    }, () => this.setErrorState());
                  });
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const disabled = !vnode.state.pluginInfos || vnode.state.pluginInfos().length === 0;
    return <HeaderPanel title={this.pageName()}
                        buttons={<Buttons.Primary data-test-id="add-secret-config"
                                                  disabled={disabled}
                                                  onclick={vnode.state.onAdd.bind(this)}>Add</Buttons.Primary>}/>;
  }
}
