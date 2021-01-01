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
(function (j) {
  "use strict";

  function mockMessageEvent(message) {
    const event = {};
    event.source = window;
    event.origin = "null";
    event.data = message;
    return event;
  }

  function fakeMessagePosting(fn) {
    const origPostMessage      = window.postMessage;
    const origAddEventListener = window.addEventListener;

    let dispatch;

    const addEventListenerSpy = j.createSpy("addEventListener").and.callFake((name, fn, bool) => {
      if ("message" === name) {
        dispatch = fn;
      } else {
        origAddEventListener(name, fn, bool);
      }
    });

    const postMessageSpy = j.createSpy("postMessage").and.callFake((message, _origin) => {
      dispatch(mockMessageEvent(message));
    });

    function setup() {
      window.addEventListener = addEventListenerSpy;
      Object.defineProperty(window, "postMessage", { value: postMessageSpy });
    }

    function teardown() {
      Object.defineProperty(window, "postMessage", { value: origPostMessage });
      window.addEventListener = origAddEventListener;
    }

    if (fn.length) { // async test, allow test author to control when to teardown
      setup();
      fn(teardown);
    } else {
      try {
        setup();
        fn();
      } finally {
        teardown();
      }
    }
  }

  j.fakeMessagePosting = fakeMessagePosting;
})(window.jasmine);
