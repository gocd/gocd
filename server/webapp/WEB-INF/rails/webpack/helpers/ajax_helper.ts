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

const m = require("mithril");
const mrequest = require("helpers/mrequest");

import * as $ from "jquery";
import "jquery";

const DEFAULT_TIMEOUT = mrequest.timeout;
const DEFAULT_MIMETYPE = "application/json";

type xhr = JQuery.jqXHR<any>;

interface RequestConfig {
  url: string,
  apiVersion: string,
  type?: any,
  timeout?: number,
  payload?: any,
  etag?: string | null,
  contentType?: string | false,
}

type AjaxTypedResolver = (data: object, xhr: xhr) => any;
type AjaxUntypedResolver = (data: object, status: string, xhr: xhr) => any;

type AjaxResolver = AjaxTypedResolver | AjaxUntypedResolver | ((data: object) => any);
type AjaxRejector = (xhr: xhr, errorText?: string, errorThrown?: any) => any;

// JQuery.Promise is super complex, and perhaps not specific
// enough (or, at least, we could not figure out how to make it
// specify the exact parameters for reject/resolve)
interface AjaxPromiseLike {
  then: (resolve: AjaxResolver, reject?: AjaxRejector) => any,
  always: (...args: any[]) => any
}

type AjaxCRUDMethod = (config: RequestConfig) => AjaxPromiseLike;

interface mkReqArgs extends RequestConfig {
  method: string
}

function makeRequest({method, url, apiVersion, type, timeout=DEFAULT_TIMEOUT, payload, etag, contentType=false}: mkReqArgs): AjaxPromiseLike {
  return <AjaxPromiseLike>($.Deferred(function doRequest(this: JQuery.Deferred<any>) {
    const deferred = this;

    const xhr: xhr = $.ajax({
      method,
      url,
      data:       JSON.stringify(payload),
      timeout,
      beforeSend: (xhr: xhr) => {
        if (etag) {
          xhr.setRequestHeader("GET" === method.toUpperCase() ? 'If-None-Match' : 'If-Match', etag);
        }
        mrequest.xhrConfig.forVersion(apiVersion)(xhr);
      },
      contentType
    });

    const resolve: AjaxResolver = (data:object, _st:string, xhr:xhr) => {
      const NOT_MODIFIED = 304 === xhr.status;

      if (type) {
        deferred.resolve(NOT_MODIFIED ? undefined : type.fromJSON(data, xhr), xhr);
      } else {
        deferred.resolve(data, _st, xhr);
      }
    };

    xhr.then(resolve, deferred.reject);
    xhr.always(m.redraw);
  }).promise());
}

export default class AjaxHelper {
  static GET: AjaxCRUDMethod = ({url, apiVersion, type, timeout=DEFAULT_TIMEOUT, etag}: RequestConfig) =>
    makeRequest({method: "GET", url, apiVersion, type, timeout, etag});

  static PUT: AjaxCRUDMethod = ({url, apiVersion, timeout=DEFAULT_TIMEOUT, payload, etag, contentType=DEFAULT_MIMETYPE}: RequestConfig) =>
    makeRequest({method: "PUT", url, apiVersion, timeout, payload, etag, contentType});

  static POST: AjaxCRUDMethod = ({url, apiVersion, timeout=DEFAULT_TIMEOUT, payload, etag, type, contentType=DEFAULT_MIMETYPE}: RequestConfig) =>
    makeRequest({method: "POST", url, apiVersion, timeout, type, payload, etag, contentType});

  static PATCH: AjaxCRUDMethod = ({url, apiVersion, timeout=DEFAULT_TIMEOUT, payload, type, etag, contentType=DEFAULT_MIMETYPE}: RequestConfig) =>
    makeRequest({method: "PATCH", url, apiVersion, timeout, payload, type, etag, contentType});

  static DELETE: AjaxCRUDMethod = ({url, apiVersion, type, timeout=DEFAULT_TIMEOUT, etag}: RequestConfig) =>
    makeRequest({method: "DELETE", url, apiVersion, type, timeout, etag});
};

export { RequestConfig, AjaxPromiseLike, AjaxResolver, AjaxRejector };
