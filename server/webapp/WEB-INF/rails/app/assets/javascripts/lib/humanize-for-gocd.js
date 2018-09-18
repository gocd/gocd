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

// requires momentjs and moment-duration-format

(function (root, undefined) {

  // define internal moment reference
  var moment;

  if (typeof require === "function") {
    try {
      moment = require('moment');
    }
    catch (e) {
    }
  }

  if (!moment && root.moment) {
    moment = root.moment;
  }

  if (!moment) {
    throw "Cannot find Moment.js";
  }

  var HOURS_24 = moment.duration(24, 'hour').asMilliseconds();
  var HOUR_1   = moment.duration(1, 'hour').asMilliseconds();
  var MINUTE_1 = moment.duration(1, 'minute').asMilliseconds();

  moment.duration.fn.humanizeForGoCD = function () {
    var d = moment.duration(this, "ms");

    if (d >= HOURS_24) {
      return d.format("d[d] h:m:s.S");
    }

    if (d >= HOUR_1) {
      return d.format("h[h] m[m] s.S[s]");
    }

    if (d >= MINUTE_1) {
      return d.format("m[m] s.S[s]");
    }

    return d.format("s.S[s]", {trim: false});
  };
})(this);
