/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import m from 'mithril';
import $ from 'jquery';
import {mrequest} from "helpers/mrequest";

function makeRequest({method, url, apiVersion, type, timeout = mrequest.timeout, payload, etag, contentType = false} = {}) {
  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method,
      url,
      data:       JSON.stringify(payload),
      timeout,
      beforeSend: (xhr) => {
        xhr.setRequestHeader("GET" === method.toUpperCase() ? 'If-None-Match' : 'If-Match', etag);
        mrequest.xhrConfig.forVersion(apiVersion)(xhr);
      },
      contentType
    });

    const didFulfill = (data, _textStatus, jqXHR) => {
      const NOT_MODIFIED = 304 === jqXHR.status;

      if (type) {
        deferred.resolve(NOT_MODIFIED ? undefined : type.fromJSON(data, jqXHR), jqXHR);
      } else {
        deferred.resolve(data, _textStatus, jqXHR);
      }
    };

    jqXHR.then(didFulfill, deferred.reject);

    jqXHR.always(m.redraw);
  }).promise();

}

export const AjaxHelper = {
  GET({url, apiVersion, type, timeout = mrequest.timeout, etag} = {}) {
    return makeRequest({method: 'GET', url, apiVersion, type, timeout, etag});
  },

  PUT({url, apiVersion, timeout = mrequest.timeout, payload, etag, contentType = 'application/json'}) {
    return makeRequest({method: 'PUT', url, apiVersion, timeout, payload, etag, contentType});
  },

  POST({url, apiVersion, timeout = mrequest.timeout, payload, etag, type, contentType = 'application/json'}) {
    return makeRequest({method: 'POST', url, apiVersion, timeout, type, payload, etag, contentType});
  },

  PATCH({url, apiVersion, timeout = mrequest.timeout, payload, type, etag, contentType = 'application/json'}) {
    return makeRequest({method: 'PATCH', url, apiVersion, timeout, payload, type, etag, contentType});
  },

  DELETE({url, apiVersion, type, timeout = mrequest.timeout, etag} = {}) {
    return makeRequest({method: 'DELETE', url, apiVersion, type, timeout, etag});
  }
};
