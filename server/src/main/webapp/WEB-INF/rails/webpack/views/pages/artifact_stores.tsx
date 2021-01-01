/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import m from "mithril";
import Stream from "mithril/stream";
import {ArtifactStore, ArtifactStores} from "models/artifact_stores/artifact_stores";
import {ArtifactStoresCRUD} from "models/artifact_stores/artifact_stores_crud";
import {Configurations} from "models/shared/configuration";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {ArtifactStoresWidget} from "views/pages/artifact_stores/artifact_stores_widget";
import {
  CloneArtifactStoreModal,
  CreateArtifactStoreModal,
  DeleteArtifactStoreModal,
  EditArtifactStoreModal
} from "views/pages/artifact_stores/modals";
import {Page, PageState} from "views/pages/page";
import {AddOperation, CloneOperation, DeleteOperation, EditOperation, RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";

interface State extends RequiresPluginInfos, AddOperation<ArtifactStore>, EditOperation<ArtifactStore>, CloneOperation<ArtifactStore>, DeleteOperation<ArtifactStore>, SaveOperation {
  artifactStores: ArtifactStores;
}

export class ArtifactStoresPage extends Page<null, State> {
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

      const pluginId         = vnode.state.pluginInfos()[0].id;
      const newArtifactStore = new ArtifactStore("", pluginId, new Configurations([]));
      new CreateArtifactStoreModal(newArtifactStore, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.onEdit = (artifactStore: ArtifactStore, e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new EditArtifactStoreModal(artifactStore, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.onClone = (artifactStore: ArtifactStore, e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new CloneArtifactStoreModal(artifactStore, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.onDelete = (artifactStore: ArtifactStore, e: Event) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new DeleteArtifactStoreModal(artifactStore, new PluginInfos(), vnode.state.onSuccessfulSave,
                                   this.flashMessage.setMessage.bind(this.flashMessage)).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return (<div>
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <ArtifactStoresWidget artifactStores={vnode.state.artifactStores}
                            pluginInfos={vnode.state.pluginInfos}
                            onEdit={vnode.state.onEdit}
                            onClone={vnode.state.onClone}
                            onDelete={vnode.state.onDelete}/>
    </div>);
  }

  pageName(): string {
    return "Artifact Stores";
  }

  headerPanel(vnode: m.Vnode<null, State>): any {
    const disabled      = !vnode.state.pluginInfos || vnode.state.pluginInfos().length === 0;
    const headerButtons = [
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}
                       data-test-id="add-artifact-stores-button"
                       disabled={disabled}>Add</Buttons.Primary>
    ];
    return <HeaderPanel title={this.pageName()} buttons={headerButtons} help={this.helpText()}/>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PluginInfoCRUD.all({type: ExtensionTypeString.ARTIFACT}), ArtifactStoresCRUD.all()])
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

  helpText(): m.Children {
    return ArtifactStoresWidget.helpText();
  }
}
