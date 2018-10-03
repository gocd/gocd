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


const $    = require('jquery');
const meta = $("meta[name='gocd-params']");

module.exports = {
  SERVER_TIMEZONE_UTC_OFFSET: parseInt(meta.attr('data-timezone') || '0'),
  SPA_REQUEST_TIMEOUT:        parseInt(meta.attr('data-page-timeout') || '5000'),
  SPA_REFRESH_INTERVAL:       parseInt(meta.attr('data-page-refresh-interval') || '10000'),
  AUTH_LOGIN_PATH:  '/go/auth/login'
};
