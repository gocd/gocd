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

const _      = require('lodash');
const m      = require('mithril');
const Stream = require('mithril/stream');
const Repeat = require('repeat');

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

const defaultOptions = {intervalSeconds: 10, inSeconds: 0, visibilityBackoffFactor: 4};

const AjaxPoller = function (args = {}) {
  let options;
  if (_.isFunction(args)) {
    options = _.assign({}, defaultOptions, {fn: args});
  } else {
    options = _.assign({}, defaultOptions, args);
  }


  const self       = this;
  const currentXHR = Stream();
  let repeater;

  function createRepeater() {
    return Repeat((repeatAgain) => {
      options.fn(currentXHR).always(() => {
        repeatAgain();
        currentXHR(null);
        m.redraw();
      });
    });
  }

  function currentPollInterval() {
    if (document[hidden]) {
      return options.intervalSeconds * options.visibilityBackoffFactor;
    } else {
      return options.intervalSeconds;
    }
  }

  const handleVisibilityChange = () => {
    self.stop();
    repeater = createRepeater();
    repeater.every(currentPollInterval(), 'sec').start.in(options.inSeconds, 'sec');
  };

  const doesBrowserSupportPageVisibilityAPI = () => {
    return (typeof document.hidden !== "undefined");
  };

  this.start = function () {
    repeater = createRepeater();

    repeater.every(currentPollInterval(), 'sec').start.in(options.inSeconds, 'sec');
    if (doesBrowserSupportPageVisibilityAPI()) {
      document.addEventListener(visibilityChange, handleVisibilityChange, false);
    } else {
      console.warn("Browser doesn't support the Page Visibility API!"); //eslint-disable-line
    }
    return this;
  };

  this.stop = () => {
    repeater.stop();
    if (currentXHR()) {
      currentXHR().abort();
    }
  };
};

module.exports = AjaxPoller;
