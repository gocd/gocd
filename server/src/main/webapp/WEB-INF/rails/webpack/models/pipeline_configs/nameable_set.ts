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

import {Validatable, ValidatableMixin} from "models/mixins/new_validatable_mixin";

export interface Nameable extends Validatable {
  name: () => string;
  toApiPayload: () => any;
}

// Specialized Set<T> implementation where member equality is based on only the `name`
// of the material, and NOT the identity or the structure.
export class NameableSet<T extends Nameable> extends ValidatableMixin implements Set<T> {
  [Symbol.toStringTag]: string = `NameableSet`;
  private readonly _members    = new Map<string, T>(); // preserves insertion order

  constructor(readonly items?: T[] | null) {
    super();
    Object.setPrototypeOf(this, Object.create(NameableSet.prototype));

    if (items) {
      items.forEach(this.add.bind(this));
    }
  }

  get size(): number {
    return this._members.size;
  }

  get length(): number {
    return this._members.size;
  }

  toJSON(): any {
    const r: any[] = [];
    this.forEach((m: T) => {
      r.push(m.toApiPayload());
    });
    return r;
  }

  validate(key?: string) {
    this.clearErrors(key);
    this.forEach((item: T) => {
      if (!item.isValid()) {
        this.errors().add(item.name(), `${item.constructor.name} named \`${item.name()}\` is invalid`);
      }
    }, this);
    return this.errors();
  }

  add(value: T): this {
    this._members.set(value.name(), value);
    return this;
  }

  clear() {
    this._members.clear();
  }

  delete(value: T): boolean {
    return this._members.delete(value.name());
  }

  forEach(callbackfn: (value: T, value2: T, set: Set<T>) => void, thisArg?: any) {
    const thisProvided = arguments.length > 1;

    this._members.forEach((val: T, name: string, _: Map<string, T>) => {
      if (thisProvided) {
        callbackfn.apply(thisArg, [val, val, this]);
      } else {
        callbackfn(val, val, this);
      }
    });
  }

  has(value: T): boolean {
    return this._members.has(value.name());
  }

  [Symbol.iterator](): IterableIterator<T> {
    return this._members.values();
  }

  entries(): IterableIterator<[T, T]> {
    return new Set<T>(this._members.values()).entries();
  }

  keys(): IterableIterator<T> {
    return this[Symbol.iterator]();
  }

  values(): IterableIterator<T> {
    return this[Symbol.iterator]();
  }

  findByName(name: string): T | undefined {
    return Array.from(this._members.values()).find((value: T) => value.name().toLowerCase() === name.toLowerCase());
  }
}
