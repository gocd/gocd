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

import {CaseInsensitiveMap} from "../collections";

describe("CaseInsensitiveMap", () => {
  it("set() and get() handle keys case-insensitvely", () => {
    const m = new CaseInsensitiveMap<number>();

    m.set("Ab", 1);
    expect(m.get("ab")).toBe(1);
    expect(m.get("aB")).toBe(1);
    expect(m.get("Ab")).toBe(1);
    expect(m.get("AB")).toBe(1);

    m.set("ab", 2);
    expect(m.get("ab")).toBe(2);
    expect(m.get("aB")).toBe(2);
    expect(m.get("Ab")).toBe(2);
    expect(m.get("AB")).toBe(2);

    m.set("aB", 3);
    expect(m.get("ab")).toBe(3);
    expect(m.get("aB")).toBe(3);
    expect(m.get("Ab")).toBe(3);
    expect(m.get("AB")).toBe(3);

    m.set("AB", 4);
    expect(m.get("ab")).toBe(4);
    expect(m.get("aB")).toBe(4);
    expect(m.get("Ab")).toBe(4);
    expect(m.get("AB")).toBe(4);
  });

  it("has() and delete() handles keys case-insensitvely", () => {
    const m = new CaseInsensitiveMap<number>();

    m.set("Ab", 1);
    expect(m.has("ab")).toBe(true);
    expect(m.has("aB")).toBe(true);
    expect(m.has("Ab")).toBe(true);
    expect(m.has("AB")).toBe(true);

    expect(m.delete("ab")).toBe(true);

    expect(m.has("ab")).toBe(false);
    expect(m.has("aB")).toBe(false);
    expect(m.has("Ab")).toBe(false);
    expect(m.has("AB")).toBe(false);
  });
});
