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

import Stream from "mithril/stream";
import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {secretConfigsTestData, secretConfigTestData} from "models/secret_configs/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {SecretPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {TestSecretConfigModal} from "views/pages/secret_configs/spec/test_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("SecretConfigModal", () => {
  const helper           = new TestHelper();
  const secretConfigs    = SecretConfigs.fromJSON(secretConfigsTestData());
  const pluginInfos      = new PluginInfos(PluginInfo.fromJSON(SecretPluginInfo.file()));
  const resourceHelper   = new Map();
  const onSuccessfulSave = jasmine.createSpy("onSuccessfulSave");

  it("should render modal title and fields", () => {
    const modal = new TestSecretConfigModal(Stream(secretConfigs),
                                            SecretConfig.fromJSON(secretConfigTestData()),
                                            pluginInfos,
                                            onSuccessfulSave);
    helper.mount(modal.body.bind(modal));

    expect(modal.title()).toEqual("Modal title for Secret Configuration");
    expect(helper.byTestId("form-field-label-id")).toBeInDOM();
    expect(helper.byTestId("form-field-label-plugin")).toBeInDOM();

    expect(helper.byTestId("form-field-input-plugin")).toBeInDOM();
    expect(helper.byTestId("form-field-input-plugin").children[0]).toContainText("File based secrets plugin");

    expect(helper.byTestId("rules-widget")).toBeInDOM();
    helper.unmount();
  });

  it("should display error message", () => {
    const modal = new TestSecretConfigModal(Stream(secretConfigs),
                                            SecretConfig.fromJSON(secretConfigTestData()),
                                            pluginInfos,
                                            onSuccessfulSave);
    modal.setErrorMessageForTest("some error message");
    helper.mount(modal.body.bind(modal));

    expect(helper.byTestId("flash-message-alert")).toBeInDOM();
    expect(helper.textByTestId("flash-message-alert")).toContain("some error message");

    helper.unmount();
  });

  it("should display error message if there is error on a field", () => {
    const secretConfig = SecretConfig.fromJSON(secretConfigTestData());
    secretConfig.errors().add("id", "should be unique");
    const modal = new TestSecretConfigModal(Stream(secretConfigs),
                                            secretConfig,
                                            pluginInfos,
                                            onSuccessfulSave);
    helper.mount(modal.body.bind(modal));

    expect(helper.byTestId("form-field-input-id").parentElement).toContainText("should be unique");

    helper.unmount();
  });

  it('should support 4 different entities for rules', () => {
    const modal = new TestSecretConfigModal(Stream(secretConfigs),
                                            SecretConfig.fromJSON(secretConfigTestData()),
                                            pluginInfos,
                                            onSuccessfulSave);
    helper.mount(modal.body.bind(modal));

    expect(helper.byTestId("rules-widget")).toBeInDOM();
    helper.clickByTestId('add-rule-button');

    expect(helper.qa('option', helper.byTestId('rule-type')).length).toBe(6);
    expect(helper.byTestId('rule-type').textContent).toBe('SelectAllPipeline GroupEnvironmentPluggable SCMPackage Repository');
    helper.unmount();
  });

  describe("EditModal", () => {
    it("should disable id while editing secret config", () => {
      const modal = new TestSecretConfigModal(Stream(secretConfigs),
                                              SecretConfig.fromJSON(secretConfigTestData()),
                                              pluginInfos,
                                              onSuccessfulSave, resourceHelper, true);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId("form-field-input-id")).toBeDisabled();

      helper.unmount();
    });
  });

  describe("CreateModal", () => {
    it("should not disable id while creating secret config", () => {
      const modal = new TestSecretConfigModal(Stream(secretConfigs),
                                              SecretConfig.fromJSON(secretConfigTestData()),
                                              pluginInfos,
                                              onSuccessfulSave);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId("form-field-input-id")).toBeInDOM();
      expect(helper.byTestId("form-field-input-id")).not.toBeDisabled();

      helper.unmount();
    });

    it("should callback the add function when add new rule is clicked", () => {
      const modal = new TestSecretConfigModal(Stream(secretConfigs),
                                              SecretConfig.fromJSON(secretConfigTestData()),
                                              pluginInfos,
                                              onSuccessfulSave);
      helper.mount(modal.body.bind(modal));

      expect(helper.qa("tr", helper.byTestId("table-body")).length).toBe(2);

      helper.clickByTestId("add-rule-button");

      expect(helper.qa("tr", helper.byTestId("table-body")).length).toBe(3);

      helper.unmount();
    });
  });
});
