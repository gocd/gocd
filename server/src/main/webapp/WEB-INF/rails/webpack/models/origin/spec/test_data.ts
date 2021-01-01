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

import {OriginType} from "models/origin/index";

function randomString(): string {
  return Math.random().toString(36).slice(2);
}

function fileOriginJson() {
  return {
    type: OriginType.GoCD,
  };
}

function configRepoOrigin() {
  return {
    type: OriginType.ConfigRepo,
    id: `config-repo-id-${randomString()}`
  };
}

export default {
  file_origin: fileOriginJson,
  config_repo_origin: configRepoOrigin
};
