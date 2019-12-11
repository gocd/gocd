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
import {configRepoToSnakeCaseJSON} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import {Permissions} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Configuration, PlainTextValue} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";

describe("Config Repo Serialization", () => {
  it("should serialize configuration properties for JSON plugin", () => {
    const configuration1 = new Configuration("pipeline_pattern", new PlainTextValue("test-value-1"));
    const configuration2 = new Configuration("environment_pattern", new PlainTextValue("test-value-2"));
    const configRepo     = new ConfigRepo("test",
                                          ConfigRepo.JSON_PLUGIN_ID,
                                          new Material("git",
                                                       new GitMaterialAttributes("test",
                                                                                 false,
                                                                                 "https://example.com")),
                                          new Permissions(false, false),
                                          [configuration1, configuration2]);
    const json           = configRepoToSnakeCaseJSON(configRepo);
    expect(json.configuration)
      .toEqual([{key: "pipeline_pattern", value: "test-value-1"}, {key: "environment_pattern", value: "test-value-2"}]);
  });

  it("should serialize configuration properties for YAML plugin", () => {
    const configuration1 = new Configuration("file_pattern", new PlainTextValue("test-value-1"));
    const configRepo     = new ConfigRepo("test",
                                          ConfigRepo.YAML_PLUGIN_ID,
                                          new Material("git",
                                                       new GitMaterialAttributes("test",
                                                                                 false,
                                                                                 "https://example.com")),
                                          new Permissions(false, false),
                                          [configuration1]);
    const json           = configRepoToSnakeCaseJSON(configRepo);
    expect(json.configuration).toEqual([{key: "file_pattern", value: "test-value-1"}]);
  });

  it("should not serialize configuration properties when not present", () => {
    const configRepo = new ConfigRepo("test",
                                      "test",
                                      new Material("git",
                                                   new GitMaterialAttributes("test",
                                                                             false,
                                                                             "https://example.com")));
    const json       = configRepoToSnakeCaseJSON(configRepo);
    expect(json.configuration).toHaveLength(0);
  });
});
