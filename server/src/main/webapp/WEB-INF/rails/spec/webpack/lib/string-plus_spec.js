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
import {mixins as s} from "helpers/string-plus";

describe("caseInsensitiveCompare()", () => {
  it("allows case insensitive sorting", () => {
    expect(["B", "c", "a"].sort(s.caseInsensitiveCompare)).toEqual(["a", "B", "c"]);
  });
});

describe("Should sanitize JSON", () => {
  it("should change the keys of JSON to snake case", () => {
    const input = {
      toSnakeCase: "value",
      nested:      {
        toSnakeCase:   "value",
        arrays:        ["one", "two"],
        objectInArray: [{"key": "value"}, {"key2": "value2"}]
      }
    };

    /* eslint-disable camelcase */
    const expected = {
      to_snake_case: "value",
      nested:        {
        to_snake_case:   "value",
        arrays:          ["one", "two"],
        object_in_array: [{"key": "value"}, {"key_2": "value2"}]
      }
    };
    /* eslint-enable camelcase */

    expect(JSON.parse(JSON.stringify(input, s.snakeCaser))).toEqual(expected);

  });

  it('should ignore the keys that start with __', () => {
    const input = {
      toSnakeCase: "value",
      nested:      {
        __toSnakeCase: "value",
        __arrays:      ["one", "two"],
        objectInArray: [{"key": "value"}, {"key2": "value2"}]
      }
    };

    /* eslint-disable camelcase */
    const expected = {
      to_snake_case: "value",
      nested:        {
        object_in_array: [{"key": "value"}, {"key_2": "value2"}]
      }
    };
    /* eslint-enable camelcase */

    expect(JSON.parse(JSON.stringify(input, s.snakeCaser))).toEqual(expected);
  });
});
