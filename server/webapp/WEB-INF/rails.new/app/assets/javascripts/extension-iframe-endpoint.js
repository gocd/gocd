(function($, _) {
  "use strict";

  // these are intentionally private variables, hidden via closure
  var handlers = {};
  var attached = false;

  function err() {
    if (console && "function" === typeof console.error) {
      console.error.apply(null, arguments);
    }
  }

  /** constructs a message from key and body, and sends it to the target window */
  function send(win, key, body) {
    if (win && key && body) {
      // origin of target window is inaccessible when iframe sandbox is enforced, so
      // cannot check origin (no need to anyway), so just use "*"
      win.postMessage({key: key, body: body}, "*");
    } else {
      err("Failed to send ", key, "=>", body, "to window:", win);
    }
  }

  /** main function to delegate messages to appropriate handlers */
  function dispatch(ev) {
    // also does not need origin check because iframe sandbox security forbids access
    // to other window origin
    if (!(ev.data && ev.data.key)) {
      err("Message is missing key; debug:", JSON.stringify(ev.data));
      return;
    }

    if ("function" !== typeof handlers[ev.data.key]) {
      err("Don't know how to handle message key:", ev.data.key);
      return;
    }

    // a handler is a function that will receive the message payload and a reply()
    // function to *optionally* allow the handler to send a response back.
    handlers[ev.data.key](ev.data.body, function reply(key, message) { send(ev.source, key, message); });
  }

  window.ExtensionIFrameEndpoint = {
    ensure: function ensure() {
      if (!attached) {
        window.addEventListener("message", dispatch, false);
        attached = true;
      }
      return this;
    },
    /** define a handler for a single key */
    on: function addOne(key, handlerFn) {
      handlers[key] = handlerFn;
    },
    /** define an api, i.e. a set of handlers for one or more keys */
    define: function addMany(api) {
      _.assign(handlers, api); // or use _.extend() ?
    },
    send: send
  };

})(jQuery, _);
