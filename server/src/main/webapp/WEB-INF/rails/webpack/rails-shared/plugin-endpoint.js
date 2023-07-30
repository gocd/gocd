/*
 * Copyright 2023 Thoughtworks, Inc.
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

  // This file is shared between new and old pages, so we can't use ES6 syntax as the file
  // isn't guaranteed to be compiled

  /* eslint-disable no-var,prefer-template,object-shorthand,prefer-arrow-callback */
  var HANDLERS = {};
  var attached = false;
  var uid;
  var pluginId;

  var REQUEST_ID_SEQ = 0;
  var PENDING_REQUESTS = new RequestTable();
  var KNOWN_VERSIONS = ["v1"];
  var VERSION = "v1";

  function RequestTable() {
    var requests = {};

    this.register = function register(id, request) {
      requests[id] = request;
    };

    this.pop = function getRequest(id) {
      var req = requests[id];

      if (!req) {
        throw new Error("No request with id: " + id);
      }

      delete requests[id];

      return req;
    };
  }

  function err() {
    if (window.console && "function" === typeof window.console.error) {
      window.console.error.apply(null, arguments);
    }
  }

  function init(win, data) {
    new Transport(win, null).request("init", data);
  }

  /** constructs a message from metadata and payload, and sends it to the target window */
  function send(win, data, metadata) {
    metadata = metadata || {};

    if (uid) { metadata.uid = uid; }
    if (pluginId) { metadata.pluginId = pluginId; }

    if (win && data) {
      // origin of target window is inaccessible when iframe sandbox is enforced, so
      // cannot check origin (no need to anyway), so just use "*"
      win.postMessage({ head: metadata, body: data }, "*");
    } else {
      err("Failed to send", data, "with metadata", metadata, "to window:", win);
    }
  }

  /** main function to delegate messages to appropriate handlers */
  function dispatch(ev) {
    var message = ev.data;
    if ("null" !== ev.origin && ev.origin !== window.location.origin) {
      err("Disregarding message", message, "because origin", ev.origin, "does not match", window.location.origin);
      return;
    }

    // We always expect data to be an object; this also has the nice
    // side effect of ignoring bootstrap's test for window.postMessage()
    if ("object" !== typeof message) {
      return;
    }

    var error = validateMessage(message);
    if (error) {
      err(error + " debug:", message);
      return;
    }

    handleRequestResponse(ev.source, message);
  }

  function validateMessage(message) {
    if ("object" !== typeof message.head) {
      return "Missing message metadata!";
    }

    if ("string" !== typeof message.head.type) {
      return "Missing transport type!";
    }

    if ("number" !== typeof message.head.reqId) {
      return "Missing request id!";
    }
  }

  function messageKey(message) {
    return message && message.head && ("string" === typeof message.head.key) ?
      message.head.key : null;
  }

  function handleRequestResponse(source, message) {
    var reqId = message.head.reqId,
      type = message.head.type;

    switch (type) {
    case "request":
      var key = messageKey(message);

      if (!key) {
        err("Request is missing key; debug:", JSON.stringify(message));
        return;
      }

      if ("function" !== typeof HANDLERS[key]) {
        err("Don't know how to handle request key:", key);
        return;
      }

      HANDLERS[key](message, new Transport(source, reqId));
      break;
    case "response":
      var req = PENDING_REQUESTS.pop(reqId);
      req.onComplete(message.body.data, message.body.errors);
      break;
    default:
      err("Don't know how to handle type", type, "; debug:", message);
      break;
    }
  }

  function nextReqId() { return REQUEST_ID_SEQ++; }

  function msgKey(key) {
    if ("string" !== typeof key || "" === key.replace(/^\s+|\s+$/g, "")) {
      throw new Error("key cannot be blank");
    }
    return ["go.cd.analytics", VERSION, key].join(".");
  }

  function Transport(win, responseId) {
    var alreadyResponded = false;

    this.request = function sendRequest(key, data) {
      var fullKey = msgKey(key);
      var reqId = nextReqId();
      var req = new PluginRequest(reqId);

      PENDING_REQUESTS.register(reqId, req);

      /* make the send() happen asynchronously so that plugin
       * authors can attach chaining-style callbacks. */
      setTimeout(function() {
        send(win, data, {reqId: reqId, type: "request", key: fullKey});
      }, 0);

      return req;
    };

    this.respond = function sendResponse(data) {
      if ("number" !== typeof responseId) {
        err("Missing responseId, cannot locate associated request!");
        return;
      }

      if (alreadyResponded) {
        err("Response already sent! Cannot send again for request", responseId);
        return;
      }

      alreadyResponded = true;
      send(win, data, {reqId: responseId, type: "response"});
    };
  }

  function PluginRequest() {
    function noop() {}

    var handlers = {
      done: noop,
      fail: noop,
      always: noop
    };

    function addCallbackSetter(obj, name) {
      obj[name] = function(fn) {
        if ("function" === typeof fn) {
          handlers[name] = fn;
        }

        return obj;
      };
    }

    addCallbackSetter(this, "done");
    addCallbackSetter(this, "fail");
    addCallbackSetter(this, "always");

    this.onComplete = function (data, errors) {
      if (!errors) {
        if ("function" === typeof handlers.done) {
          handlers.done(data);
        }
      } else {
        if ("function" === typeof handlers.fail) {
          handlers.fail(errors);
        }
      }

      if ("function" === typeof handlers.always) {
        handlers.always();
      }
    };
  }


  function validateVersion(version) {
    if ("undefined" === typeof version) {
      throw new Error("You must specify a version to `ensure()`");
    }

    if (KNOWN_VERSIONS.indexOf(version) === -1) {
      throw new Error("`version` must be one of: " + KNOWN_VERSIONS.join(", "));
    }

    return version;
  }

  var AnalyticsEndpoint = {
    reset: function reset() {
      HANDLERS = {},
      attached = false,
      uid = undefined,
      pluginId = undefined,

      REQUEST_ID_SEQ = 0,
      PENDING_REQUESTS = new RequestTable();

      window.removeEventListener("message", dispatch);
    },
    ensure: function ensure(version) {
      VERSION = validateVersion(version);

      if (!attached) {
        window.addEventListener("message", dispatch, false);
        attached = true;
      }
      return this;
    },
    /** define a handler for a single key */
    on: function addOne(key, handlerFn) {
      HANDLERS[key] = handlerFn;
    },
    /** define an api, i.e. a set of handlers for one or more keys */
    define: function addMany(api) {
      for (var i = 0, keys = Object.keys(api), len = keys.length; i < len; ++i) {
        if ("function" === typeof api[keys[i]]) {
          HANDLERS[keys[i]] = api[keys[i]];
        }
      }
    },
    init: init,
    onInit: function onInit(initializerFn) {
      this.on(msgKey("init"), function handleInit(message, transport) {
        var body = message.body;
        uid = body.uid;
        pluginId = body.pluginId;
        initializerFn(body.initialData, transport);
      });
    }
  };

  if ("undefined" !== typeof module) {
    module.exports = AnalyticsEndpoint;
  } else {
    window.PluginEndpoint = AnalyticsEndpoint;
    window.AnalyticsEndpoint = AnalyticsEndpoint;
  }
})();
