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

export function treeMap<A extends Traversable, B extends Traversable>(tree: A, fn: (tree: A) => B): B {
  return makeTree<B>(fn(tree), _.map(childrenOf<A>(tree), (child) => treeMap(child, fn)));
}

function childrenOf<T extends Traversable>(tree: T): T[] {
  return tree.children! as T[];
}

function makeTree<T extends Traversable>(datum: T, children: T[]): T {
  datum.children = children;
  return datum;
}

// NOTE: To those scatching their heads thinking this isn't simply:
//
// ```
// export interface Traversable {
//   children: Traversable[]
// }
// ```
//
// This weirdness is intentional so that `m.Vnode` can also satisfy this interface. It has
// no effect on the resultant/compiled JS. Implementations and extensions may (should?) be
// stricter (i.e., force the presence and shape of `children` to something more sensible,
// such as `children: Traversable[]`).
interface Data extends Array<Datum | Data> {}
type Datum = Traversable | string | number | boolean | null | undefined;

export interface Traversable {
  children?: Datum | Data;
}
