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

type maybeString = string | undefined;
type Partial<T> = { [P in keyof T]?: maybeString }; // represents a subset of attrs on a `styles` object

/**
 * This is the foundation of all subsequent proxies; takes a `styles` object
 * and applies an arbitrary lambda transformation to a given CSS className.
 */
export function remapCss<T>(obj: T, transform: (key: keyof T) => maybeString): T {
  if ("function" === typeof Proxy) {
    return new Proxy(obj, {
      get: (target: any, key: (keyof T)) => {
        if (key in target) {
          return transform(key);
        }
        throw new ReferenceError(`Cannot find classname ${key}`);
      }
    });
  }

  return (Object.keys(obj) as Array<keyof T>).reduce((result, key) => {
    Object.defineProperty(result, key, {
      get() { return transform(key); }
    });
    return result;
  }, {} as T);
}

/**
 * Takes a `styles` object and returns a proxy to prefix all
 * classnames with a `.` so it can be used directly in Element.querySelector().
 * Perfect for the lazy. Like me.
 *
 * Example:
 *
 * import styles from "./index.scss";
 * import {asSelector} from "helpers/css_proxies";
 *
 * const sel = asSelector<typeof styles>(styles);
 *
 * console.log(styles.myClass); // "my-class"
 * console.log(sel.myClass);    // ".my-class"
 */
export function asSelector<T>(obj: T): T {
  return remapCss<T>(obj, (key: keyof T) => `.${obj[key]}`);
}

/**
 * Allows simple (partial or full) overrides of any css classname. Essentially
 * just syntactic sugar over a simple case for remapCss().
 */
export function override<T>(obj: T, clobbers: Partial<T>): T {
  const overrides = asMap<T>(clobbers);
  return remapCss<T>(obj, (key: keyof T) => ((overrides.has(key) ? overrides.get(key) : obj[key]) as maybeString));
}

function asMap<T>(obj: Partial<T>): Map<keyof T, maybeString> {
  const result = new Map<keyof T, maybeString>();
  for (const k of Object.keys(obj) as Array<keyof T>) {
    result.set(k, obj[k] as maybeString);
  }
  return result;
}
