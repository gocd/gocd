/*
 * Copyright 2015 ThoughtWorks, Inc.
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
define(["string-plus"], function (s) {
  beforeEach(function () {
  });

  describe("Should sanitize JSON", function () {
    it("should change the keys of JSON to snake case", function () {
      var input = {
        toSnakeCase: "value",
        nested:      {
          toSnakeCase:   "value",
          arrays:        ["one", "two"],
          objectInArray: [{"key": "value"}, {"key2": "value2"}]
        }
      }

      var expected = {
        to_snake_case: "value",
        nested:        {
          to_snake_case: "value",
          arrays:        ["one", "two"],
          object_in_array: [{"key": "value"}, {"key_2": "value2"}]
        }
      }

      expect(JSON.parse(JSON.stringify(input, s.snakeCaser))).toEqual(expected);

    });
  });
});
