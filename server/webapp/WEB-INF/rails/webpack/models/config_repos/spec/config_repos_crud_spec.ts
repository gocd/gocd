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

import {toSnakeCaseJSON} from "models/config_repos/config_repos_crud";
import {ConfigRepo, GitMaterialAttributes, Material} from "models/config_repos/types";
import {Configuration, PlainTextValue} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";

describe("Serialization", () => {
  it("should serialize configuration properties", () => {
    const configuration1 = new Configuration("test-key-1", new PlainTextValue("test-value-1"));
    const configuration2 = new Configuration("test-key-2", new PlainTextValue("test-value-2"));
    const configRepo     = new ConfigRepo("test",
                                          "test",
                                          new Material("git",
                                                       new GitMaterialAttributes("test",
                                                                                 false,
                                                                                 "https://example.com")),
                                          [configuration1, configuration2]);
    const json           = toSnakeCaseJSON(configRepo);
    expect(json.configuration)
      .toEqual([{key: "test-key-1", value: "test-value-1"}, {key: "test-key-2", value: "test-value-2"}]);
  });

  it("should not serialize configuration properties when not present", () => {
    const configRepo     = new ConfigRepo("test",
                                          "test",
                                          new Material("git",
                                                       new GitMaterialAttributes("test",
                                                                                 false,
                                                                                 "https://example.com")));
    const json           = toSnakeCaseJSON(configRepo);
    expect(json.configuration).toHaveLength(0);
  });
});
