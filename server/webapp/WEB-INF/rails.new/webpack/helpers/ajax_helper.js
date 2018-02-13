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


const m        = require('mithril');
const $        = require('jquery');
const mrequest = require('helpers/mrequest');

function makeRequest({method, url, type, apiVersion, timeout = mrequest.timeout} = {}) {
  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method,
      url,
      timeout,
      beforeSend:  mrequest.xhrConfig.forVersion(apiVersion),
      contentType: false
    });

    const didFulfill = (data, _textStatus, _jqXHR) => {
      deferred.resolve(type ? type.fromJSON(data) : data);
    };

    jqXHR.then(didFulfill, deferred.reject);

    jqXHR.always(m.redraw);
  }).promise();

}


module.exports = class AjaxHelper {
  static GET({url, apiVersion, type, timeout = mrequest.timeout} = {}) {
    return makeRequest({method: 'GET', url, type, apiVersion, timeout});
  }
};


