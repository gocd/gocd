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
import {FilterValidations} from "views/dashboard/models/filter_validations";

describe("FilterValidations", () => {
  it("should accept valid name", () => {
    const obj = validatable({ name: () => "a new tab" });

    obj.validate("name");
    expect(obj.errors().hasErrors()).toBe(false);
  });

  it("validates name is required", () => {
    const obj = validatable({ name: () => "" });

    obj.validate("name");
    expect(obj.errors().hasErrors()).toBe(true);
    expect(obj.errors().errorsForDisplay("name")).toBe("Name must be present.");
  });

  it("validates name should not contain leading or trailing spaces", () => {
    let name = " foobar";
    const obj = validatable({ name: () => name });

    obj.validate("name");
    expect(obj.errors().hasErrors()).toBe(true);
    expect(obj.errors().errorsForDisplay("name")).toBe("View name must not have leading or trailing whitespace.");

    name = "foobar ";
    obj.validate("name");
    expect(obj.errors().hasErrors()).toBe(true);
    expect(obj.errors().errorsForDisplay("name")).toBe("View name must not have leading or trailing whitespace.");

    name = " foobar ";
    obj.validate("name");
    expect(obj.errors().hasErrors()).toBe(true);
    expect(obj.errors().errorsForDisplay("name")).toBe("View name must not have leading or trailing whitespace.");
  });

  it("validates name should only contain ASCII printable characters", () => {
    const obj = validatable({ name: () => "f\u0101" });

    obj.validate("name");
    expect(obj.errors().hasErrors()).toBe(true);
    expect(obj.errors().errorsForDisplay("name")).toBe("View name is only allowed to contain letters, numbers, spaces, and punctuation marks.");
  });

  it("validates name is unique among views", () => {
    const obj = validatable({ name: () => "a" });

    obj.validate("name");
    expect(obj.errors().hasErrors()).toBe(true);
    expect(obj.errors().errorsForDisplay("name")).toBe("Another view with this name already exists.");
  });

  it("validates modal should have at least one pipeline", () => {
    const obj = validatable({ hasPipelinesSelected: () => false });

    obj.validate("hasPipelinesSelected");
    expect(obj.errors().hasErrors()).toBe(true);
    expect(obj.errors().errorsForDisplay("hasPipelinesSelected")).toBe("At least one pipeline must be selected.");
  });
});

function validatable(obj) {
  FilterValidations.call(obj, { names: () => ["a", "b", "c"] });
  return obj;
}
