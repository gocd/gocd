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

import {IGNORED_MATERIAL_ATTRIBUTES} from "models/config_repos/types";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

describe("Config Repo Types", () => {
  it("should ignore attributes that are in the ValidatableMixin", () => {
    const properties = Object.getOwnPropertyNames(new ValidatableMixin());
    expect(IGNORED_MATERIAL_ATTRIBUTES).toEqual(properties);
  });
});
