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

import m from "mithril";
import Stream from "mithril/stream";
import {ConfigRepo} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Rule} from "models/rules/rules";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ConfigRepoModal} from "views/pages/config_repos/modals";
import {configRepoPluginInfo, createConfigRepoParsedWithError} from "views/pages/config_repos/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

class TestConfigRepoModal extends ConfigRepoModal {

  constructor(onSuccessfulSave: (msg: m.Children) => any,
              onError: (msg: m.Children) => any,
              pluginInfos: Stream<PluginInfos>,
              resourceAutocompleteHelper: Map<string, string[]> = new Map(),
              isNew: boolean = true) {
    super(onSuccessfulSave, onError, pluginInfos, resourceAutocompleteHelper);
    this.isNew = isNew;
  }

  setErrorMessageForTest(errorMsg: string): void {
    this.error = errorMsg;
  }

  performSave(): Promise<any> {
    return Promise.resolve();
  }

  title(): string {
    return "Modal Title for Config Repo";
  }

  protected getRepo(): ConfigRepo {
    const configRepo = new ConfigRepo(undefined,
                                      this.pluginInfos()[0].id,
                                      new Material("git", new GitMaterialAttributes()));
    const parsedRepo = createConfigRepoParsedWithError();
    parsedRepo.rules().push(Stream(new Rule("allow", "refer", "pipeline", "common*")));
    return this.isNew ? configRepo : parsedRepo;
  }

}

describe("ConfigRepoModal", () => {
  const helper           = new TestHelper();
  const pluginInfos      = Stream(new PluginInfos(configRepoPluginInfo()));
  const onSuccessfulSave = jasmine.createSpy("onSuccessfulSave");
  const onError          = jasmine.createSpy("onError");

  afterEach((done) => helper.unmount(done));

  it("should render modal title and fields", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    helper.mount(modal.body.bind(modal));

    expect(modal.title()).toEqual("Modal Title for Config Repo");
    expect(helper.byTestId("form-field-label-plugin-id")).toBeInDOM();
    expect(helper.byTestId("form-field-label-material-type")).toBeInDOM();
    expect(helper.byTestId("form-field-label-config-repository-name")).toBeInDOM();

    expect(helper.byTestId("form-field-input-plugin-id")).toBeInDOM();
    expect(helper.byTestId("form-field-input-plugin-id").firstElementChild!.textContent).toContain("JSON Configuration Plugin");
    expect(helper.byTestId("form-field-input-material-type")).toBeInDOM();
    expect(helper.byTestId("form-field-input-material-type").firstElementChild!.textContent).toContain("Git");
  });

  it("should display error message", () => {
    const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
    modal.setErrorMessageForTest("some error message");
    helper.mount(modal.body.bind(modal));

    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toContain("some error message");
  });

  describe("CreateModal", () => {
    it("should not disable id while creating secret config", () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId("form-field-input-config-repository-name")).toBeInDOM();
      expect(helper.byTestId("form-field-input-config-repository-name")).not.toBeDisabled();
    });

    it('should not add any default rule', () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId('rules-widget')).toBeInDOM();
      expect(helper.byTestId('rules-table')).not.toBeInDOM();
      expect(helper.byTestId('add-rule-button')).toBeInDOM();
    });
  });

  describe("EditModal", () => {
    it("should disable id while editing secret config", () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos, new Map(), false);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId("form-field-input-config-repository-name")).toBeDisabled();
    });

    it('should render any rules configures', () => {
      const modal = new TestConfigRepoModal(onSuccessfulSave, onError, pluginInfos, new Map(), false);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId('rules-table')).toBeInDOM();
      expect(helper.qa('tr', helper.byTestId('rules-table')).length).toBe(2);
    });
  });

});
