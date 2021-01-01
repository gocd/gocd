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

type fn = () => void;

export interface Scheduler {
  schedule(task: fn): void;
}

/** A simple task scheduler that ensures each task runs after the page `load` event has fired. */
export class OnloadScheduler implements Scheduler {
  private queue: fn[] = [];
  private hasLoaded: boolean = false;

  constructor() {
    if ("complete" === document.readyState) {
      this.hasLoaded = true;
      return;
    }

    window.addEventListener("load", () => {
      this.hasLoaded = true;

      for (const fn of this.queue) {
        fn();
      }
    });
  }

  schedule(task: fn) {
    if (this.hasLoaded) {
      task();
    } else {
      this.queue.push(task);
    }
  }
}

/** A deduplicating, thrash-preventing (i.e., window.requestAnimationFrame) scheduler */
export class NonThrashingScheduler implements Scheduler {
  private queue = new Set<fn>();

  schedule(task: fn) {
    this.queue.add(task);
    this.dequeue();
  }

  private dequeue() {
    window.requestAnimationFrame(() => {
      this.queue.forEach((fn) => {
        fn();
        this.queue.delete(fn);
      });
    });
  }
}
