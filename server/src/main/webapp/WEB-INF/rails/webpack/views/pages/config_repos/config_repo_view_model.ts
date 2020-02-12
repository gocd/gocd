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

import {ApiResult} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {AbstractObjCache, ObjectCache, rejectAsString} from "models/base/cache";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {DefinedStructures} from "models/config_repos/defined_structures";
import {ConfigRepo} from "models/config_repos/types";
import {EventAware} from "models/mixins/event_aware";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {EditConfigRepoModal} from "views/pages/config_repos/modals";
import {FlashContainer, RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";

interface PageResources extends SaveOperation, RequiresPluginInfos, FlashContainer {
  resourceAutocompleteHelper: Stream<Map<string, string[]>>;
}

class CRResultCache extends AbstractObjCache<DefinedStructures> {
  private repoId: string;
  private etag: Stream<string> = Stream();

  constructor(repoId: string) {
    super();
    this.repoId = repoId;
  }

  doFetch(resolve: (data: DefinedStructures) => void, reject: (reason: string) => void) {
    DefinedStructures.fetch(this.repoId, this.etag()).then((result) => {
      if (304 === result.getStatusCode()) {
        resolve(this.contents()); // no change
        return;
      }

      if (result.getEtag()) {
        this.etag(result.getEtag()!);
      }

      result.do((resp) => {
        resolve(DefinedStructures.fromJSON(JSON.parse(resp.body)));
      }, (error) => {
        reject(error.message);
      });
    }).catch(rejectAsString(reject));
  }

  empty() {
    // don't dump contents, just force a fresh set of data
    this.etag = Stream();
  }
}

// a subset of Event
interface Propagable {
  stopPropagation: () => void;
}

export class ConfigRepoVM {
  repo: ConfigRepo;
  results: ObjectCache<DefinedStructures>;
  reparseRepo: (e: Propagable) => Promise<void>;
  showEditModal: (e: Propagable) => void;
  showDeleteModal: (e: Propagable) => void;

  constructor(repo: ConfigRepo, page: PageResources, results?: ObjectCache<DefinedStructures>) {
    const cache = results || new CRResultCache(repo.id()!);

    Object.assign(ConfigRepoVM.prototype, EventAware.prototype);
    EventAware.call(this);

    this.repo = repo;
    this.results = cache;

    this.on("expand", () => !cache.failed() && cache.prime(m.redraw));
    this.on("refresh", () => (cache.invalidate(), cache.prime(m.redraw)));

    this.reparseRepo = (e) => {
      e.stopPropagation();
      page.flash.clear();

      const repoId = this.repo.id()!;

      return ConfigReposCRUD.triggerUpdate(repoId).then((result: ApiResult<any>) => {
        repo.materialUpdateInProgress(true);
        result.do(() => {
          page.flash.success(`An update was scheduled for '${repoId}' config repository.`);
          this.notify("refresh");
        }, (err) => {
          page.flash.alert(`Unable to schedule an update for '${repoId}' config repository. ${err.message}`);
        });
      });
    };

    this.showEditModal = (e) => {
      e.stopPropagation();
      page.flash.clear();

      new EditConfigRepoModal(this.repo.id()!,
                              page.onSuccessfulSave,
                              page.onError,
                              page.pluginInfos,
                              page.resourceAutocompleteHelper()).render();
    };

    this.showDeleteModal = (e) => {
      e.stopPropagation();
      page.flash.clear();

      const message                   = ["Are you sure you want to delete the config repository ", m("strong", this.repo.id()), "?"];
      const modal: DeleteConfirmModal = new DeleteConfirmModal(message, () => {
        return ConfigReposCRUD.delete(this.repo).then((resp) => {
          resp.do(
            (resp) => page.onSuccessfulSave(resp.body.message),
            (err) => page.onError(err.message));
        }).then(modal.close.bind(modal));
      });
      modal.render();
    };
  }
}

// tslint:disable-next-line
export interface ConfigRepoVM extends EventAware {}

export interface CRVMAware {
  vm: ConfigRepoVM;
}
