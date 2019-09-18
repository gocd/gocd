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

export default {
  environment_variable_json: environmentVariableJson,
};
