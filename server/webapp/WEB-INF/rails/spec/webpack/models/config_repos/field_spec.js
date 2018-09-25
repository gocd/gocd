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

const Field = require("models/config_repos/field");

describe("Config Repo Field", () => {
  it("getter and setter for a key", () => {
    function TestObj() {
      Field.call(this, "name");
    }

    const o = new TestObj();
    expect(typeof o.name).toBe("function");

    o.name("hello");
    expect(o.name()).toBe("hello");

    o.name("world");
    expect(o.name()).toBe("world");

    expect(o.keys).toEqual(["name"]);
  });

  it("should set options on fields", () => {
    function TestObj() {
      Field.call(this, "name", {display: "Great name"});
      Field.call(this, "password", {type: "secret"});
    }
    const o = new TestObj();
    expect(o.name.display).toBe("Great name");
    expect(o.name.type).toBe("text"); // default option

    expect(o.password.display).toBe("Password"); // default option
    expect(o.password.type).toBe("secret");
  });

  it("init() honors default values", () => {
    function TestObj() {
      Field.call(this, "name", {default: "chewbacca"});
      Field.call(this, "ok", {default: true, type: "boolean"});
    }

    const o = new TestObj();
    expect(o.name.init(null)).toBe("chewbacca");
    expect(o.name.init(undefined)).toBe("chewbacca");
    expect(o.name.init("hi")).toBe("hi");
    expect(o.name.init("")).toBe("");

    expect(o.ok.init(null)).toBe(true);
    expect(o.ok.init(undefined)).toBe(true);
    expect(o.ok.init(false)).toBe(false);
  });

  it("normalizes values according to the specified type", () => {
    function TestObj() {
      Field.call(this, "name");
      Field.call(this, "ok", {type: "boolean"});
    }

    const o = new TestObj();

    expect(o.name(null)).toBe("");
    expect(o.name(undefined)).toBe("");
    expect(o.name(5)).toBe("5");
    expect(o.name("    hi    ")).toBe("hi");

    expect(o.ok(0)).toBe(false);
    expect(o.ok(1)).toBe(true);
  });
});
