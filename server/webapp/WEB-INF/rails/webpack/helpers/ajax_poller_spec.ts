/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {AjaxPoller} from "helpers/ajax_poller";
import * as _ from "lodash";

class AjaxPollerSubclass<T> extends AjaxPoller<T> {
  isHidden: boolean = false;

  protected isPageHidden() {
    return this.isHidden;
  }
}

describe("AjaxPoller", () => {
  let apiPoller: AjaxPollerSubclass<string>;
  let initialTimeout: number;
  beforeEach(() => {
    initialTimeout                   = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 3000;
  });

  afterEach(() => {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = initialTimeout;
  });

  afterEach(() => {
    if (apiPoller) {
      apiPoller.stop();
    }
  });

  it("should poll every `intervalSeconds` when promise resolves", (done) => {
    let time  = new Date().getTime();
    let count = 0;

    apiPoller = new AjaxPollerSubclass({
      intervalSeconds: 0.1,
      initialIntervalSeconds: 0.1,
      visibilityBackoffFactor: 4,
      repeaterFn: () => {
        count++;
        // because timer is set to run every 100ms, that should be the minimum interval between calls
        expect(new Date().getTime() - time).toBeGreaterThanOrEqual(100);
        expect(new Date().getTime() - time).toBeLessThanOrEqual(100 + 100);
        time = new Date().getTime();
        if (count === 3) {
          done();
        }
        return new Promise((resolve, reject) => {
          window.setTimeout(() => {
            resolve("resolved!");
          }, 10);
        });
      }
    });

    apiPoller.isHidden = false;

    apiPoller.start();
  });

  it("should poll every `intervalSeconds` when promise rejects", (done) => {
    let time  = new Date().getTime();
    let count = 0;

    apiPoller = new AjaxPollerSubclass({
      intervalSeconds: 0.1,
      initialIntervalSeconds: 0.1,
      visibilityBackoffFactor: 4,
      repeaterFn: () => {
        count++;
        // because timer is set to run every 100ms, that should be the minimum interval between calls
        expect(new Date().getTime() - time).toBeGreaterThanOrEqual(100);
        expect(new Date().getTime() - time).toBeLessThanOrEqual(100 + 100);
        time = new Date().getTime();
        if (count === 3) {
          done();
        }
        return new Promise<string>((resolve, reject) => {
          window.setTimeout(() => {
            reject("failed!");
          }, 10);
        })
          .then<string>(_.identity, _.identity); // because chrome will log this as an "uncaught error"
      }
    });

    apiPoller.isHidden = false;

    apiPoller.start();
  });

  it("should reduce polling interval when window is hidden", (done) => {
    let time  = new Date().getTime();
    let count = 0;

    apiPoller = new AjaxPollerSubclass({
      intervalSeconds: 0.1,
      initialIntervalSeconds: 0.4,
      visibilityBackoffFactor: 4,
      repeaterFn: () => {
        count++;
        // because timer is set to run every 100ms (with a 4x backoff because window is invisible)
        // 400ms should be the minimum interval between calls
        expect(new Date().getTime() - time).toBeGreaterThanOrEqual(100 * 4);
        expect(new Date().getTime() - time).toBeLessThanOrEqual(100 * 4 + 100);
        time = new Date().getTime();
        if (count === 3) {
          done();
        }
        return Promise.resolve("resolved!");
      }
    });

    apiPoller.isHidden = true;

    apiPoller.start();
  });

  it("should tweak call interval when window goes hidden in between calls", (done) => {
    let time  = new Date().getTime();
    let count = 0;

    apiPoller = new AjaxPollerSubclass({
      intervalSeconds: 0.1,
      initialIntervalSeconds: 0.1,
      visibilityBackoffFactor: 4,
      repeaterFn: () => {
        count++;
        if (apiPoller.isHidden) {
          expect(new Date().getTime() - time).toBeGreaterThanOrEqual(100 * 4);
          expect(new Date().getTime() - time).toBeLessThanOrEqual(100 * 4 + 100);
        } else {
          expect(new Date().getTime() - time).toBeGreaterThanOrEqual(100);
          expect(new Date().getTime() - time).toBeLessThanOrEqual(100 + 100);
        }
        time = new Date().getTime();

        // hide after 2 counts
        if (count === 2) {
          apiPoller.isHidden = true;
        }
        // show after 5 counts
        if (count === 5) {
          apiPoller.isHidden = false;
        }
        if (count === 7) {
          done();
        }
        return Promise.resolve("resolved!");
      }
    });

    apiPoller.start();

  });
});
