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

import Stream from "mithril/stream";

export function rejectAsString(reject: (r: string) => void) {
  return (reason: any) => {
    if ("string" === typeof reason) {
      return reject(reason);
    }

    if ("message" in reason) {
      return reject(reason.message);
    }

    reject(String(reason));
  };
}

export interface ObjectCache<T> {
  ready: () => boolean;
  prime: (onSuccess: () => void, onError?: () => void) => void;
  contents: () => T;
  failureReason: () => string | undefined;
  failed: () => boolean;
  invalidate: () => void;
}

export abstract class AbstractObjCache<T> implements ObjectCache<T> {
  private primed: boolean = false;
  private syncing: boolean = false;
  private data: Stream<T> = Stream();
  private error: Stream<string> = Stream();

  prime(onSuccess: () => void, onError?: () => void) {
    if (this.busy() || this.ready()) {
      return;
    }

    this.lock();

    this.error = Stream();

    this.doFetch((data: T) => {
      this.data(data);
      this.primed = true;
      onSuccess();
      this.release();
    }, (reason: string) => {
      this.invalidate();
      this.error(reason);
      if (onError) {
        onError();
      }
      this.release();
    });
  }

  abstract doFetch(resolve: (data: T) => void, reject: (reason: string) => void): void;

  contents(): T {
    return this.data();
  }

  failed(): boolean {
    return "string" === typeof this.error();
  }

  failureReason(): string | undefined {
    return this.error();
  }

  ready(): boolean {
    return this.primed;
  }

  invalidate() {
    this.markStale();
    this.primed = false;
  }

  protected markStale() {
    this.empty();
  }

  protected empty() {
    this.data = Stream();
  }

  private busy(): boolean {
    return this.syncing;
  }

  private lock(): void {
    this.syncing = true;
  }

  private release(): void {
    this.syncing = false;
  }
}
