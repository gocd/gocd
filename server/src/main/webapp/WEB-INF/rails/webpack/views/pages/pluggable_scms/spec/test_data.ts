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

import {ScmJSON} from "models/materials/pluggable_scm";

export function getPluggableScm() {
  return {
    id:              "scm-id",
    name:            "pluggable.scm.material.name",
    plugin_metadata: {
      id:      "github.pr",
      version: "1"
    },
    auto_update:     true,
    configuration:   [
      {
        key:   "url",
        value: "https://github.com/sample/example.git"
      }
    ]
  } as ScmJSON;
}
