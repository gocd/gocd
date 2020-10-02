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

import {NonThrashingScheduler, OnloadScheduler} from "../scheduler";

describe("Scheduler", () => {
  // this is really the only thing that can be tested without some awkward hack; testing
  // the exec delay until onload fires would be difficult
  it("OnloadScheduler immediately runs a task once the page has loaded", (done) => {
    const a = jasmine.createSpy("first()"),
          b = jasmine.createSpy("second()"),
          c = jasmine.createSpy("third()");

    const scheduler = new OnloadScheduler();

    scheduler.schedule(a);
    scheduler.schedule(b);
    scheduler.schedule(c);

    scheduler.schedule(() => {
      expect(a).toHaveBeenCalled();
      expect(b).toHaveBeenCalled();
      expect(c).toHaveBeenCalled();
      done();
    });
  });

  // please don't be flaky :)
  it("NonThrashingScheduler deduplicates schedules between RAFs", (done) => {
    const a = jasmine.createSpy("first()");

    const scheduler = new NonThrashingScheduler();

    scheduler.schedule(a), scheduler.schedule(a); // ought to be deduplicated when scheduling before the next animation frame

    scheduler.schedule(() => {
      // `a` should have been dequeued by now
      expect(a).toHaveBeenCalledTimes(1);

      scheduler.schedule(a); // can run once again after we've dequeued
      scheduler.schedule(() => {
        // after dequeuing, `a` should have been run exactly once more
        expect(a).toHaveBeenCalledTimes(2);
        done();
      });
    });
  });
});
