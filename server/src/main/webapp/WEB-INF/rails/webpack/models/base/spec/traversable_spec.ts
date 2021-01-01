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

import _ from "lodash";
import {Traversable, treeMap} from "models/base/traversable";

describe("Traversable", () => {
  it("treeMap() returns a transformed tree with the same hierarchy", () => {
    const tree = new NumNode(1,
      new NumNode(2, 5, 6), new NumNode(3, 7, 8), new NumNode(4, 9, 10)
    );

    function square(n: NumNode): NumNode {
      return new NumNode(n.datum * n.datum);
    }

    expect(tree.toJSON()).toEqual({
      datum: 1,
      children: [
        {datum: 2, children: [
          {datum: 5, children: []}, {datum: 6, children: []}
        ]}, {datum: 3, children: [
          {datum: 7, children: []}, {datum: 8, children: []}
        ]}, {datum: 4, children: [
          {datum: 9, children: []}, {datum: 10, children: []}
        ]}
      ]
    });

    expect((treeMap<NumNode, NumNode>(tree, square) as NumNode).toJSON()).toEqual({
      datum: 1,
      children: [
        {datum: 4, children: [
          {datum: 25, children: []}, {datum: 36, children: []}
        ]}, {datum: 9, children: [
          {datum: 49, children: []}, {datum: 64, children: []}
        ]}, {datum: 16, children: [
          {datum: 81, children: []}, {datum: 100, children: []}
        ]}
      ]
    });
  });
});

class NumNode implements Traversable {
  datum: number;
  children: NumNode[];

  constructor(datum: number, ...children: Array<number | NumNode>) {
    this.datum = datum;
    this.children = children.map((c) => "number" === typeof c ? new NumNode(c) : c);
  }

  toJSON(): any {
    return { datum: this.datum, children: this.children.map((c) => c.toJSON()) };
  }
}
