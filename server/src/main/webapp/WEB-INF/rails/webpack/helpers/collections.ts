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

// not able to extend Map<K, V> without problems, so we implement by wrapping
// Map instead https://github.com/Microsoft/TypeScript/issues/10853
export class CaseInsensitiveMap<V> implements Map<string, V> {
  get size(): number {
    return this.delegate.size;
  }

  [Symbol.toStringTag] = "CaseInsensitiveMap";
  private readonly delegate: Map<string, V>;

  constructor(entries?: ReadonlyArray<readonly [string, V]> | null) {
    const m = this.delegate = new Map<string, V>();

    if (entries && entries.length) {
      entries.forEach((entry) => m.set(entry[0].toLowerCase(), entry[1]));
    }
  }

  set(key: string, val: V) {
    this.delegate.set(key.toLowerCase(), val);
    return this;
  }

  delete(key: string) {
    return this.delegate.delete(key.toLowerCase());
  }

  get(key: string) {
    return this.delegate.get(key.toLowerCase());
  }

  has(key: string) {
    return this.delegate.has(key.toLowerCase());
  }

  // boilerplate

  [Symbol.iterator]() {
    return this.delegate[Symbol.iterator]();
  }

  forEach(callbackfn: (value: V, key: string, map: Map<string, V>) => void, thisArg?: any) {
    const thisProvided = arguments.length > 1;

    this.delegate.forEach((val: V, key: string, _: Map<string, V>) => {
      if (thisProvided) {
        callbackfn.apply(thisArg, [val, key, this]);
      } else {
        callbackfn(val, key, this);
      }
    });
  }

  keys() {
    return this.delegate.keys();
  }

  values() {
    return this.delegate.values();
  }

  entries() {
    return this.delegate.entries();
  }

  clear() {
    this.delegate.clear();
  }
}
