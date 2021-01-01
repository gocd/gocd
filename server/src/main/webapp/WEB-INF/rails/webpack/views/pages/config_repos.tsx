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

import {AjaxPoller} from "helpers/ajax_poller";
import {ApiResult} from "helpers/api_request_builder";
import {pipeline} from "helpers/utils";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {AbstractObjCache, ObjectCache} from "models/base/cache";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {DefinedStructures} from "models/config_repos/defined_structures";
import {ConfigRepo} from "models/config_repos/types";
import { baseUrlProvider, currentUrlOriginAndPath } from "models/server-configuration/base_url_provider";
import {SiteUrls} from "models/server-configuration/server_configuration";
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
import {ConfigRepoVM, WebhookUrlGenerator} from "views/pages/config_repos/config_repo_view_model";
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

type State = AddOperation<ConfigRepo> & SaveOperation & SearchOperation & RequiresPluginInfos & FlashContainer & WebhookUrlGenerator;

// This instance will be shared with all config repo widgets and never changes
const sm: ScrollManager = new AnchorVM();

class ConfigReposCache extends AbstractObjCache<ConfigRepo[]> {
  etag: Stream<string> = Stream();
  autocompleteSuggestions = Stream(new Map<string, string[]>());

  doFetch(resolve: (data: ConfigRepo[]) => void, reject: (error: string) => void) {
    ConfigReposCRUD.all(this.etag()).then((apiResult) => {
      if (304 === apiResult.getStatusCode()) {
        return resolve(this.contents());
      }

      apiResult.do((successResponse) => {
        if (apiResult.getEtag()) {
          this.etag(apiResult.getEtag()!);
        }

        this.autocompleteSuggestions(
          _.reduce(successResponse.body.autoCompletion, (map, s) => {
              map.set(s.key, ["*"].concat(s.value));
              return map;
            }, new Map<string, string[]>()
          )
        );

        resolve(successResponse.body.configRepos);
      }, errorResponse => {
        reject(errorResponse.body!);
      });
    });
  }

  markStale() {
    // don't dump the old contents, just allow overwrite
  }

  flushEtag() {
    this.etag = Stream();
  }

  promise(): Promise<ConfigRepo[]> {
    this.invalidate();

    return new Promise((res, rej) => {
      if (this.ready()) { // shouldn't get here because we just invalidated, but just in case
        return res(this.contents());
      }

      this.prime(() => res(this.contents()), () => rej(this.failureReason()));
    });
  }
}

export class ConfigReposPage extends Page<null, State> {
  cache = new ConfigReposCache();
  resultCaches = new Map<string, ObjectCache<DefinedStructures>>();
  siteUrls = Stream(new SiteUrls());

  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.pluginInfos      = Stream();
    vnode.state.unfilteredModels = Stream();
    vnode.state.searchText       = Stream();
    vnode.state.flash            = this.flashMessage;
    vnode.state.resourceAutocompleteHelper = this.cache.autocompleteSuggestions;

    this.updateFilterText(vnode);
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

    vnode.state.webhookUrlFor = (_, __) => "server url provider not initialized.";
    vnode.state.siteUrlsConfigured = () => this.siteUrls().isConfigured();

    new AjaxPoller({repeaterFn: this.refreshConfigRepos.bind(this, vnode), initialIntervalSeconds: 10}).start();
  }

  oncreate(vnode: m.Vnode<null, State>) {
    const el = document.querySelector("[data-server-site-urls]");

    if (el) {
      const json = JSON.parse(el.getAttribute("data-server-site-urls")!);
      this.siteUrls(SiteUrls.fromJSON(json));
    }

    const fallback = pipeline(                            // To derive the base URI from the ConfigRepos page:
      currentUrlOriginAndPath(),                          //   1. get the origin and path of the current URI
      (u) => u.replace(/\/admin\/config_repos(\/)?$/, "") //   2. then slice off the last 2 path segments
    );

    // try to get the configured site URLs, then fall back to guessing if absent
    const serverUrl = baseUrlProvider(this.siteUrls(), () => fallback);

    vnode.state.webhookUrlFor = (type, id) => `${serverUrl()}/api/webhooks/${encodeURIComponent(type)}/config_repos/${encodeURIComponent(id)}`;
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
                         flushEtag={() => this.cache.flushEtag()}
                         pluginInfos={vnode.state.pluginInfos}
                         sm={sm}
                         urlGenerator={vnode.state}
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
                         this.cache.promise(),
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
      const [,configRepos, permissionsResponse] = args;
      this.onConfigReposAPIResponse(configRepos, permissionsResponse, vnode);
    });
  }

  refreshConfigRepos(vnode: m.Vnode<null, State>) {
    return Promise.all([this.cache.promise(), Permissions.all([SupportedEntity.config_repo])])
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

  private onConfigReposAPIResponse(configRepos: ConfigRepo[], permissionsResponse: ApiResult<Permissions>, vnode: m.Vnode<null, State>) {
    const onError = (errorBody: string) => {
      vnode.state.onError(JSON.parse(errorBody).message);
      this.pageState = PageState.FAILED;
    };

    if (this.cache.failed()) {
      return onError(this.cache.failureReason()!);
    }

    this.pageState = PageState.OK;

    permissionsResponse.do((permissions) => {
      const repoPermissions = permissions.body.for(SupportedEntity.config_repo);
      configRepos.forEach((repo) => repo.canAdminister(repoPermissions.canAdminister(repo.id())));
    }, (e) => onError(e.body!));

    const reusedCaches = new Map<string, ObjectCache<DefinedStructures>>();
    const models = _.map(configRepos, (repo) => {
      const vm = new ConfigRepoVM(repo, vnode.state, this.resultCaches.get(repo.id()));
      vm.results.invalidate(); // always refresh definitions when getting new config repo data
      // persist results cache to the next poll; this eliminates the flicker when pulling new data after the first load
      reusedCaches.set(repo.id()!, vm.results);
      return vm;
    });
    this.resultCaches = reusedCaches; // release any unused caches for garbage collection
    vnode.state.unfilteredModels(models);
  }
}
