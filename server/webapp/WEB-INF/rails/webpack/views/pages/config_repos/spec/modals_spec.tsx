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
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ConfigRepo} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {ConfigRepoModal} from "views/pages/config_repos/modals";
import {configRepoPluginInfo, createConfigRepo} from "views/pages/config_repos/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

class TestConfigRepoModal extends ConfigRepoModal {

  constructor(onSuccessfulSave: (msg: m.Children) => any,
              onError: (msg: m.Children) => any,
              pluginInfos: Stream<Array<PluginInfo<any>>>, isNew: boolean = true) {
    super(onSuccessfulSave, onError, pluginInfos);
    this.isNew = isNew;
  }

  setErrorMessageForTest(errorMsg: string): void {
    this.error = errorMsg;
  }

  performSave(): void {
    //do nothing
  }

  title(): string {
    return "Modal Title for Config Repo";
  }

  protected getRepo(): ConfigRepo {
    const configRepo = new ConfigRepo(undefined,
                                    this.pluginInfos()[0].id,
                                    new Material("git", new GitMaterialAttributes()));
    return this.isNew ? configRepo : createConfigRepo();
  }

}

describe("ConfigRepoModal", () => {
  const helper           = new TestHelper();
  const pluginInfos      = stream([configRepoPluginInfo()]);
  const onSuccessfulSave = jasmine.createSpy("onSuccessfulSave");
  const onError          = jasmine.createSpy("onError");

  afterEach((done) => helper.unmount(done));

  it("should render modal title and fields", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    helper.mount(modal.body.bind(modal));

    expect(modal.title()).toEqual("Modal Title for Config Repo");
    expect(helper.findByDataTestId("form-field-label-plugin-id")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-label-material-type")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-label-config-repository-id")).toBeInDOM();

    expect(helper.findByDataTestId("form-field-input-plugin-id")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-plugin-id").get(0).children[0])
      .toContainText("JSON Configuration Plugin");
    expect(helper.findByDataTestId("form-field-input-material-type")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-material-type").get(0).children[0])
      .toContainText("Git");
  });

  it("should display error message", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    modal.setErrorMessageForTest("some error message");
    helper.mount(modal.body.bind(modal));

    expect(helper.findByDataTestId("flash-message-alert")).toBeInDOM();
    expect(helper.findByDataTestId("flash-message-alert")).toContainText("some error message");
  });

  describe("CreateModal", () => {
    it("should not disable id while creating secret config", () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
      helper.mount(modal.body.bind(modal));

      expect(helper.findByDataTestId("form-field-input-config-repository-id")).toBeInDOM();
      expect(helper.findByDataTestId("form-field-input-config-repository-id")).not.toBeDisabled();
    });
  });

  describe("EditModal", () => {
    it("should disable id while editing secret config", () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos, false);
      helper.mount(modal.body.bind(modal));

      expect(helper.findByDataTestId("form-field-input-config-repository-id")).toBeDisabled();
    });
  });

});
