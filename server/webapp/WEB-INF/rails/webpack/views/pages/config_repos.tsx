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

import {AjaxPoller} from "helpers/ajax_poller";
import {ApiResult, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {Attrs, ConfigReposWidget} from "views/pages/config_repos/config_repos_widget";
import {EditConfigRepoModal, NewConfigRepoModal} from "views/pages/config_repos/modals";
import {Page, PageState} from "views/pages/page";
import {AddOperation, SaveOperation} from "views/pages/page_operations";
import * as styles from "./config_repos/index.scss";

interface State extends AddOperation<ConfigRepo>, Attrs<ConfigRepo>, SaveOperation, SearchOperation {
}

interface SearchOperation {
  configReposCopy: Stream<ConfigRepo[] | null>;
  searchText: Stream<string>;
}

export class ConfigReposPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.objects     = stream();
    vnode.state.pluginInfos = stream();
    vnode.state.configReposCopy = stream();
    vnode.state.searchText = stream();

    this.fetchData(vnode);

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new NewConfigRepoModal(vnode.state.onSuccessfulSave, vnode.state.onError, vnode.state.pluginInfos).render();
    };

    vnode.state.onRefresh = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      ConfigReposCRUD.triggerUpdate(repo.id()).then((result: ApiResult<any>) => {
        result.do(() => {
          this.flashMessage.setMessage(MessageType.success, "An update was scheduled for this config repository.");
        }, (err: ErrorResponse) => {
          try {
            if (err.message) {
              this.flashMessage.setMessage(MessageType.alert,
                                           `Unable to schedule an update for this config repository. ${err.message}`);
            } else {
              this.flashMessage.setMessage(MessageType.alert,
                                           `Unable to schedule an update for this config repository. ${err.message}`);
            }
          } catch (e) {
            this.flashMessage.setMessage(MessageType.alert,
                                         `Unable to schedule an update for this config repository. ${err.message}`);
          }
        });
      });

    };

    vnode.state.onEdit = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new EditConfigRepoModal(repo.id(),
                              vnode.state.onSuccessfulSave,
                              vnode.state.onError,
                              vnode.state.pluginInfos).render();
    };

    vnode.state.onDelete = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

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
    new AjaxPoller({repeaterFn: this.refreshConfigRepos.bind(this, vnode), initialIntervalSeconds: 10}).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (this.searchInProgress(vnode) && _.isEmpty(vnode.state.objects())) {
      return <div><FlashMessage type={MessageType.info}>No Results</FlashMessage>
      </div>;
    }

    return <div>
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <ConfigReposWidget objects={vnode.state.objects}
                         pluginInfos={vnode.state.pluginInfos}
                         onRefresh={vnode.state.onRefresh.bind(vnode.state)}
                         onEdit={vnode.state.onEdit.bind(vnode.state)}
                         onDelete={vnode.state.onDelete.bind(vnode.state)}
      />
    </div>;
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [
      <div class={styles.wrapperForSearchBox}>
        <SearchField property={vnode.state.searchText} onchange={() => this.search(vnode)} dataTestId={"search-box"}
                     placeholder="Search Config Repo"/>
      </div>,
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Add</Buttons.Primary>
    ];
    return <HeaderPanel title="Config Repositories" buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    const state = vnode.state;
    state.objects(null);
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
      this.onConfigReposAPIResponse(apiResponse, vnode);
    });
  }

  refreshConfigRepos(vnode: m.Vnode<null, State>) {
    if (this.searchInProgress(vnode)) {
      return Promise.resolve();
    }
    return ConfigReposCRUD.all().then((response) => this.onConfigReposAPIResponse(response, vnode));
  }

  pageName(): string {
    return "Config repositories";
  }

  private onConfigReposAPIResponse(apiResponse: ApiResult<ConfigRepo[]>, vnode: m.Vnode<null, State>) {
    apiResponse.do(
      (successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.objects(successResponse.body);
      },
      (errorResponse) => {
        vnode.state.onError(errorResponse.message);
        this.pageState = PageState.FAILED;
      }
    );
  }

  private searchInProgress(vnode: m.Vnode<null, State>): boolean {
    return vnode.state.searchText() ? true : false;
  }

  private search(vnode: m.Vnode<null, State>) {
    if (!vnode.state.searchText()) {
      vnode.state.objects(vnode.state.configReposCopy());
      vnode.state.configReposCopy();
      return;
    }

    if (!vnode.state.configReposCopy()) {
      vnode.state.configReposCopy(vnode.state.objects());
    }

    // @ts-ignore
    const filtered = vnode.state.configReposCopy().filter((o) => {
      return [
        this.pluginId,
        this.goodRevision,
        this.latestRevision,
        this.materialUrl
      ].some((getter) => getter(o) ? getter(o)!.toLowerCase().includes(vnode.state.searchText().toLowerCase()) : false);
    });
    vnode.state.objects(filtered);
  }

  private pluginId(o: ConfigRepo): string {
    return o.id();
  }

  private goodRevision(o: ConfigRepo): string | null {
    // @ts-ignore
    if (!o.lastParse() || !o.lastParse().goodModification) {
      return null;
    }

    // @ts-ignore
    return o.lastParse().goodModification.revision;
  }

  private latestRevision(o: ConfigRepo): string | null {
    // @ts-ignore
    if (!o.lastParse() || !o.lastParse().latestParsedModification) {
      return null;
    }

    // @ts-ignore
    return o.lastParse().latestParsedModification.revision;
  }

  private materialUrl(o: ConfigRepo): string {
    // @ts-ignore
    return o.material().type() === "p4" ? o.material().attributes().port() : o.material().attributes().url();
  }
}
