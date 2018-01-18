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

(function() {
  "use strict";

  // these are intentionally private variables, hidden via closure
  /* eslint-disable no-var */
  // disabling no-var because this is also shared with rails
  var handlers = {};
  var attached = false;
  var uid;
  var pluginId;
  /* eslint-enable no-var */

  function err() {
    if (window.console && "function" === typeof window.console.error) {
      window.console.error.apply(null, arguments);
    }
  }

  function init(win, message) {
    send(win, "init", message);
  }

  /** constructs a message from key and body, and sends it to the target window */
  function send(win, key, message) {
    if (uid) { message.uid = uid; }
    if (pluginId) { message.pluginId = pluginId; }

    if (win && key && message) {
      message.key = key;

      // origin of target window is inaccessible when iframe sandbox is enforced, so
      // cannot check origin (no need to anyway), so just use "*"
      win.postMessage(message, "*");

    } else {
      err("Failed to send", key, "=>", message, "to window:", win);
    }
  }

  /** main function to delegate messages to appropriate handlers */
  function dispatch(ev) {
    if ("null" !== ev.origin && ev.origin !== window.location.origin) {
      err("Disregarding message", ev.data, "because origin", ev.origin, "does not match", window.location.origin);
      return;
    }

    // We always expect data to be an object; this also has the nice
    // side effect of ignoring bootstrap's test for window.postMessage()
    if ("object" !== typeof ev.data) {
      return;
    }

    if (!(ev.data && ev.data.key)) {
      err("Message is missing key; debug:", JSON.stringify(ev.data));
      return;
    }

    if ("function" !== typeof handlers[ev.data.key]) {
      err("Don't know how to handle message key:", ev.data.key);
      return;
    }

    function reply(key, data) {
      var message = { data: data }; // eslint-disable-line no-var, object-shorthand
      send(ev.source, key, message);
    }

    // a handler is a function that will receive the message payload and a reply()
    // function to *optionally* allow the handler to send a response back.
    handlers[ev.data.key](ev.data, reply);
  }

  var PluginEndpoint = { // eslint-disable-line no-var
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
      for (var i = 0, keys = Object.keys(api), len = keys.length; i < len; ++i) { // eslint-disable-line no-var
        if ("function" === typeof api[keys[i]]) {
          handlers[keys[i]] = api[keys[i]];
        }
      }
    },
    init: init, // eslint-disable-line object-shorthand
    onInit: function onInit(initializerFn) {
      PluginEndpoint.on("init", function handleInit(message, reply) { // eslint-disable-line prefer-arrow-callback
        uid = message.uid;
        pluginId = message.pluginId;
        initializerFn(message, reply);
      });
    }
  };

  if ("undefined" !== typeof module) {
    module.exports = PluginEndpoint;
  } else {
    window.PluginEndpoint = PluginEndpoint;
  }
})();
