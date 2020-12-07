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

import {SparkRoutes} from "helpers/spark_routes";
import Stream from "mithril/stream";
import {ConfigRepo} from "models/config_repos/types";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashContainer, RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";
import {ConfigRepoVM, WebhookUrlGenerator} from "../config_repo_view_model";
import {mockResultsCache} from "./test_data";

describe("ConfigRepoVM", () => {
  it("fetches data on the expand event", () => {
    const vm = createVm();
    expect(vm.results.prime).not.toHaveBeenCalled();

    vm.notify("expand");

    expect(vm.results.prime).toHaveBeenCalled();
  });

  it("invalidates data on refresh event", () => {
    const vm = createVm();

    expect(vm.results.invalidate).not.toHaveBeenCalled();

    vm.notify("refresh");

    expect(vm.results.invalidate).toHaveBeenCalled();
  });

  it("reparseRepo() triggers a reparse on configRepo and invalidates the results cache", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.configRepoTriggerUpdatePath("my-repo"), undefined, 'POST').andReturn({
        responseText:    JSON.stringify({message: `OK`}),
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v2+json'
        },
        status:          201
      });

      const vm = createVm();
      const event = { stopPropagation: jasmine.createSpy("stopPropagation") };

      expect(vm.results.invalidate).not.toHaveBeenCalled();

      vm.reparseRepo(event).catch(done.fail).finally(() => {
        expect(event.stopPropagation).toHaveBeenCalled();
        expect(vm.results.invalidate).toHaveBeenCalled();
        done();
      });
    });
  });
});

function createVm() {
  return new ConfigRepoVM(new ConfigRepo("my-repo"), new MockResources(), mockResultsCache({}));
}

class MockResources implements FlashContainer, RequiresPluginInfos, SaveOperation, WebhookUrlGenerator {
  // tslint:disable-next-line no-empty
  flash = { clear() {}, success() {}, alert() {} };
  pluginInfos: Stream<PluginInfos> = Stream();
  resourceAutocompleteHelper: Stream<Map<string, string[]>> = Stream(new Map());
  // tslint:disable-next-line no-empty
  onError() {}
  // tslint:disable-next-line no-empty
  onSuccessfulSave() {}

  webhookUrlFor(type: string, id: string): string {
    return `//url.for/${type}/${id}`;
  }

  siteUrlsConfigured(): boolean {
    return true;
  }
}
