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

import m from "mithril";
import Stream from "mithril/stream";
import {SecretConfigs} from "models/secret_configs/secret_configs";
import {secretConfigsTestData} from "models/secret_configs/spec/test_data";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {SecretPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import {SecretConfigsWidget} from "views/pages/secret_configs/secret_configs_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("SecretConfigsWidget", () => {
  const helper   = new TestHelper();
  const onEdit   = jasmine.createSpy("onEdit");
  const onClone  = jasmine.createSpy("onClone");
  const onDelete = jasmine.createSpy("onDelete");

  const secretConfigs = SecretConfigs.fromJSON(secretConfigsTestData());
  const pluginInfos   = [PluginInfo.fromJSON(SecretPluginInfo.file())];

  afterEach((done) => helper.unmount(done));

  it("should show flash message when no secret plugin installed", () => {
    mount([], []);

    expect(helper.findByDataTestId("flash-message-info")).toBeInDOM();
    expect(helper.findByDataTestId("flash-message-info").text()).toEqual("No secret plugin installed.");
  });

  it("should render action buttons", () => {
    mount(secretConfigs, pluginInfos);

    const groups = helper.findByDataTestId("secret-configs-group");
    expect(helper.findIn(groups.eq(0), "secret-config-edit")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "secret-config-clone")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "secret-config-delete")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "secret-config-edit")).not.toBeDisabled();
    expect(helper.findIn(groups.eq(0), "secret-config-clone")).not.toBeDisabled();
    expect(helper.findIn(groups.eq(0), "secret-config-delete")).not.toBeDisabled();
  });

  it("should disable edit and clone button when plugin is not installed", () => {
    mount(secretConfigs, []);
    const groups = helper.findByDataTestId("secret-configs-group");

    expect(helper.findIn(groups.eq(0), "secret-config-edit")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "secret-config-clone")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "secret-config-delete")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "secret-config-edit")).toBeDisabled();
    expect(helper.findIn(groups.eq(0), "secret-config-clone")).toBeDisabled();
    expect(helper.findIn(groups.eq(0), "secret-config-delete")).not.toBeDisabled();
  });

  it("should render secret config properties", () => {
    mount(secretConfigs, pluginInfos);

    const groups = helper.findByDataTestId("secret-configs-group");

    expect(helper.findIn(groups.eq(0), "key-value-key-secrets-file-path")).toContainText("secrets_file_path");
    expect(helper.findIn(groups.eq(0), "key-value-value-secrets-file-path")).toContainText("/home/secret/secret.dat");

    expect(helper.findIn(groups.eq(0), "key-value-key-cipher-file-path")).toContainText("cipher_file_path");
    expect(helper.findIn(groups.eq(0), "key-value-value-cipher-file-path"))
      .toContainText("/home/secret/secret-key.aes");

    expect(helper.findIn(groups.eq(0), "key-value-key-secret-password")).toContainText("secret_password");
    expect(helper.findIn(groups.eq(0), "key-value-value-secret-password")).toContainText("********");
  });

  it("should callback the edit function when edit button is clicked", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.findByDataTestId("secret-configs-group");
    simulateEvent.simulate(helper.findIn(groups.eq(0), "secret-config-edit").get(0), "click");

    expect(onEdit).toHaveBeenCalledWith(secretConfigs[0](), jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.findByDataTestId("secret-configs-group");
    simulateEvent.simulate(helper.findIn(groups.eq(0), "secret-config-clone").get(0), "click");

    expect(onClone).toHaveBeenCalledWith(secretConfigs[0](), jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.findByDataTestId("secret-configs-group");
    simulateEvent.simulate(helper.findIn(groups.eq(0), "secret-config-delete").get(0), "click");

    expect(onDelete).toHaveBeenCalledWith(secretConfigs[0](), jasmine.any(Event));
  });

  it("should list secret configs", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.findByDataTestId("secret-configs-group");

    expect(groups.length).toEqual(2);
    expect(helper.findByDataTestId("secret-config-description").eq(0).text())
      .toEqual("This is used to lookup for secrets for the team X.");
    expect(helper.findIn(groups.eq(0), "key-value-value-plugin-id").text()).toEqual("cd.go.secrets.file");
    expect(helper.findIn(groups.eq(0), "key-value-value-id").text()).toEqual("file");
    expect(helper.findIn(groups.eq(1), "key-value-value-plugin-id").text()).toBe("cd.go.secrets.aws");
    expect(helper.findIn(groups.eq(1), "key-value-value-id").text()).toEqual("aws");

  });

  it("should list rules information for secrets", () => {
    mount(secretConfigs, pluginInfos);
    const table = helper.findByDataTestId("rule-table").eq(0);

    const headerRow = helper.findIn(table, "table-header").find("th");
    expect(headerRow.length).toEqual(3);
    expect(headerRow.eq(0)).toContainText("Directive");
    expect(headerRow.eq(1)).toContainText("Type");
    expect(headerRow.eq(2)).toContainText("Resource");

    const ruleBodyRow = helper.findIn(table, "table-body").find("td");
    expect(ruleBodyRow.eq(0)).toContainText("allow");
    expect(ruleBodyRow.eq(1)).toContainText("pipeline_group");
    expect(ruleBodyRow.eq(2)).toContainText("DeployPipelines");

    expect(ruleBodyRow.eq(3)).toContainText("deny");
    expect(ruleBodyRow.eq(4)).toContainText("pipeline_group");
    expect(ruleBodyRow.eq(5)).toContainText("TestPipelines");
  });

  it("should display info when no secret configs are present", () => {
    mount([], pluginInfos);
    const groups = helper.findByDataTestId("secret-configs-group");

    expect(groups).not.toBeInDOM();
    expect(helper.findByDataTestId("secret-config-info")).toBeInDOM();
    expect(helper.findByDataTestId("secret-config-info").text())
      .toBe(
        "Click on \"Add\" to add new secret configuration.A secret configuration can be used to access secrets from a secret management store.");

  });

  function mount(secretConfigs: SecretConfigs, pluginInfos: Array<PluginInfo<any>>) {
    helper.mount(() => <SecretConfigsWidget pluginInfos={Stream(pluginInfos)}
                                            secretConfigs={Stream(secretConfigs)}
                                            onEdit={onEdit}
                                            onClone={onClone}
                                            onDelete={onDelete}/>);
  }
});
