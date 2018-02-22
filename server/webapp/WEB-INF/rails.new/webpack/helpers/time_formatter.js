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


const $      = require('jquery');
const moment = require("moment");
require("moment-duration-format");

let offsetInMilliSeconds;

const serverTimezoneUTCOffset = () => {
  if (offsetInMilliSeconds) {
    return offsetInMilliSeconds;
  }
  offsetInMilliSeconds = parseInt(JSON.parse($('body').attr('data-timezone')));
  return offsetInMilliSeconds;
};

const formatter = {
  format: (time) => {
    return moment(time).format('DD MMM YYYY [at] HH:mm:ss [Local Time]');
  },

  formatInServerTime: (time) => {
    const format             = 'DD MMM, YYYY [at] HH:mm:ss Z [Server Time]';
    const utcOffsetInMinutes = parseInt(serverTimezoneUTCOffset()) / 60000;
    return moment(time).utcOffset(utcOffsetInMinutes).format(format);
  }
};
module.exports  = formatter;
