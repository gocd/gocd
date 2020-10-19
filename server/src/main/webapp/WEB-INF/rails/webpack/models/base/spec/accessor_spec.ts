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

import {basicAccessor, bidirectionalTransform, serializing} from "models/base/accessor";

describe("Accessor", () => {
  it("basicAccessor() stores and fetches a value", () => {
    const a = basicAccessor<string>("hi");
    expect(a()).toBe("hi");

    a("bye");
    expect(a()).toBe("bye");

    a("another day, another Doug");
    expect(a()).toBe("another day, another Doug");
  });

  it("serializing() makes an accessor serializable", () => {
    const orig = basicAccessor<number>(1);
    expect(JSON.stringify(orig)).toBeUndefined();

    const jsonable = serializing(orig);
    expect(JSON.stringify(jsonable)).toBe("1");

    // respects custom toJSON()
    (orig as any).toJSON = () => ({val: orig()});
    expect(JSON.stringify(jsonable)).toBe(`{"val":1}`);
  });

  it("bindingTransform() transforms data on store and fetch", () => {
    type Input = "on" | "off";
    const backing = basicAccessor<boolean>(true);
    const facing = bidirectionalTransform<Input, boolean>(backing, (v) => v === "on", (v) => v ? "on" : "off");

    // initial
    expect(facing()).toBe("on");
    expect(backing()).toBe(true);

    // setting value on facing will alter the value in backing
    facing("off");
    expect(backing()).toBe(false);
    expect(facing()).toBe("off"); // of course facing should retain its value

    facing("on");
    expect(backing()).toBe(true);
    expect(facing()).toBe("on"); // of course facing should retain its value

    // setting value on backing should alter what facing returns as well
    backing(false);
    expect(facing()).toBe("off");

    backing(true);
    expect(facing()).toBe("on");
  });
});
