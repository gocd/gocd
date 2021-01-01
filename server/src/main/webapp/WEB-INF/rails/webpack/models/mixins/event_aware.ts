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

// a generic function
type fn = (args?: any) => any;

export interface EventAware {
  on: (type: string, fn: fn) => void;
  off: (type: string, fn?: fn) => void;
  notify: (type: string, ...data: any[]) => void;
  reset: () => void;
}

/** Provides message-passing capabilities to instances. Use as a mixin or base class. */
export class EventAware {
  // Forming a closure here means there's never a need to `Function.bind()` for
  // `on()`, `notify()`, etc. It also means the internal event map will never get
  // serialized and is truly private.
  constructor(handlers: Map<string, fn[]> = new Map()) {
    this.on = (type: string, fn: fn) => {
      if ("function" !== typeof fn) { throw new Error("Not registering a non-function"); }

      const fns: fn[] = handlers.get(type) || [];
      fns.push(fn);
      handlers.set(type, fns);
    };

    this.notify = (type: string, ...data: any[]) => {
      for (const fn of (handlers.get(type) || [])) {
        fn(...data);
      }
    };

    this.off = (type: string, fn?: fn) => {
      if (!fn) {
        handlers.delete(type);
        return;
      }

      if (handlers.get(type)) {
        const fns: fn[] = handlers.get(type)!;
        const idx = fns.indexOf(fn);

        if (-1 !== idx) {
          fns.splice(idx, 1);
          handlers.set(type, fns);
        }
      }
    };

    this.reset = () => {
      handlers.clear();
    };
  }
}
