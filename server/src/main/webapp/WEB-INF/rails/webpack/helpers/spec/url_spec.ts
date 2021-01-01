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

import {normalizePath, queryParamAsString} from "../url";

describe("URL helpers", () => {
  describe("queryParamAsString()", () => {
    it("parses params in any position", () => {
      const search = "?foo=bar&baz=foo&quu=baz";

      expect(queryParamAsString(search, "foo")).toBe("bar");
      expect(queryParamAsString(search, "quu")).toBe("baz");
      expect(queryParamAsString(search, "baz")).toBe("foo");
    });

    it("takes the last value of repeated keys", () => {
      const search = "?baz=one&baz=two&baz=three";

      expect(queryParamAsString(search, "baz")).toBe("three");
    });

    it("handles missing keys", () => {
      const search = "?foo=1";

      expect(queryParamAsString(search, "baz")).toBe("");
    });

    it("handles empty values", () => {
      expect(queryParamAsString("?baz", "baz")).toBe("");
      expect(queryParamAsString("?baz=", "baz")).toBe("");
      expect(queryParamAsString("?", "baz")).toBe("");
      expect(queryParamAsString("&", "baz")).toBe("");
      expect(queryParamAsString("", "baz")).toBe("");
    });

    it("handles numbers", () => {
      expect(queryParamAsString("?foo=0", "foo")).toBe("0"); // falsey values do not collapse to empty string
      expect(queryParamAsString("?foo=1", "foo")).toBe("1");
    });

    it("handles booleans", () => {
      expect(queryParamAsString("?foo=false", "foo")).toBe("false"); // falsey values do not collapse to empty string
      expect(queryParamAsString("?foo=true", "foo")).toBe("true");
    });

    it("handles enumerables", () => {
      expect(queryParamAsString("?foo[]", "foo")).toBe(`[""]`);
      expect(queryParamAsString("?foo[]=", "foo")).toBe(`[""]`);
      expect(queryParamAsString("?foo[0]=&foo[1]=", "foo")).toBe(`["",""]`);
      expect(queryParamAsString("?foo[0]=a&foo[1]=b", "foo")).toBe(`["a","b"]`);
      expect(queryParamAsString("?foo[bar]=baz", "foo")).toBe(`{"bar":"baz"}`);
    });
  });

  describe("normalizePath()", () => {
    it("treats relative paths as absolute", () => {
      expect(normalizePath("./a/b/c/d")).toBe("/a/b/c/d");
      expect(normalizePath("../../a/b/c/d")).toBe("/a/b/c/d");
      expect(normalizePath("a/b/c/d")).toBe("/a/b/c/d");
    });

    it("collapses `./` and `../`", () => {
      expect(normalizePath("/a/b/c/../../d/./e/f/./g/h/i/../j/k/l")).toBe("/a/d/e/f/g/h/j/k/l");
    });

    it("handles trailing `.` and `..`", () => {
      expect(normalizePath("/../../a/b/../e/f/g/..")).toBe("/a/e/f");
    });

    it("denormalizes consecutive `/`", () => {
      expect(normalizePath("/../../a//////////b/../e/f/////////////g/..//")).toBe("/a/e/f");
      expect(normalizePath("/////a//../////../////b/../e/f/////////////g/..//")).toBe("/e/f");
    });

    it("ensures no trailing `/` for non-root paths", () => {
      expect(normalizePath("/a/b/c/")).toBe("/a/b/c");
      expect(normalizePath("/a/b/c//")).toBe("/a/b/c");
      expect(normalizePath("/a/b/c")).toBe("/a/b/c");
    });

    it("works for root pathnames", () => {
      expect(normalizePath("/")).toBe("/");
      expect(normalizePath("..//../")).toBe("/");
      expect(normalizePath("/.//")).toBe("/");
      expect(normalizePath(".")).toBe("/");
      expect(normalizePath("///")).toBe("/");
      expect(normalizePath("..")).toBe("/");
      expect(normalizePath("")).toBe("/");
    });
  });
});
