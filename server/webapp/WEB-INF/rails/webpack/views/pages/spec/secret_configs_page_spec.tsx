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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {SecretConfigs} from "models/secret_configs/secret_configs";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {SecretPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {PageState} from "views/pages/page";
import {SecretConfigsPage} from "views/pages/secret_configs";
import {TestHelper} from "views/pages/spec/test_helper";

describe("SecretConfigPage", () => {
  const helper = new TestHelper();
  beforeEach(() => {
    jasmine.Ajax.install();
  });

  afterEach(() => {
    helper.unmount();
    jasmine.Ajax.uninstall();
  });

  it("should disable add secret config button if secret plugin not found", () => {
    mount([]);
    expect(helper.findByDataTestId("add-secret-config")).toBeDisabled();
    expect(helper.findByDataTestId("flash-message-info")).toBeInDOM();
    expect(helper.findByDataTestId("flash-message-info"))
      .toHaveText("No secret plugin installed.");
  });

  it("should not disable add secret config button if secret plugin not found", () => {
    mount([PluginInfo.fromJSON(SecretPluginInfo.file())]);

    expect(helper.findByDataTestId("add-secret-config")).not.toBeDisabled();
    expect(helper.findByDataTestId("flash-message-info")).not.toBeInDOM();
  });

  function mount(pluginInfos: any[]) {
    helper.mountPage(() => new StubbedPage(pluginInfos));
  }
});

class StubbedPage extends SecretConfigsPage {
  private pluginInfos: any[];

  constructor(pluginInfos: any[]) {
    super();
    this.pluginInfos = pluginInfos;
  }

  fetchData(vnode: m.Vnode<any, any>): Promise<any> {
    this.pageState            = PageState.OK;
    vnode.state.secretConfigs = stream(new SecretConfigs());
    vnode.state.pluginInfos   = stream(this.pluginInfos);
    return Promise.resolve();
  }
}
