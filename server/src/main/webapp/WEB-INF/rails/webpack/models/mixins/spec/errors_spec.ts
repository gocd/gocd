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

import {Errors} from "models/mixins/errors";

describe("Errors", () => {
  it("should clone errors", () => {
    const errors       = new Errors({name: ["should not be blank"], foo: ["bar"]});
    const clonedErrors = errors.clone();

    expect(errors).not.toBe(clonedErrors);
    expect(errors.errors()).toEqual({name: ["should not be blank"], foo: ["bar"]});
  });

  it("should add errors", () => {
    const errors = new Errors({name: ["should not be blank"]});

    expect(errors.count()).toBe(1);

    errors.add("foo", "bar");

    expect(errors.count()).toBe(2);
    expect(errors.errors("foo")).toEqual(["bar"]);
  });

  it("should clear errors", () => {
    const errors = new Errors({name: ["should not be blank"], foo: ["bar"]});

    expect(errors.count()).toBe(2);

    errors.clear("name");

    expect(errors.count()).toBe(1);
    expect(errors.errors("name")).toBeUndefined();
  });

  it("should clear all errors if attribute name is not passed", () => {
    const errors = new Errors({name: ["should not be blank"], foo: ["bar"]});

    expect(errors.count()).toBe(2);

    errors.clear();

    expect(errors.count()).toBe(0);
  });

  it("should respond to `hasErrors`", () => {
    const errors = new Errors();

    expect(errors.hasErrors()).toBe(false);

    errors.add("foo", "bar");

    expect(errors.hasErrors()).toBe(true);
  });

  it("should respond to `keys`", () => {
    const errors = new Errors({name: ["should not be blank"], foo: ["bar"]});

    expect(errors.keys()).toEqual(["name", "foo"]);
  });

  it("should respond to `errorsForDisplay`", () => {
    const errors = new Errors({name: ["should not be blank", "bar"]});

    expect(errors.errorsForDisplay("name")).toEqual("should not be blank. bar.");
  });
});
