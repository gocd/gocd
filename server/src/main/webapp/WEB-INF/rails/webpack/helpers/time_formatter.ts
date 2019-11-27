/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import * as CONSTANTS from "helpers/constants";
import _ from "lodash";
import {LRUMap} from "lru_map";
import moment from "moment";
import "moment-duration-format";

const utcOffsetInMinutes = CONSTANTS.SERVER_TIMEZONE_UTC_OFFSET / 60000;
const CACHE_SIZE         = 10000;
const DATE_FORMAT        = "DD MMM YYYY";
const LOCAL_TIME_FORMAT  = "DD MMM, YYYY [at] HH:mm:ss [Local Time]";
const SERVER_TIME_FORMAT = "DD MMM, YYYY [at] HH:mm:ss Z [Server Time]";
// the default timestamp format rendered by the server
const defaultFormat      = "YYYY-MM-DDTHH:mm:ssZ";

const format = _.memoize((time) => {
  return moment(time, defaultFormat).format(LOCAL_TIME_FORMAT);
});
format.cache = new LRUMap(CACHE_SIZE);

const formatInServerTime = _.memoize((time) => {
  return moment(time, defaultFormat).utcOffset(utcOffsetInMinutes).format(SERVER_TIME_FORMAT);
});
formatInServerTime.cache = new LRUMap(CACHE_SIZE);

const formatInDate = (time?: moment.MomentInput) => {
  return moment(time, defaultFormat).format(DATE_FORMAT);
};

const toDate = (time?: moment.MomentInput) => {
  return moment(time, defaultFormat).toDate();
};

function formattedDuration(diff: moment.MomentInput) {
  return moment.utc(diff).format("HH:mm:ss");
}

const formattedTimeDiff = (from: moment.MomentInput, to: moment.MomentInput) => {
  const start = moment(from, defaultFormat);
  const end   = moment(to, defaultFormat);
  const diff  = end.diff(start);

  return formattedDuration(diff);
};

export const timeFormatter = {
  format,
  toDate,
  formatInDate,
  formatInServerTime,
  formattedTimeDiff,
  formattedDuration
};
