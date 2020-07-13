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

import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {SecretConfigs} from "models/secret_configs/secret_configs";
import {secretConfigsTestData} from "models/secret_configs/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {SecretPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {SecretConfigsWidget} from "views/pages/secret_configs/secret_configs_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("SecretConfigsWidget", () => {
  const helper   = new TestHelper();
  const onEdit   = jasmine.createSpy("onEdit");
  const onClone  = jasmine.createSpy("onClone");
  const onDelete = jasmine.createSpy("onDelete");

  const secretConfigs = SecretConfigs.fromJSON(secretConfigsTestData());
  const pluginInfos   = new PluginInfos(PluginInfo.fromJSON(SecretPluginInfo.file()));

  afterEach((done) => helper.unmount(done));

  it("should show flash message when no secret plugin installed", () => {
    mount([], new PluginInfos());

    expect(helper.byTestId("flash-message-info")).toBeInDOM();
    expect(helper.textByTestId("flash-message-info")).toBe("No secret plugin installed.");
  });

  it("should render action buttons", () => {
    mount(secretConfigs, pluginInfos);

    const groups = helper.byTestId("secret-configs-group");
    expect(helper.byTestId("secret-config-edit", groups)).toBeInDOM();
    expect(helper.byTestId("secret-config-clone", groups)).toBeInDOM();
    expect(helper.byTestId("secret-config-delete", groups)).toBeInDOM();
    expect(helper.byTestId("secret-config-edit", groups)).not.toBeDisabled();
    expect(helper.byTestId("secret-config-clone", groups)).not.toBeDisabled();
    expect(helper.byTestId("secret-config-delete", groups)).not.toBeDisabled();
  });

  it("should disable edit and clone button when plugin is not installed", () => {
    mount(secretConfigs, new PluginInfos());

    const groups = helper.byTestId("secret-configs-group");
    expect(helper.byTestId("secret-config-edit", groups)).toBeInDOM();
    expect(helper.byTestId("secret-config-clone", groups)).toBeInDOM();
    expect(helper.byTestId("secret-config-delete", groups)).toBeInDOM();
    expect(helper.byTestId("secret-config-edit", groups)).toBeDisabled();
    expect(helper.byTestId("secret-config-clone", groups)).toBeDisabled();
    expect(helper.byTestId("secret-config-delete", groups)).not.toBeDisabled();
  });

  it("should render secret config properties", () => {
    mount(secretConfigs, pluginInfos);

    const groups = helper.byTestId("secret-configs-group");

    expect(helper.textByTestId("key-value-key-secrets-file-path", groups)).toContain("secrets_file_path");
    expect(helper.textByTestId("key-value-value-secrets-file-path", groups)).toContain("/home/secret/secret.dat");

    expect(helper.textByTestId("key-value-key-cipher-file-path", groups)).toContain("cipher_file_path");
    expect(helper.textByTestId("key-value-value-cipher-file-path", groups)).toContain("/home/secret/secret-key.aes");

    expect(helper.textByTestId("key-value-key-secret-password", groups)).toContain("secret_password");
    expect(helper.textByTestId("key-value-value-secret-password", groups)).toContain("********");
  });

  it("should callback the edit function when edit button is clicked", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.byTestId("secret-configs-group");
    helper.clickByTestId("secret-config-edit", groups);

    expect(onEdit).toHaveBeenCalledWith(secretConfigs[0](), jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.byTestId("secret-configs-group");
    helper.clickByTestId("secret-config-clone", groups);

    expect(onClone).toHaveBeenCalledWith(secretConfigs[0](), jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.byTestId("secret-configs-group");
    helper.clickByTestId("secret-config-delete", groups);

    expect(onDelete).toHaveBeenCalledWith(secretConfigs[0](), jasmine.any(Event));
  });

  it("should list secret configs", () => {
    mount(secretConfigs, pluginInfos);
    const groups = helper.allByTestId("secret-configs-group");

    expect(groups.length).toBe(2);
    expect(helper.textByTestId("secret-config-description")).toBe("This is used to lookup for secrets for the team X.");
    expect(helper.textByTestId("key-value-value-plugin-id", groups.item(0))).toBe("cd.go.secrets.file");
    expect(helper.textByTestId("key-value-value-id", groups.item(0))).toBe("file");
    expect(helper.textByTestId("key-value-value-plugin-id", groups.item(1))).toBe("cd.go.secrets.aws");
    expect(helper.textByTestId("key-value-value-id", groups.item(1))).toBe("aws");

  });

  it("should list rules information for secrets", () => {
    mount(secretConfigs, pluginInfos);

    expect(helper.byTestId('rules-info')).toBeInDOM();
    const values = helper.qa('td', helper.byTestId('rule-table'));
    expect(values.length).toBe(6);

    expect(values[0].textContent).toBe('Allow');
    expect(values[1].textContent).toBe('Pipeline Group');
    expect(values[2].textContent).toBe('DeployPipelines');

    expect(values[3].textContent).toBe('Deny');
    expect(values[4].textContent).toBe('Pipeline Group');
    expect(values[5].textContent).toBe('TestPipelines');
  });

  it("should display info when no secret configs are present", () => {
    mount([], pluginInfos);

    const helpElement = helper.byTestId("secret-config-info");

    expect(helper.byTestId("secret-configs-group")).toBeFalsy();
    expect(helpElement).toBeInDOM();
    expect(helpElement.textContent).toBe("Click on \"Add\" to add new secret configuration.A secret configuration can be used to access secrets from an external secret management store.You can read more about secret configurations from here.");
    expect(helper.q('a', helpElement)).toHaveAttr('href', docsUrl("configuration/secrets_management.html"));
  });

  function mount(secretConfigs: SecretConfigs, pluginInfos: PluginInfos) {
    helper.mount(() => <SecretConfigsWidget pluginInfos={Stream(pluginInfos)}
                                            secretConfigs={Stream(secretConfigs)}
                                            onEdit={onEdit}
                                            onClone={onClone}
                                            onDelete={onDelete}/>);
  }
});
