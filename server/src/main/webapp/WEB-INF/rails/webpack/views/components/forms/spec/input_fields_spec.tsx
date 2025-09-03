/*
 * Copyright Thoughtworks, Inc.
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
import m from "mithril";
import {labelToId} from "../input_fields";

describe("Input Fields:", () => {
  describe("labelToId converts labels into identifiers for fields", () => {
    const divVnode = m("div", "hello world");

    it("treats simple label types as identifiers directly", () => {
      expect(labelToId(null)).toBe("");
      expect(labelToId(undefined)).toBe("");
      expect(labelToId("hello")).toBe("hello");
      expect(labelToId("hello world")).toBe("hello-world");
      expect(labelToId(543)).toBe("543");
      expect(labelToId(true)).toBe("true");
      expect(labelToId(false)).toBe("false");
    });

    it("extracts text from rendered vnode", () => {
      expect(labelToId(divVnode)).toBe("hello-world");
      expect(labelToId(m("span", "hello world"))).toBe("hello-world");
    });

    it("ignores unexpected types", () => {
      // @ts-ignore
      const randomObject = Object.create({hello: "world"});
      expect(labelToId(randomObject)).toBe("");
    });

    describe("arrays", () => {
      it("concatenates labels from string arrays", () => {
        expect(labelToId(["hello", "world"])).toBe("helloworld");
      });

      it("ignores nil elements", () => {
        expect(labelToId([null, "world", undefined])).toBe("world");
      });

      it("concatenates labels from vnode elements of array", () => {
        expect(labelToId([divVnode, divVnode])).toBe("hello-worldhello-world");
      });

      it("takes label from text vnode", () => {
        const oneChild = m("span", "first element");
        expect(labelToId([oneChild])).toBe("first-element");
      });

      it("doesn't recurse into multi child vnode", () => {
        const twoChildren = m("div", [
          m("span", "first element"),
          m("span", "second element")
        ]);
        expect(labelToId([twoChildren])).toBe("");
      });
    });
  });
});