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

import {RuleJSON} from "models/rules/rules";

export function newSecretConfig(id = "secrets_id", pluginId = "cd.go.secrets.file") {
  return {
    id,
    description: "This is used to lookup for secrets for the team X.",
    plugin_id: pluginId,
    properties: [
      {
        key: "secrets_file_path",
        value: "/home/secret/secret.dat"
      },
      {
        key: "cipher_file_path",
        value: "/home/secret/secret-key.aes"
      },
      {
        key: "secret_password",
        encrypted_value: "0a3ecba2e196f73d07b361398cc9d08b"
      }
    ],
    rules: [
      {
        directive: "allow",
        action: "refer",
        type: "pipeline_group",
        resource: "DeployPipelines"
      },
      {
        directive: "deny",
        action: "refer",
        type: "pipeline_group",
        resource: "TestPipelines"
      }
    ] as RuleJSON[]
  };
}

export function secretConfigTestData() {
  return newSecretConfig();
}

export function secretConfigsTestData() {
  return {
    _embedded: {
      secret_configs: [
        newSecretConfig("file", "cd.go.secrets.file"),
        newSecretConfig("aws", "cd.go.secrets.aws")
      ]
    }
  };
}
