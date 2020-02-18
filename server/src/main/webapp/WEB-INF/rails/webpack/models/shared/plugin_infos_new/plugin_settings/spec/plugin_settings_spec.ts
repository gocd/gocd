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
import {Configuration, EncryptedValue, PlainTextValue, PluginSettings} from "../plugin_settings";

describe("Plugin Settings", () => {

  const links = {
    _links: {
      self: {
        href: "http://localhost:8153/go/api/admin/plugin_settings/com.thoughtworks.gocd.test"
      },
      doc: {
        href: "https://api.gocd.org/#plugin-settings"
      },
      find: {
        href: "http://localhost:8153/go/api/admin/plugin_settings/:plugin_id"
      }
    }
  };

  const jsonWithoutLinks = {
    plugin_id: "com.thoughtworks.gocd.test",
    configuration: [
      {
        key: "GoServerUrl",
        value: "http://localhost:8154/go"
      },
      {
        errors: {
          accessKeyId: [
            "Unable to load credentials from any provider in the chain"
          ]
        },
        key: "accessKeyId"
      },
      {
        key: "region"
      },
      {
        key: "secret",
        encrypted_value: "hidden-value"
      }
    ]
  };

  const json = {
    ...links,
    ...jsonWithoutLinks
  };

  it("should deserialize plugin settings", () => {
    const configuration          = [
      new Configuration("GoServerUrl", new PlainTextValue("http://localhost:8154/go")),
      new Configuration("accessKeyId", new PlainTextValue(""), ["Unable to load credentials from any provider in the chain"]),
      new Configuration("region", new PlainTextValue("")),
      new Configuration("secret", new EncryptedValue("hidden-value"))
    ];
    const expectedPluginSettings = new PluginSettings("com.thoughtworks.gocd.test", configuration);
    const pluginSettings         = PluginSettings.fromJSON(json);
    expect(pluginSettings).toEqual(expectedPluginSettings);
  });

  it("should serialize plugin settings", () => {
    const configuration  = [
      new Configuration("GoServerUrl", new PlainTextValue("http://localhost:8154/go")),
      new Configuration("accessKeyId", new PlainTextValue(""), ["Unable to load credentials from any provider in the chain"]),
      new Configuration("region", new PlainTextValue("")),
      new Configuration("secret", new EncryptedValue("hidden-value"))
    ];
    const pluginSettings = new PluginSettings("com.thoughtworks.gocd.test", configuration);
    expect(pluginSettings.toJSON()).toEqual({
      plugin_id: "com.thoughtworks.gocd.test",
      configuration: [
        {
          key: "GoServerUrl",
          value: "http://localhost:8154/go"
        },
        {
          key: "accessKeyId",
          value: "",
        },
        {
          key: "region",
          value: "",
        },
        {
          key: "secret",
          encrypted_value: "hidden-value"
        }
      ]
    });
  });

  it("should send 'value' as the key when encrypted_value has changed", () => {
    const configuration  = [
      new Configuration("secret", new EncryptedValue("hidden-value"))
    ];
    configuration[0].value = "changed-value";
    const pluginSettings = new PluginSettings("com.thoughtworks.gocd.test", configuration);
    expect(pluginSettings.toJSON()).toEqual({
      plugin_id: "com.thoughtworks.gocd.test",
      configuration: [
        {
          key: "secret",
          value: "changed-value"
        }
      ]
    });
  });

  it("should send 'encrypted_value' as the key when the value hasn't changed, even if the setter is called", () => {
    const configuration  = [
      new Configuration("secret", new EncryptedValue("hidden-value"))
    ];
    const pluginSettings = new PluginSettings("com.thoughtworks.gocd.test", configuration);
    pluginSettings.configuration[0].value = "hidden-value";
    expect(pluginSettings.toJSON()).toEqual({
      plugin_id: "com.thoughtworks.gocd.test",
      configuration: [
        {
          key: "secret",
          encrypted_value: "hidden-value"
        }
      ]
    });
  });
});
