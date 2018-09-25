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

const Materials = require("models/config_repos/materials");

describe("Config Repo Materials", () => {
  it("throws error on unknown material type", () => {
    expect(() => Materials.get("made up type", {})).toThrow(new Error("Unknown material type: made up type"));
  });

  it("should return type requested with appropriate data set", () => {
    const actual = Materials.get("hg", {
      material: {
        attributes: {
          "name": "My Material",
          "url": "https://fakeurl.com",
          "auto_update": false
        }
      }
    });

    const expected = { "name": "My Material", "url": "https://fakeurl.com", "auto_update": false };
    expect(actual.toJSON()).toEqual(expected);
  });

  it("instance clone() creates a deep copy", () => {
    const original = Materials.get("hg", {
      material: {
        attributes: {
          "url": "https://fakeurl.com"
        }
      }
    });

    const copy = original.clone();

    expect(original !== copy).toBe(true);
    expect(original.toJSON()).toEqual(copy.toJSON());

    copy.url("https://realurl.com");
    expect(original.url()).toBe("https://fakeurl.com");
    expect(copy.url()).toBe("https://realurl.com");
  });

  it("should use default values when value is not set", () => {

  });

});
