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

import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as s from "underscore.string";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {ConfigReposWidget, State} from "views/pages/config_repos/config_repos_widget";
import {EditConfigRepoModal, NewConfigRepoModal} from "views/pages/config_repos/modals";

export class ConfigReposPage extends MithrilComponent<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.configRepos = stream();
    vnode.state.pluginInfos = stream();
    this.reload(vnode.state);
    let timeoutID: number;

    const setMessage = (msg: m.Children, type: MessageType) => {
      vnode.state.message = msg;
      vnode.state.messageType = type;
      timeoutID           = window.setTimeout(vnode.state.clearMessage.bind(vnode.state), 10000);
      this.reload(vnode.state);
    };

    vnode.state.onError = (msg: m.Children) => {
      setMessage(msg, MessageType.alert);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      setMessage(msg, MessageType.success);
    };

    vnode.state.clearMessage = () => {
      vnode.state.message        = null;
    };

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      new NewConfigRepoModal(vnode.state.onSuccessfulSave, vnode.state.onError, vnode.state.pluginInfos).render();
    };

    vnode.state.onEdit = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      new EditConfigRepoModal(repo.id, vnode.state.onSuccessfulSave, vnode.state.onError, vnode.state.pluginInfos).render();
    };

    vnode.state.onDelete = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }

      const message = <span>Are you sure you want to delete the config repository <strong>{repo.id}</strong>?</span>;
      const modal   = new DeleteConfirmModal(message, () => {
        ConfigReposCRUD
          .delete(repo)
          .then((resp) => {
            vnode.state.onSuccessfulSave(resp.message);
          })
          .then(modal.close.bind(modal))
          .catch((xhr: XMLHttpRequest) => {
              let message;
              try {
                message = JSON.parse(xhr.responseText).message;
              } catch (e) {
                // ignore
              }

              if (s.isBlank(message)) {
                message = `There was an unknown error (${xhr.status}) deleting the config repo ${repo.id}.`;
              }

              vnode.state.onError(message);
            }
          )
          .then(modal.close.bind(modal));
      });

      modal.render();
    };
  }

  view(vnode: m.Vnode<null, State>) {
    const headerButtons = [
      <Buttons.Primary onclick={vnode.state.onAdd.bind(this)}>Add</Buttons.Primary>
    ];

    return <main class="main-container">
      <HeaderPanel title="Config repositories" buttons={headerButtons}/>
      <FlashMessage type={vnode.state.messageType} message={vnode.state.message}/>
      <ConfigReposWidget objects={vnode.state.configRepos}
                         pluginInfos={vnode.state.pluginInfos}
                         onEdit={vnode.state.onEdit.bind(this)}
                         onDelete={vnode.state.onDelete.bind(this)}
      />
    </main>;
  }

  private reload(state: State) {
    state.configRepos(null);

    Promise.all([PluginInfoCRUD.all({type: ExtensionType.CONFIG_REPO}), ConfigReposCRUD.all()]).then((args) => {
      state.pluginInfos(args[0]);
      state.configRepos(args[1]);
    });
  }
}
