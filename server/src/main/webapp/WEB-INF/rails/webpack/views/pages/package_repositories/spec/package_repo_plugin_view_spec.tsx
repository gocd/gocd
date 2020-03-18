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
import {PlainTextValue} from "models/shared/config_value";
import {Configuration, Configurations} from "models/shared/configuration";
import {PluginSettings} from "models/shared/plugin_infos_new/extensions";
import {PluginView} from "views/pages/package_repositories/package_repo_plugin_view";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Package Repo Plugin View", () => {
  const helper         = new TestHelper();
  const pluginSettings = PluginSettings.fromJSON(pluginSettingsJSON());

  afterEach((done) => helper.unmount(done));

  function mount(configurations: Configurations) {
    helper.mount(() => <PluginView pluginSettings={pluginSettings} configurations={configurations}/>);
  }

  it("should render input field for each configuration", () => {
    mount(new Configurations([]));

    expect(helper.byTestId("form-field-label-package-id")).toBeInDOM();
    expect(helper.byTestId("input-for-Package Id")).toBeInDOM();
    expect((helper.byTestId("input-for-Package Id") as HTMLInputElement).type).toBe("text");

    expect(helper.byTestId("form-field-label-version-to-poll")).toBeInDOM();
    expect(helper.byTestId("input-for-Version to poll >=")).toBeInDOM();
    expect((helper.byTestId("input-for-Version to poll >=") as HTMLInputElement).type).toBe("password");
  });

  it("should create configuration field when one does not exists", () => {
    const configurations = new Configurations([]);

    expect(configurations.allConfigurations()).toHaveLength(0);

    mount(configurations);

    expect(configurations.allConfigurations()).toHaveLength(2);
    expect(configurations.findConfiguration("PACKAGE_ID")).not.toBeFalsy();
    expect(configurations.findConfiguration("POLL_VERSION_FROM")).not.toBeFalsy();
  });

  it("should bind the plain text field view with the configuration", () => {
    const configurations = new Configurations([]);

    mount(configurations);

    expect(configurations.findConfiguration("PACKAGE_ID")!.getValue()).toBe("");

    helper.oninput(helper.byTestId("input-for-Package Id"), "package-id");

    expect(helper.byTestId("input-for-Package Id")).toHaveValue("package-id");
    expect(configurations.findConfiguration("PACKAGE_ID")!.getValue()).toBe("package-id");
  });

  it("should bind the secure text field view with the configuration", () => {
    const configurations = new Configurations([]);

    mount(configurations);

    expect(configurations.findConfiguration("POLL_VERSION_FROM")!.getValue()).toBe("");

    helper.oninput(helper.byTestId("input-for-Version to poll >="), "package-version");

    expect((helper.byTestId("input-for-Version to poll >=") as HTMLInputElement)).toHaveValue("package-version");
    expect(configurations.findConfiguration("POLL_VERSION_FROM")!.getValue()).toBe("package-version");
  });

  it("should render configuration value when one exists", () => {
    const configurations = new Configurations([]);
    configurations.setConfiguration("PACKAGE_ID", "my-package-id");
    configurations.setConfiguration("POLL_VERSION_FROM", "some-package-version");

    mount(configurations);

    expect(helper.byTestId("input-for-Package Id")).toHaveValue("my-package-id");
    expect((helper.byTestId("input-for-Version to poll >=") as HTMLInputElement)).toHaveValue("some-package-version");
  });

  it("should allow updating existing plain text configuration value", () => {
    const configurations = new Configurations([]);
    configurations.setConfiguration("PACKAGE_ID", "my-package-id");

    mount(configurations);

    expect(helper.byTestId("input-for-Package Id")).toHaveValue("my-package-id");

    helper.oninput(helper.byTestId("input-for-Package Id"), "package-id");

    expect(helper.byTestId("input-for-Package Id")).toHaveValue("package-id");
    expect(configurations.findConfiguration("PACKAGE_ID")!.getValue()).toBe("package-id");
  });

  it("should allow updating existing secure text configuration value", () => {
    const configurations = new Configurations([]);
    configurations.setConfiguration("POLL_VERSION_FROM", "some-package-version");

    mount(configurations);

    expect((helper.byTestId("input-for-Version to poll >=") as HTMLInputElement)).toHaveValue("some-package-version");

    helper.oninput(helper.byTestId("input-for-Version to poll >="), "package-version");

    expect((helper.byTestId("input-for-Version to poll >=") as HTMLInputElement)).toHaveValue("package-version");
    expect(configurations.findConfiguration("POLL_VERSION_FROM")!.getValue()).toBe("package-version");
  });

  it("should render errors associated with the field", () => {
    const configurations = new Configurations([new Configuration("PACKAGE_ID", new PlainTextValue("repo-1"), ["Boom!"])]);
    mount(configurations);

    expect(helper.qa("span", helper.byTestId("plugin-view"))[1].innerText).toBe("Boom!")
  });

  function pluginSettingsJSON() {
    return {
      configurations: [
        {
          key: "PACKAGE_ID",
          metadata: {
            secure: false,
            required: true,
            part_of_identity: true,
            display_name: "Package Id",
            display_order: 0
          }
        },
        {
          key: "POLL_VERSION_FROM",
          metadata: {
            secure: true,
            required: false,
            part_of_identity: false,
            display_name: "Version to poll >=",
            display_order: 1
          }
        }
      ]
    };
  }

});
