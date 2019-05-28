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
const $         = require('jquery');
const CONSTANTS = require('helpers/constants');

const setHeaders = (xhr, version) => {
  xhr.setRequestHeader("Content-Type", "application/json");
  xhr.setRequestHeader("Accept", mrequest.versionHeader(version));
  xhr.setRequestHeader("X-GoCD-Confirm", "true");
  const csrfToken = $('meta[name=csrf-token]').attr('content');
  if (csrfToken) {
    xhr.setRequestHeader('X-CSRF-Token', csrfToken);
  }
};

const mrequest = {
  timeout:   CONSTANTS.SPA_REQUEST_TIMEOUT,
  globalAjaxErrorHandler: () => {
    $(document).ajaxError((_event, jqXHR) => {
      if (jqXHR.status === 401) {
        window.location.pathname = CONSTANTS.AUTH_LOGIN_PATH;
      }
    });
  },
  versionHeader(version) {
    return `application/vnd.go.cd.${version}+json`;
  },
  xhrConfig: {
    v1:         (xhr) => {
      setHeaders(xhr, 'v1');
    },
    v2:         (xhr) => {
      setHeaders(xhr, 'v2');
    },
    v3:         (xhr) => {
      setHeaders(xhr, 'v3');
    },
    v4:         (xhr) => {
      setHeaders(xhr, 'v4');
    },
    forVersion: (version) => (xhr) => {
      setHeaders(xhr, version);
    }
  },

  unwrapMessageOrEntity:     (type, originalEtag) => (data, xhr) => {
    if (xhr.status === 200) {
      const entity = type.fromJSON(data);
      entity.etag(xhr.getResponseHeader('ETag'));
      return entity;
    }
    if (xhr.status === 422 && !!data.data) {
      const fromJSON = new type.fromJSON(data.data);
      fromJSON.etag(originalEtag);
      return fromJSON;
    } else {
      return mrequest.unwrapErrorExtractMessage(data, xhr);
    }
  },
  unwrapMessage:             (data) => {
    if (data && data.message) {
      return data.message;
    }
  },
  unwrapErrorExtractMessage: (data, xhr, defaultMessage = "There was an unknown error performing the operation") => {
    if (mrequest.unwrapMessage(data)) {
      return mrequest.unwrapMessage(data);
    } else {
      if (xhr && xhr.status === 0) {
        return `There was an unknown error performing the operation. Possible reason (${xhr.statusText})`;
      } else {
        return defaultMessage;
      }
    }
  }
};
module.exports = mrequest;

