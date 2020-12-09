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

import {cascading, pipeline} from "../utils";

describe("Utils", () => {
  describe("cascading()", () => {
    it("fails over until the criterion is satisfied", () => {
      const provider = cascading((s) => s.startsWith("m"),
        () => "nope",
        () => "maybe",
        () => "shouldn't get here"
      );

      expect(provider()).toBe("maybe");
    });

    it("chooses the initial provider if it meets the criterion", () => {
      const provider = cascading((s) => !!s.length,
        () => "here",
        () => "you'll never see me",
        () => "shouldn't be possible!"
      );

      expect(provider()).toBe("here");
    });

    it("defaults to the last value if nothing meets the criterion", () => {
      const provider = cascading((s) => !s.length,
        () => "nope",
        () => "sorry",
        () => "it's the best I could do"
      );

      expect(provider()).toBe("it's the best I could do");
    });

    it("uses the initial provider if no alternatives are provided", () => {
      // a logical subset of the default case, but worth explicitly stating as a logical
      // requirement in a test
      const provider = cascading((s) => !s.length, () => "oh well");

      expect(provider()).toBe("oh well");
    });
  });

  describe("pipeline()", () => {
    it("transforms input sequentially", () => {
      expect(pipeline("     yes    ",
        (v) => v.trim(),
        (v) => v.toUpperCase(),
        (v) => v.replace(/[AEIOU]+/g, "_")
      )).toBe("Y_S");
    });

    it("can transform input to another type", () => {
      expect(pipeline<any>({ a: "beef", b: "dead"}, // obj
        (v) => v.b, // string "dead"
        (v) => parseInt(v, 16), // hexadecimal number => 57005
        (v) => v % 7, // number => 4
        (v) => "abcdefg".split("").slice(v) // array subset: ["e", "f", "g"]
      )).toEqual(["e", "f", "g"]);
    });

    it("returns the initial value when there are no transforms", () => {
      expect(pipeline("hello")).toBe("hello");
    });
  });
});
