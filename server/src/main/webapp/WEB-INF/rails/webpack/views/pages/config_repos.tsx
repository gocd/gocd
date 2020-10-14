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

import {AjaxPoller} from "helpers/ajax_poller";
import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ObjectCache} from "models/base/cache";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {DefinedStructures} from "models/config_repos/defined_structures";
import {ConfigRepo, ConfigRepos} from "models/config_repos/types";
import {Permissions, SupportedEntity} from "models/shared/permissions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {AnchorVM, ScrollManager} from "views/components/anchor/anchor";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {ConfigReposWidget} from "views/pages/config_repos/config_repos_widget";
import {ConfigRepoVM} from "views/pages/config_repos/config_repo_view_model";
import {NewConfigRepoModal} from "views/pages/config_repos/modals";
import {Page, PageState} from "views/pages/page";
import {AddOperation, FlashContainer, RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";
import styles from "./config_repos/index.scss";

interface SearchOperation {
  unfilteredModels: Stream<ConfigRepoVM[]>;
  filteredModels: Stream<ConfigRepoVM[]>;
  searchText: Stream<string>;
  resourceAutocompleteHelper: Stream<Map<string, string[]>>;
}

interface State extends AddOperation<ConfigRepo>, SaveOperation, SearchOperation, RequiresPluginInfos, FlashContainer {
  flushEtag: () => void;
}

// This instance will be shared with all config repo widgets and never changes
const sm: ScrollManager = new AnchorVM();

export class ConfigReposPage extends Page<null, State> {
  etag: Stream<string> = Stream();
  resultCaches = new Map<string, ObjectCache<DefinedStructures>>();

  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.pluginInfos      = Stream();
    vnode.state.unfilteredModels = Stream();
    vnode.state.searchText       = Stream();
    vnode.state.flash            = this.flashMessage;
    this.updateFilterText(vnode);
    vnode.state.resourceAutocompleteHelper = Stream(new Map());
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
      new NewConfigRepoModal(vnode.state.onSuccessfulSave, vnode.state.onError, vnode.state.pluginInfos, vnode.state.resourceAutocompleteHelper()).render();
    };

    new AjaxPoller({repeaterFn: this.refreshConfigRepos.bind(this, vnode), initialIntervalSeconds: 10}).start();
  }

  updateFilterText(vnode: m.Vnode<null, State>) {
    vnode.state.filteredModels   = Stream.combine<ConfigRepoVM[]>(
      (collection: Stream<ConfigRepoVM[]>) => _.filter(collection(), (vm) => vm.repo.matches(vnode.state.searchText())),
      [vnode.state.unfilteredModels]
    );
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
        <SearchField property={vnode.state.searchText}
                     oninput={this.updateFilterText.bind(this, vnode)}
                     dataTestId={"search-box"}
                     placeholder="Search Config Repo"/>
      </div>,
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Add</Buttons.Primary>
    ];
    return <HeaderPanel title="Config Repositories" buttons={headerButtons} help={this.helpText()}/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    const state = vnode.state;
    this.pageState = PageState.LOADING;

    return Promise.all([
                         PluginInfoCRUD.all({type: ExtensionTypeString.CONFIG_REPO}),
                         ConfigReposCRUD.all(this.etag()),
                         Permissions.all([SupportedEntity.config_repo])
                       ]).then((args) => {
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
      const apiResponse: ApiResult<ConfigRepos> = args[1];
      const permissionsResponse: ApiResult<Permissions> = args[2];
      this.onConfigReposAPIResponse(apiResponse, permissionsResponse, vnode);
    });
  }

  refreshConfigRepos(vnode: m.Vnode<null, State>) {
    return Promise.all([ConfigReposCRUD.all(), Permissions.all([SupportedEntity.config_repo])])
                  .then((args) => this.onConfigReposAPIResponse(args[0], args[1], vnode));
  }

  parseRepoLink(sm: ScrollManager) {
    sm.setTarget(m.route.param().id || "");
  }

  pageName(): string {
    return "Config repositories";
  }

  helpText(): m.Children {
    return ConfigReposWidget.helpText();
  }

  private onConfigReposAPIResponse(apiResponse: ApiResult<ConfigRepos>, permissionsResponse: ApiResult<Permissions>, vnode: m.Vnode<null, State>) {
    if (304 === apiResponse.getStatusCode() && 304 === permissionsResponse.getStatusCode()) {
      return;
    }

    if (apiResponse.getEtag()) {
      this.etag(apiResponse.getEtag()!);
    }

    const onError = (errorResponse: ErrorResponse) => {
      vnode.state.onError(JSON.parse(errorResponse.body!).message);
      this.pageState = PageState.FAILED;
    };

    apiResponse.do((successResponse) => {
        permissionsResponse.do((permissions) => {
          const repoPermissions = permissions.body.for(SupportedEntity.config_repo);
          successResponse.body.configRepos.map((repo) => {
            repo.canAdminister(repoPermissions.canAdminister(repo.id()!));
          });
        }, onError);
        this.pageState = PageState.OK;
        const reusedCaches = new Map<string, ObjectCache<DefinedStructures>>();
        const models = _.map(successResponse.body.configRepos, (repo) => {
          const vm = new ConfigRepoVM(repo, vnode.state, this.resultCaches.get(repo.id()!));
          vm.results.invalidate(); // always refresh definitions when getting new config repo data
          // persist results cache to the next poll; this eliminates the flicker when pulling new data after the first load
          reusedCaches.set(repo.id()!, vm.results);
          return vm;
        });
        this.resultCaches = reusedCaches; // release any unused caches for garbage collection
        vnode.state.unfilteredModels(models);

        successResponse.body.autoCompletion.forEach((suggestion) => {
          vnode.state.resourceAutocompleteHelper().set(suggestion.key, ["*"].concat(suggestion.value));
        });
      }, onError);
  }
}
