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
import {Nameable, NameableSet} from "models/pipeline_configs/nameable_set";

describe("NameableSet", () => {
  it("determines equality based on `name()`", () => {
    const a = new MockNameable("one", true),
          b = new MockNameable("two", true),
          c = new MockNameable("three", true);

    const d = new MockNameable("one", false);

    const s = new NameableSet<MockNameable>([a, b, c]);
    expect(s.size).toBe(3);
    expect(s.has(a)).toBe(true);
    expect(s.has(b)).toBe(true);
    expect(s.has(c)).toBe(true);

    expect(a).not.toBe(d); // assert a and d are indeed referentially diff
    expect(a.toApiPayload()).not.toEqual(d.toApiPayload()); // assert a and d are indeed structurally diff
    expect(a.name()).toBe(d.name());
    expect(s.has(d)).toBe(true); // Set matches on has() because the names of a and d are the same
  });

  it("retains only values with unique names and evicts duplicates", () => {
    const a = new MockNameable("one", true),
          b = new MockNameable("two", true),
          c = new MockNameable("three", true),
          d = new MockNameable("one", true);

    const s = new NameableSet<MockNameable>([a, b, c, d]);

    expect(s.size).toBe(3);
    expect(Array.from(s.values())).toEqual([d, b, c]); // `a` was evicted

    s.add(a);
    expect(Array.from(s.values())).toEqual([a, b, c]); // `d` was evicted
  });

  it("maintains original insertion order based on `name()`", () => {
    const a = new MockNameable("one", true),
          b = new MockNameable("two", true),
          c = new MockNameable("three", true),
          d = new MockNameable("one", true),
          e = new MockNameable("three", true);

    const s = new NameableSet<MockNameable>([]);

    expect(s.size).toBe(0);

    s.add(a);
    expect(Array.from(s.values())).toEqual([a]);

    s.add(b);
    expect(Array.from(s.values())).toEqual([a, b]);

    s.add(c);
    expect(Array.from(s.values())).toEqual([a, b, c]);

    s.add(d);
    expect(Array.from(s.values())).toEqual([d, b, c]);

    s.add(e);
    expect(Array.from(s.values())).toEqual([d, b, e]);
  });

  it("validates all members", () => {
    const a = new MockNameable("one", true),
          b = new MockNameable("two", true),
          c = new MockNameable("three", true),
          d = new MockNameable("one", false);

    const s = new NameableSet<MockNameable>([a, b, c]);

    expect(s.isValid()).toBe(true);

    s.add(d);
    expect(s.isValid()).toBe(false);
  });

  it("clear() deletes all members", () => {
    const a = new MockNameable("one", true),
          b = new MockNameable("two", true),
          c = new MockNameable("three", true);

    const s = new NameableSet<MockNameable>([a, b, c]);

    expect(s.size).toBe(3);
    expect(Array.from(s.values())).toEqual([a, b, c]);

    s.clear();
    expect(s.size).toBe(0);
    expect(Array.from(s.values())).toEqual([]);
  });

  it("serializes to an array", () => {
    const a = new MockNameable("one", true),
          b = new MockNameable("two", true),
          c = new MockNameable("three", true);

    const s = new NameableSet<MockNameable>([a, b, c]);

    expect(s.toJSON()).toEqual([a.toApiPayload(), b.toApiPayload(), c.toApiPayload()]);
  });
});

class MockNameable implements Nameable {
  private static nextId: number = 0;

  private _id: number;
  private _name: string;
  private _result: boolean;
  private _e: Errors = new Errors();

  constructor(name: string, mockValid: boolean) {
    this._name = name;
    this._id = MockNameable.nextId++;
    this._result = mockValid;
  }

  name(): string { return this._name; }
  toApiPayload(): any { return { id: this._id, name: this._name }; }

  isValid(): boolean { return this._result; }
  validate(_?: string): Errors { return this._e; }
  errors(_?: Errors): Errors { return this._e; }
}
