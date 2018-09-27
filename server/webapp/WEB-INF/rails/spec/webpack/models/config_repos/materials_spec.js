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

  it("serializes to plain object with only declared attributes", () => {
    const o = Materials.get("hg", {});
    o.name("merc");
    o.url("repo.hg");

    expect(o.toJSON()).toEqual({name: "merc", url: "repo.hg", auto_update: true});
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
    const mat = Materials.get("git", {});
    expect(mat.branch()).toBe("master");
    expect(mat.auto_update()).toBe(true);
  });

  it("sets up validation rules based on field configuration", () => {
    const Field = Materials.Field, Common = Materials.Common;

    function CustomMaterial(data) {
      Field.call(this, "id", {required: true});
      Field.call(this, "hostname", {required: true, format: /^[a-z0-9]+(\.[a-z0-9]+)*$/i});
      Field.call(this, "optional", {required: false});

      Common.call(this, data);
    }

    const o = new CustomMaterial({});
    expect(o.isValid()).toBe(false);

    expect(o.errors().errorsForDisplay("id")).toBe("Id must be present.");
    expect(o.errors().errorsForDisplay("hostname")).toBe("Hostname must be present.");
    expect(o.errors().hasErrors("optional")).toBe(false);

    o.id("123");
    o.validate("id");

    expect(o.errors().hasErrors("id")).toBe(false);

    o.hostname("boom!");
    o.validate("hostname");
    expect(o.errors().errorsForDisplay("hostname")).toBe("Hostname format is invalid.");

    o.hostname("foo.bar.com");

    expect(o.isValid()).toBe(true);
    expect(o.errors().hasErrors()).toBe(false);
  });
});
