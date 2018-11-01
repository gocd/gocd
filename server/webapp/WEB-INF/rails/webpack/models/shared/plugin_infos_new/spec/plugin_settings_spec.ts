/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {Configuration, PluginSettings} from "../plugin_settings";

describe('Plugin Settings', () => {

  const json: any = {
    message: "Save failed. There are errors in the plugin settings. Please fix them and resubmit.",
    data:    {
      _links:        {
        self: {
          href: "http://localhost:8153/go/api/admin/plugin_settings/com.thoughtworks.gocd.test"
        },
        doc:  {
          href: "https://api.gocd.org/#plugin-settings"
        },
        find: {
          href: "http://localhost:8153/go/api/admin/plugin_settings/:plugin_id"
        }
      },
      plugin_id:     "com.thoughtworks.gocd.test",
      configuration: [
        {
          key:   "GoServerUrl",
          value: "http://localhost:8154/go"
        },
        {
          errors: {
            accessKeyId: [
              "Unable to load credentials from any provider in the chain"
            ]
          },
          key:    "accessKeyId"
        },
        {
          key: "region"
        }
      ]
    }
  };

  it('should deserialize plugin settings', () => {
    const configuration          = [
      new Configuration("GoServerUrl", "http://localhost:8154/go"),
      new Configuration("accessKeyId", undefined, ["Unable to load credentials from any provider in the chain"]),
      new Configuration("region")
    ];
    const expectedPluginSettings = new PluginSettings("com.thoughtworks.gocd.test", configuration);
    expect(PluginSettings.fromJSON(json)).toEqual(expectedPluginSettings);
  });
});
