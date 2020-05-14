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

/** An interface similar to, but more generic than, {@link Stream} that is both getter and setter */
export type Accessor<T> = (val?: T) => T;

/** An {@link Accessor} that also serializes like {@link Stream} does */
export interface SerializableAccessor<T> extends Accessor<T> {
  toJSON(): any;
}

/** Higher order function to convert an {@link Accessor} into a {@link SerializableAccessor} by ensuring a `toJSON()` */
export function serializing<T>(fn: Accessor<T>, thisArg?: any): SerializableAccessor<T> {
  const f = ((arguments.length > 1) ? fn.bind(thisArg) : fn) as any;
  if ("function" !== typeof f.toJSON) {
    f.toJSON = () => f();
  }
  return f;
}

/**
 * Returns a very simple {@link Accessor}; sometimes you want something even simpler than
 * a {@link Stream}. This will not serialize by default. This is sort of a "Stream-lite".
 */
export function basicAccessor<T>(initial?: T): Accessor<T> {
  let value: T;

  if (arguments.length) {
    value = initial!;
  }

  return function accessor(v?: T): T {
    if (arguments.length) {
      value = v!;
    }
    return value;
  };
}

/**
 * Calls `toJSON()` on a given object if defined and returns its value.
 * Returns the original object when `toJSON()` is absent. Convenient when
 * we want to call an object's custom serializer
 */
export function serialize<T>(v: any): T {
  return "function" === typeof v.toJSON ? v.toJSON() : v;
}