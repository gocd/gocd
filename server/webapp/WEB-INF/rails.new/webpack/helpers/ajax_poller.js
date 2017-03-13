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

const m = require('mithril');
const Stream = require('mithril/stream');
const Repeat = require('repeat');

const AjaxPoller = function (fn) {
  const currentXHR = Stream();
  const repeater   = Repeat((repeatAgain) => {
    fn(currentXHR).always(() => {
      repeatAgain();
      currentXHR(null);
      m.redraw();
    });
  });

  this.start = function ({intervalSeconds = 10, inSeconds = 0} = {}) {
    repeater.every(intervalSeconds, 'sec').start.in(inSeconds, 'sec');
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
