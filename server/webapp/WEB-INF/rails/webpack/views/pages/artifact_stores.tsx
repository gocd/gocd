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
import stream = require("mithril/stream");
import {ArtifactStoresCRUD} from "models/artifact_stores/artifact_stores_crud";
import {ArtifactStore, ArtifactStores} from "models/artifact_stores/artifact_stores_new";
import {Configurations} from "models/shared/configuration";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {ArtifactStoresWidget} from "views/pages/artifact_stores/artifact_stores_widget";
import {
  CloneArtifactStoreModal,
  CreateArtifactStoreModal, DeleteArtifactStoreModal,
  EditArtifactStoreModal
} from "views/pages/artifact_stores/modals";
import {Page, PageState} from "views/pages/page";
import {
  AddOperation,
  CloneOperation,
  DeleteOperation,
  EditOperation, HasMessage,
  RequiresPluginInfos, SaveOperation
} from "views/pages/page_operations";

interface State extends RequiresPluginInfos, AddOperation<ArtifactStore>, EditOperation<ArtifactStore>, CloneOperation<ArtifactStore>, DeleteOperation<ArtifactStore>, HasMessage, SaveOperation {
  artifactStores: ArtifactStores;
}

export class ArtifactStoresPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    let timeoutID: number;
    vnode.state.pluginInfos = stream();

    const setMessage = (msg: m.Children, type: MessageType) => {
      vnode.state.message     = msg;
      vnode.state.messageType = type;
      timeoutID               = window.setTimeout(vnode.state.clearMessage.bind(vnode.state), 10000);
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

      const pluginId         = vnode.state.pluginInfos()[0].id;
      const newArtifactStore = new ArtifactStore("", pluginId, new Configurations([]));
      new CreateArtifactStoreModal(newArtifactStore, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.onEdit = (artifactStore: ArtifactStore, e: Event) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }

      new EditArtifactStoreModal(artifactStore, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.onClone = (artifactStore: ArtifactStore, e: Event) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }

      new CloneArtifactStoreModal(artifactStore, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.onDelete = (artifactStore: ArtifactStore, e: Event) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }

      new DeleteArtifactStoreModal(artifactStore, [], vnode.state.onSuccessfulSave).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <ArtifactStoresWidget artifactStores={vnode.state.artifactStores}
                                 pluginInfos={vnode.state.pluginInfos}
                                 onEdit={vnode.state.onEdit}
                                 onClone={vnode.state.onClone}
                                 onDelete={vnode.state.onDelete}/>;
  }

  pageName(): string {
    return "Authorization Configurations";
  }

  headerPanel(vnode: m.Vnode<null, State>): any {
    const disabled      = !vnode.state.pluginInfos || vnode.state.pluginInfos().length === 0;
    const headerButtons = [
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}
                       data-test-id="add-artifact-stores-button"
                       disabled={disabled}>Add</Buttons.Primary>
    ];
    return <HeaderPanel title={this.pageName()} buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PluginInfoCRUD.all({type: ExtensionType.ARTIFACT}), ArtifactStoresCRUD.all()])
                  .then((results) => {
                    results[0].do((successResponse) => {
                      vnode.state.pluginInfos(successResponse.body);
                      this.pageState = PageState.OK;
                    }, () => this.setErrorState());

                    results[1].do((successResponse) => {
                      vnode.state.artifactStores = successResponse.body;
                      this.pageState             = PageState.OK;
                    }, () => this.setErrorState());
                  });
  }
}
