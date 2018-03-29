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


const m         = require('mithril');
const $         = require('jquery');
const mrequest  = require('helpers/mrequest');

function makeRequest({method, url, apiVersion, type, timeout = mrequest.timeout, payload, contentType = false} = {}) {
  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method,
      url,
      data:       JSON.stringify(payload),
      timeout,
      beforeSend: mrequest.xhrConfig.forVersion(apiVersion),
      contentType
    });

    const didFulfill = (data, _textStatus, _jqXHR) => {
      if (type) {
        deferred.resolve(type.fromJSON(data));
      } else {
        deferred.resolve(data, _textStatus, _jqXHR);
      }
    };

    jqXHR.then(didFulfill, deferred.reject);

    jqXHR.always(m.redraw);
  }).promise();

}

module.exports = class AjaxHelper {

  static GET({url, apiVersion, type, timeout = mrequest.timeout} = {}) {
    return makeRequest({method: 'GET', url, apiVersion, type, timeout});
  }

  static PUT({url, apiVersion, timeout = mrequest.timeout, payload}) {
    return makeRequest({method: 'PUT', url, apiVersion, timeout, payload, contentType: 'application/json'});
  }

  static POST({url, apiVersion, timeout = mrequest.timeout, payload}) {
    return makeRequest({method: 'POST', url, apiVersion, timeout, payload, contentType: 'application/json'});
  }
};


