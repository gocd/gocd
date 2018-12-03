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

import {ApiResult, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {ConfigReposWidget, State} from "views/pages/config_repos/config_repos_widget";
import {EditConfigRepoModal, NewConfigRepoModal} from "views/pages/config_repos/modals";
import {Page, PageState} from "views/pages/page";

export class ConfigReposPage extends Page<null, State> {

  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.configRepos = stream();
    vnode.state.pluginInfos = stream();
    let timeoutID: number;

    this.fetchData(vnode);

    const setMessage = (msg: m.Children, type: MessageType) => {
      vnode.state.message     = msg;
      vnode.state.messageType = type;
      timeoutID               = window.setTimeout(vnode.state.clearMessage.bind(vnode.state), 10000);
    };

    vnode.state.onError = (msg: m.Children) => {
      setMessage(msg, MessageType.alert);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      setMessage(msg, MessageType.success);
      this.fetchData(vnode);
    };

    vnode.state.clearMessage = () => {
      vnode.state.message = null;
    };

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      new NewConfigRepoModal(vnode.state.onSuccessfulSave, vnode.state.onError, vnode.state.pluginInfos).render();
    };

    vnode.state.onRefresh = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }

      ConfigReposCRUD.get(repo.id()).then((result: ApiResult<any>) => {
        result.do(() => {
          setMessage("An update was scheduled for this config repository.", MessageType.success);
        }, (err: ErrorResponse) => {
          try {
            const parse = JSON.parse(err.body || "{}");
            if (parse.message) {
              setMessage(`Unable to schedule an update for this config repository. ${parse.message}`, MessageType.alert);
            } else {
              setMessage(`Unable to schedule an update for this config repository. ${err.message}`, MessageType.alert);
            }
          } catch (e) {
            setMessage(`Unable to schedule an update for this config repository. ${err.message}`, MessageType.alert);
          }
        });
      });

    };

    vnode.state.onEdit = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
      new EditConfigRepoModal(repo.id(), vnode.state.onSuccessfulSave, vnode.state.onError, vnode.state.pluginInfos).render();
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
            resp.do(
              (successResponse: SuccessResponse<any>) => vnode.state.onSuccessfulSave(successResponse.body.message),
              (errorResponse: ErrorResponse) => vnode.state.onError(errorResponse.message));
          })
          .then(modal.close.bind(modal));
      });
      modal.render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <div>
      <FlashMessage type={vnode.state.messageType} message={vnode.state.message}/>
      <ConfigReposWidget objects={vnode.state.configRepos}
                         pluginInfos={vnode.state.pluginInfos}
                         onRefresh={vnode.state.onRefresh.bind(vnode.state)}
                         onEdit={vnode.state.onEdit.bind(vnode.state)}
                         onDelete={vnode.state.onDelete.bind(vnode.state)}
      />
    </div>;
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Add</Buttons.Primary>
    ];
    return <HeaderPanel title="Config repositories" buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    const state = vnode.state;
    state.configRepos(null);
    this.pageState = PageState.LOADING;

    return Promise.all([PluginInfoCRUD.all({type: ExtensionType.CONFIG_REPO}), ConfigReposCRUD.all()]).then((args) => {
      const pluginInfosResponse: ApiResult<Array<PluginInfo<any>>> = args[0];
      pluginInfosResponse.do(
        (successResponse) => {
          state.pluginInfos(successResponse.body);
          this.pageState = PageState.OK;
        },
        (errorResponse) => {
          state.onError(errorResponse.message);
          this.pageState = PageState.FAILED;
        }
      );
      const apiResponse: ApiResult<ConfigRepo[]> = args[1];
      apiResponse.do(
        (successResponse) => {
          this.pageState = PageState.OK;
          state.configRepos(successResponse.body);
        },
        (errorResponse) => {
          state.onError(errorResponse.message);
          this.pageState = PageState.FAILED;
        }
      );
    });
  }

  pageName(): string {
    return "Config repositories";
  }
}
