/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const _         = require('lodash');
const m         = require('mithril');
const Stream    = require('mithril/stream');
const CONSTANTS = require('helpers/constants');

// Set the name of the hidden property and the change event for visibility
let hidden, visibilityChange;
if (typeof document.hidden !== "undefined") { // Opera 12.10 and Firefox 18 and later support
  hidden           = "hidden";
  visibilityChange = "visibilitychange";
} else if (typeof document.msHidden !== "undefined") {
  hidden           = "msHidden";
  visibilityChange = "msvisibilitychange";
} else if (typeof document.webkitHidden !== "undefined") {
  hidden           = "webkitHidden";
  visibilityChange = "webkitvisibilitychange";
}

const defaultOptions = {
  intervalSeconds:         CONSTANTS.SPA_REFRESH_INTERVAL / 1000,
  inSeconds:               0,
  visibilityBackoffFactor: 4
};

function AjaxPoller (args={}) {
  const options = _.assign({}, defaultOptions, "function" === typeof args ? {fn: args} : args);
  const currentXHR = Stream();
  const self = this;

  let timeout = null;
  let abort = false;

  function fire() {
    options.fn(currentXHR).always(() => {
      currentXHR(null);
      m.redraw();

      if (!abort && !autoRefreshDisabled()) {
        timeout = setTimeout(fire, currentPollInterval() * 1000);
      }
    });
  }

  function start() {
    abort = false;

    if (doesBrowserSupportPageVisibilityAPI()) {
      document.addEventListener(visibilityChange, handleVisibilityChange, false);
    } else {
      console.warn("Browser doesn't support the Page Visibility API!"); // eslint-disable-line no-console
    }

    const period = Math.max("number" === typeof options.inSeconds ? options.inSeconds : 0, 0);
    timeout      = setTimeout(fire, period * 1000);
  }

  function stop() {
    if ("number" === typeof timeout) {
      clearTimeout(timeout);
      abort   = true;
      timeout = null;
    }

    document.removeEventListener(visibilityChange, handleVisibilityChange);

    if (currentXHR()) {
      currentXHR().abort();
    }
  }

  function currentPollInterval() {
    return document[hidden] ?
      options.intervalSeconds * options.visibilityBackoffFactor :
      options.intervalSeconds;
  }

  function handleVisibilityChange() {
    self.restart();
  }

  function doesBrowserSupportPageVisibilityAPI() {
    return "undefined" !== typeof document[hidden];
  }

  this.stop  = stop;
  this.start = start;

  this.restart = () => {
    this.stop();
    this.start();
  };
}

function autoRefreshDisabled() {
  return !!~window.location.search.indexOf("auto_refresh=false");
}

module.exports = AjaxPoller;
