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

import {AbstractObjCache} from "models/base/cache";

describe("AbstractObjectCache", () => {
  class SuccessCache extends AbstractObjCache<string[]> {
    doFetch(resolve: (data: any[]) => void, reject: (reason: string) => void) {
      resolve(["foo", "bar"]);
    }
  }

  class FailCache extends AbstractObjCache<string[]> {
    doFetch(resolve: (data: any[]) => void, reject: (reason: string) => void) {
      reject("boom!");
    }
  }

  it("prime() only fetches once", () => {
    const cache = new SuccessCache();
    const onsuccess = jasmine.createSpy("good");
    const onfail = jasmine.createSpy("bad");

    expect(cache.ready()).toBe(false);
    expect(cache.contents()).toBeUndefined();

    cache.prime(onsuccess, onfail);
    expect(cache.contents()).toEqual(["foo", "bar"]);

    expect(cache.ready()).toBe(true);
    expect(cache.failed()).toBe(false);
    expect(cache.failureReason()).toBeUndefined();

    expect(onsuccess).toHaveBeenCalledTimes(1);
    expect(onfail).not.toHaveBeenCalled();

    cache.prime(onsuccess, onfail);
    expect(onsuccess).toHaveBeenCalledTimes(1); // prime should not refetch
    expect(onfail).not.toHaveBeenCalled();
  });

  it("invalidate() clears cache and allows prime to fetch again", () => {
    const cache = new SuccessCache();
    const onsuccess = jasmine.createSpy("good");
    const onfail = jasmine.createSpy("bad");

    expect(cache.ready()).toBe(false);
    expect(cache.contents()).toBeUndefined();

    cache.prime(onsuccess, onfail);
    expect(cache.contents()).toEqual(["foo", "bar"]);

    expect(cache.ready()).toBe(true);
    expect(cache.failed()).toBe(false);
    expect(cache.failureReason()).toBeUndefined();

    expect(onsuccess).toHaveBeenCalledTimes(1);
    expect(onfail).not.toHaveBeenCalled();

    cache.invalidate();
    expect(cache.ready()).toBe(false);
    expect(cache.contents()).toBeUndefined();

    cache.prime(onsuccess, onfail);
    expect(cache.contents()).toEqual(["foo", "bar"]);
    expect(cache.ready()).toBe(true);
    expect(cache.failed()).toBe(false);
    expect(cache.failureReason()).toBeUndefined();

    expect(onsuccess).toHaveBeenCalledTimes(2);
    expect(onfail).not.toHaveBeenCalled();
  });

  it("Reports errors", () => {
    const cache = new FailCache();
    const onsuccess = jasmine.createSpy("good");
    const onfail = jasmine.createSpy("bad");

    expect(cache.ready()).toBe(false);

    cache.prime(onsuccess, onfail);
    expect(cache.failed()).toBe(true);
    expect(cache.failureReason()).toBe("boom!");

    expect(cache.ready()).toBe(false);
    expect(onsuccess).not.toHaveBeenCalled();
    expect(onfail).toHaveBeenCalled();
  });
});
