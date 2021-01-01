/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {newSecretConfig, secretConfigsTestData, secretConfigTestData} from "models/secret_configs/spec/test_data";

describe("SecretConfigsModelSpec", () => {

  it("should deserialize secret config json", () => {
    const secretConfigJson = secretConfigTestData();

    const secretConfig = SecretConfig.fromJSON(secretConfigJson);

    expect(secretConfig.id()).toEqual(secretConfigJson.id);
    expect(secretConfig.description()).toEqual(secretConfigJson.description);
    expect(secretConfig.pluginId()).toEqual(secretConfigJson.plugin_id);
    expect(secretConfig.properties().count()).toEqual(secretConfigJson.properties.length);
    expect(secretConfig.rules()).toHaveLength(secretConfigJson.rules.length);
  });

  it("should deserialize secret configs json", () => {
    const responseData              = secretConfigsTestData();
    const secretConfigsFromResponse = responseData._embedded.secret_configs;

    const secretConfigs = SecretConfigs.fromJSON(responseData);

    expect(secretConfigs).toHaveLength(2);

    expect(secretConfigs[0]().id()).toEqual(secretConfigsFromResponse[0].id);
    expect(secretConfigs[0]().description()).toEqual(secretConfigsFromResponse[0].description);
    expect(secretConfigs[0]().pluginId()).toEqual(secretConfigsFromResponse[0].plugin_id);
    expect(secretConfigs[0]().properties().count()).toEqual(secretConfigsFromResponse[0].properties.length);
    expect(secretConfigs[0]().rules()).toHaveLength(secretConfigsFromResponse[0].rules.length);

    expect(secretConfigs[1]().id()).toEqual(secretConfigsFromResponse[1].id);
    expect(secretConfigs[1]().description()).toEqual(secretConfigsFromResponse[1].description);
    expect(secretConfigs[1]().pluginId()).toEqual(secretConfigsFromResponse[1].plugin_id);
    expect(secretConfigs[1]().properties().count()).toEqual(secretConfigsFromResponse[1].properties.length);
    expect(secretConfigs[1]().rules()).toHaveLength(secretConfigsFromResponse[1].rules.length);
  });

  describe("Validate", () => {
    it("should validate presence of id", () => {
      const secretConfigJson = secretConfigTestData();
      // @ts-ignore
      delete secretConfigJson.id;
      const secretConfig = SecretConfig.fromJSON(secretConfigJson);
      secretConfig.isValid();

      const errors = secretConfig.errors();
      expect(errors.hasErrors()).toBeTruthy();
      expect(errors.errorsForDisplay("id")).toEqual("Id must be present.");
    });

    it("should validate format of id", () => {
      const secretConfig = SecretConfig.fromJSON(newSecretConfig("id with spaces not allowed"));

      secretConfig.isValid();

      const errors = secretConfig.errors();
      expect(errors.hasErrors()).toBeTruthy();
      expect(errors.errorsForDisplay("id"))
        .toEqual(
          "Invalid id. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    });

    it("should validate presence of plugin id", () => {
      const secretConfigJson = secretConfigTestData();
      // @ts-ignore
      delete secretConfigJson.plugin_id;
      const secretConfig = SecretConfig.fromJSON(secretConfigJson);
      secretConfig.isValid();

      const errors = secretConfig.errors();
      expect(errors.hasErrors()).toBeTruthy();
      expect(errors.errorsForDisplay("pluginId")).toEqual("Plugin id must be present.");
    });
  });
});
