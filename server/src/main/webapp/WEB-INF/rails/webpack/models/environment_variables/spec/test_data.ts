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
import data from "models/origin/spec/test_data";

function randomString(): string {
  return Math.random().toString(36).slice(2);
}

function environmentVariableJson(secure: boolean = false) {
  return {
    name: `env-var-name-${randomString()}`,
    value: secure ? undefined : `env-var-value-${randomString()}`,
    encrypted_value: secure ? `env-var-secure-value-${randomString()}` : undefined,
    secure
  };
}

function environmentVariableWithOriginInXmlJson(secure: boolean = false) {
  return {
    name: `env-var-name-${randomString()}`,
    value: secure ? undefined : `env-var-value-${randomString()}`,
    encrypted_value: secure ? `env-var-secure-value-${randomString()}` : undefined,
    origin: data.file_origin(),
    secure
  };
}

function environmentVariableWithOriginConfigRepoJson(secure: boolean = false) {
  return {
    name: `env-var-name-${randomString()}`,
    value: secure ? undefined : `env-var-value-${randomString()}`,
    encrypted_value: secure ? `env-var-secure-value-${randomString()}` : undefined,
    origin: data.config_repo_origin(),
    secure
  };
}

export default {
  environment_variable_json: environmentVariableJson,
  environment_variable_with_origin_xml_json: environmentVariableWithOriginInXmlJson,
  environment_variable_with_origin_config_repo_json: environmentVariableWithOriginConfigRepoJson,
};
