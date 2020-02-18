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

import {EventAware} from "../event_aware";

describe("EventAware Mixin", () => {
  let events: EventAware;

  beforeEach(() => {
    events = new EventAware();
  });

  it("notify() only fires handlers when event type matches", () => {
    const listener = jasmine.createSpy("listener");
    events.on("foo", listener);

    events.notify("bar");
    expect(listener).not.toHaveBeenCalled();

    events.notify("foo");
    expect(listener).toHaveBeenCalled();
  });

  it("on() binds listeners to an event and notify() triggers", () => {
    const listener = jasmine.createSpy("listener");
    events.on("foo", listener);

    events.notify("foo");
    expect(listener).toHaveBeenCalled();

    events.notify("foo", 1, 2, 3, 4);
    expect(listener).toHaveBeenCalledWith(1, 2, 3, 4);
    expect(listener).toHaveBeenCalledTimes(2);
  });

  it("on() calls all (i.e., multiple) listeners for a given event", () => {
    const listener1 = jasmine.createSpy("listener");
    const listener2 = jasmine.createSpy("listener");

    events.on("foo", listener1);
    events.notify("foo");
    expect(listener1).toHaveBeenCalled();
    expect(listener2).not.toHaveBeenCalled();

    events.on("foo", listener2);
    events.notify("foo", "a", "b", "c");
    expect(listener1).toHaveBeenCalledWith("a", "b", "c");
    expect(listener1).toHaveBeenCalledTimes(2);
    expect(listener2).toHaveBeenCalledWith("a", "b", "c");
    expect(listener2).toHaveBeenCalledTimes(1);
  });

  it("off(type, fn) removes exactly 1 listener when specified", () => {
    const listener1 = jasmine.createSpy("listener");
    const listener2 = jasmine.createSpy("listener");
    const unbound = jasmine.createSpy("listener");

    events.on("foo", listener1);
    events.on("foo", listener2);

    events.notify("foo");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(1);
    expect(unbound).toHaveBeenCalledTimes(0);

    events.off("foo", listener1);
    events.notify("foo");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(2);
    expect(unbound).toHaveBeenCalledTimes(0);

    events.off("foo", unbound); // should not affect anything as it was never bound
    events.notify("foo");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(3);
    expect(unbound).toHaveBeenCalledTimes(0);
  });

  it("off(type) removes all listeners for specified event", () => {
    const listener1 = jasmine.createSpy("listener");
    const listener2 = jasmine.createSpy("listener");

    events.on("foo", listener1);
    events.on("foo", listener2);
    events.on("bar", listener2);

    events.notify("foo");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(1);

    events.notify("bar");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(2);

    events.off("foo");
    events.notify("foo");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(2);

    events.notify("bar");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(3);
  });

  it("reset() removes all listeners for all events", () => {
    const listener1 = jasmine.createSpy("listener");
    const listener2 = jasmine.createSpy("listener");

    events.on("foo", listener1);
    events.on("bar", listener2);

    events.notify("foo");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(0);

    events.notify("bar");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(1);

    events.reset();

    events.notify("foo");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(1);

    events.notify("bar");
    expect(listener1).toHaveBeenCalledTimes(1);
    expect(listener2).toHaveBeenCalledTimes(1);
  });
});
