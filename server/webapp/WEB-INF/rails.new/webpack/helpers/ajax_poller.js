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

var m = require('mithril');
var Stream = require('mithril/stream');
var Repeat = require('repeat');

var AjaxPoller = function (fn) {
  var currentXHR = Stream();
  var repeater   = Repeat(function (repeatAgain) {
    fn(currentXHR).always(function () {
      repeatAgain();
      currentXHR(null);
      m.redraw();
    });
  });

  this.start = function ({intervalSeconds = 10, inSeconds = 0} = {}) {
    repeater.every(intervalSeconds, 'sec').start.in(inSeconds, 'sec');
    return this;
  };

  this.stop = function () {
    repeater.stop();
    if (currentXHR()) {
      currentXHR().abort();
    }
  };
};

module.exports = AjaxPoller;