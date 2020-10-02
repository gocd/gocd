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

import {queryParamAsString} from "../url";

describe("URL helpers", () => {
  it("queryParamAsString() parses params in any position", () => {
    const search = "?foo=bar&baz=foo&quu=baz";

    expect(queryParamAsString(search, "foo")).toBe("bar");
    expect(queryParamAsString(search, "quu")).toBe("baz");
    expect(queryParamAsString(search, "baz")).toBe("foo");
  });

  it("queryParamAsString() takes the last value of repeated keys", () => {
    const search = "?baz=one&baz=two&baz=three";

    expect(queryParamAsString(search, "baz")).toBe("three");
  });

  it("queryParamAsString() handles missing keys", () => {
    const search = "?foo=1";

    expect(queryParamAsString(search, "baz")).toBe("");
  });

  it("queryParamAsString() handles empty values", () => {
    expect(queryParamAsString("?baz", "baz")).toBe("");
    expect(queryParamAsString("?baz=", "baz")).toBe("");
    expect(queryParamAsString("?", "baz")).toBe("");
    expect(queryParamAsString("&", "baz")).toBe("");
    expect(queryParamAsString("", "baz")).toBe("");
  });

  it("queryParamAsString() handles numbers", () => {
    expect(queryParamAsString("?foo=0", "foo")).toBe("0"); // falsey values do not collapse to empty string
    expect(queryParamAsString("?foo=1", "foo")).toBe("1");
  });

  it("queryParamAsString() handles booleans", () => {
    expect(queryParamAsString("?foo=false", "foo")).toBe("false"); // falsey values do not collapse to empty string
    expect(queryParamAsString("?foo=true", "foo")).toBe("true");
  });

  it("queryParamAsString() handles enumerables", () => {
    expect(queryParamAsString("?foo[]", "foo")).toBe(`[""]`);
    expect(queryParamAsString("?foo[]=", "foo")).toBe(`[""]`);
    expect(queryParamAsString("?foo[0]=&foo[1]=", "foo")).toBe(`["",""]`);
    expect(queryParamAsString("?foo[0]=a&foo[1]=b", "foo")).toBe(`["a","b"]`);
    expect(queryParamAsString("?foo[bar]=baz", "foo")).toBe(`{"bar":"baz"}`);
  });
});
