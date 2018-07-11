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
