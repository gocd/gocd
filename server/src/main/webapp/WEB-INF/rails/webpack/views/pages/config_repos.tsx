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

import {AjaxPoller} from "helpers/ajax_poller";
import {ApiResult} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {AnchorVM, ScrollManager} from "views/components/anchor/anchor";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {ConfigRepoVM} from "views/pages/config_repos/config_repo_view_model";
import {ConfigReposWidget} from "views/pages/config_repos/config_repos_widget";
import {NewConfigRepoModal} from "views/pages/config_repos/modals";
import {Page, PageState} from "views/pages/page";
import {AddOperation, FlashContainer, RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";
import styles from "./config_repos/index.scss";

interface SearchOperation {
  unfilteredModels: Stream<ConfigRepoVM[]>;
  filteredModels: Stream<ConfigRepoVM[]>;
  searchText: Stream<string>;
}

interface State extends AddOperation<ConfigRepo>, SaveOperation, SearchOperation, RequiresPluginInfos, FlashContainer {
  flushEtag: () => void;
}

// This instance will be shared with all config repo widgets and never changes
const sm: ScrollManager = new AnchorVM();

export class ConfigReposPage extends Page<null, State> {
  etag: Stream<string> = Stream();

  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.pluginInfos      = Stream();
    vnode.state.unfilteredModels = Stream();
    vnode.state.searchText       = Stream();
    vnode.state.flash            = this.flashMessage;
    vnode.state.filteredModels   = Stream.combine<ConfigRepoVM[]>(
      (collection: Stream<ConfigRepoVM[]>) => _.filter(collection(), (vm) => vm.repo.matches(vnode.state.searchText())),
      [vnode.state.unfilteredModels]
    );

    vnode.state.flushEtag = () => {
      this.etag = Stream();
    };

    this.fetchData(vnode);

    vnode.state.onError = (msg) => {
      this.flashMessage.alert(msg);
    };

    vnode.state.onSuccessfulSave = (msg) => {
      this.flashMessage.success(msg);
      this.fetchData(vnode);
    };

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new NewConfigRepoModal(vnode.state.onSuccessfulSave, vnode.state.onError, vnode.state.pluginInfos).render();
    };

    new AjaxPoller({repeaterFn: this.refreshConfigRepos.bind(this, vnode), initialIntervalSeconds: 10}).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    this.parseRepoLink(sm);

    if (vnode.state.searchText() && _.isEmpty(vnode.state.filteredModels())) {
      return <div><FlashMessage type={MessageType.info}>No Results</FlashMessage>
      </div>;
    }

    return <div>
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <ConfigReposWidget models={vnode.state.filteredModels}
                         flushEtag={vnode.state.flushEtag}
                         pluginInfos={vnode.state.pluginInfos}
                         sm={sm}
      />
    </div>;
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [
      <div class={styles.wrapperForSearchBox}>
        <SearchField property={vnode.state.searchText} dataTestId={"search-box"}
                     placeholder="Search Config Repo"/>
      </div>,
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Add</Buttons.Primary>
    ];
    return <HeaderPanel title="Config Repositories" buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    const state = vnode.state;
    this.pageState = PageState.LOADING;

    return Promise.all([PluginInfoCRUD.all({type: ExtensionTypeString.CONFIG_REPO}), ConfigReposCRUD.all(this.etag())]).then((args) => {
      const pluginInfosResponse: ApiResult<PluginInfos> = args[0];
      pluginInfosResponse.do(
        (successResponse) => {
          state.pluginInfos(successResponse.body);
          this.pageState = PageState.OK;
        },
        (errorResponse) => {
          state.onError(JSON.parse(errorResponse.body!).message);
          this.pageState = PageState.FAILED;
        }
      );
      const apiResponse: ApiResult<ConfigRepo[]> = args[1];
      this.onConfigReposAPIResponse(apiResponse, vnode);
    });
  }

  refreshConfigRepos(vnode: m.Vnode<null, State>) {
    return ConfigReposCRUD.all(this.etag()).then((response) => this.onConfigReposAPIResponse(response, vnode));
  }

  parseRepoLink(sm: ScrollManager) {
    sm.setTarget(m.route.param().id || "");
  }

  pageName(): string {
    return "Config repositories";
  }

  private onConfigReposAPIResponse(apiResponse: ApiResult<ConfigRepo[]>, vnode: m.Vnode<null, State>) {
    if (304 === apiResponse.getStatusCode()) {
      return;
    }

    if (apiResponse.getEtag()) {
      this.etag(apiResponse.getEtag()!);
    }

    apiResponse.do(
      (successResponse) => {
        this.pageState = PageState.OK;
        const models   = _.map(successResponse.body, (repo) => new ConfigRepoVM(repo, vnode.state));
        vnode.state.unfilteredModels(models);
      },
      (errorResponse) => {
        vnode.state.onError(JSON.parse(errorResponse.body!).message);
        this.pageState = PageState.FAILED;
      }
    );
  }
}
