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

import {Origin} from "models/origin";
import data from "models/origin/spec/test_data";

describe("Origin", () => {
  const xmlOriginJSON        = data.file_origin();
  const configRepoOriginJSON = data.config_repo_origin();

  it("should deserialize from json", () => {
    const xmlOrigin        = Origin.fromJSON(xmlOriginJSON);
    const configRepoOrigin = Origin.fromJSON(configRepoOriginJSON);

    expect(xmlOrigin.type()).toEqual(xmlOriginJSON.type);
    expect(configRepoOrigin.type()).toEqual(configRepoOriginJSON.type);
    expect(configRepoOrigin.id()).toEqual(configRepoOriginJSON.id);
  });

  it("should answer whether a the origin is defined in config repository", () => {
    const xmlOrigin        = Origin.fromJSON(xmlOriginJSON);
    const configRepoOrigin = Origin.fromJSON(configRepoOriginJSON);

    expect(xmlOrigin.isDefinedInConfigRepo()).toEqual(false);
    expect(configRepoOrigin.isDefinedInConfigRepo()).toEqual(true);
  });
});
