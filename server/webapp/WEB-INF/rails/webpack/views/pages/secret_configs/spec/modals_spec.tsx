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

import * as stream from "mithril/stream";
import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {secretConfigsTestData, secretConfigTestData} from "models/secret_configs/spec/test_data";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {SecretPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import {TestSecretConfigModal} from "views/pages/secret_configs/spec/test_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("SecretConfigModal", () => {
  const helper           = new TestHelper();
  const secretConfigs    = SecretConfigs.fromJSON(secretConfigsTestData());
  const pluginInfos      = [PluginInfo.fromJSON(SecretPluginInfo.file())];
  const resourceHelper   = new Map();
  const onSuccessfulSave = jasmine.createSpy("onSuccessfulSave");

  it("should render modal title and fields", () => {
    const modal = new TestSecretConfigModal(stream(secretConfigs),
                                            SecretConfig.fromJSON(secretConfigTestData()),
                                            pluginInfos,
                                            onSuccessfulSave);
    helper.mount(modal.body.bind(modal));

    expect(modal.title()).toEqual("Modal title for Secret Configuration");
    expect(helper.findByDataTestId("form-field-label-id")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-label-plugin")).toBeInDOM();

    expect(helper.findByDataTestId("form-field-input-plugin")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-plugin").get(0).children[0])
      .toContainText("File based secrets plugin");

    expect(helper.findByDataTestId("rules-widget")).toBeInDOM();
    helper.unmount();
  });

  it("should display error message", () => {
    const modal = new TestSecretConfigModal(stream(secretConfigs),
                                            SecretConfig.fromJSON(secretConfigTestData()),
                                            pluginInfos,
                                            onSuccessfulSave);
    modal.setErrorMessageForTest("some error message");
    helper.mount(modal.body.bind(modal));

    expect(helper.findByDataTestId("flash-message-alert")).toBeInDOM();
    expect(helper.findByDataTestId("flash-message-alert")).toContainText("some error message");

    helper.unmount();
  });

  it("should display error message if there is error on a field", () => {
    const secretConfig = SecretConfig.fromJSON(secretConfigTestData());
    secretConfig.errors().add("id", "should be unique");
    const modal = new TestSecretConfigModal(stream(secretConfigs),
                                            secretConfig,
                                            pluginInfos,
                                            onSuccessfulSave);
    helper.mount(modal.body.bind(modal));

    expect(helper.findByDataTestId("form-field-input-id").parent()).toContainText("should be unique");

    helper.unmount();
  });

  describe("EditModal", () => {
    it("should disable id while editing secret config", () => {
      const modal = new TestSecretConfigModal(stream(secretConfigs),
                                              SecretConfig.fromJSON(secretConfigTestData()),
                                              pluginInfos,
                                              onSuccessfulSave, resourceHelper, true);
      helper.mount(modal.body.bind(modal));

      expect(helper.findByDataTestId("form-field-input-id")).toBeDisabled();

      helper.unmount();
    });
  });

  describe("CreateModal", () => {
    it("should not disable id while creating secret config", () => {
      const modal = new TestSecretConfigModal(stream(secretConfigs),
                                              SecretConfig.fromJSON(secretConfigTestData()),
                                              pluginInfos,
                                              onSuccessfulSave);
      helper.mount(modal.body.bind(modal));

      expect(helper.findByDataTestId("form-field-input-id")).toBeInDOM();
      expect(helper.findByDataTestId("form-field-input-id")).not.toBeDisabled();

      helper.unmount();
    });

    it("should callback the add function when add new rule is clicked", () => {
      const modal = new TestSecretConfigModal(stream(secretConfigs),
                                              SecretConfig.fromJSON(secretConfigTestData()),
                                              pluginInfos,
                                              onSuccessfulSave);
      helper.mount(modal.body.bind(modal));

      expect(helper.findByDataTestId("rules-table-row").length).toBe(2);

      const addRuleButton = helper.findByDataTestId("add-rule-button")[0];
      simulateEvent.simulate(addRuleButton, "click");

      expect(helper.findByDataTestId("rules-table-row").length).toBe(3);

      helper.unmount();
    });
  });
});
