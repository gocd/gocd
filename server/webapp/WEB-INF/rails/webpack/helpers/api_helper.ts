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

const AjaxHelper = require("helpers/ajax_helper");
const parseError = require("helpers/mrequest").unwrapErrorExtractMessage;
const Dfr        = require("jquery").Deferred;

import "jquery";

interface RequestConfig {
  url: string,
  apiVersion: string,
  type?: any,
  timeout?: number,
  payload?: any,
  etag?: string | null | undefined,
  contentType?: boolean | string | undefined,
}

type xhr = JQuery.jqXHR<any>;
type AjaxTypedResolver = (data: any, xhr: xhr) => any;
type AjaxUntypedResolver = (data: any, status: string, xhr: xhr) => any;

type AjaxResolver = AjaxTypedResolver | AjaxUntypedResolver;
type AjaxRejector = (xhr: xhr, errorText?: string, errorThrown?: any) => any;

// JQuery.Promise is super complex, and perhaps not specific
// enough (or, at least, we could not figure out how to make it
// specify the exact parameters for reject/resolve)
interface AjaxPromiseLike {
  then: (resolve: AjaxResolver, reject?: AjaxRejector) => any,
  always: () => any
}

type AjaxPromiser = () => AjaxPromiseLike;

type ApiResolver = ((d: any) => any) |
  ((d: any, etag: string) => any) |
  ((d: any, etag: string, status: number) => any);
type ApiRejector = ((errorMessage: string) => any) | ((errorMessage: string, status: number) => any);

interface ApiPromiseLike {
  then: (resolve: ApiResolver, reject?: ApiRejector) => any,
  always: () => any
}

type ApiCRUDMethod = (config: RequestConfig) => ApiPromiseLike;

/** This helper parses data, etags, and errors for a convenient API */
function req(exec: AjaxPromiser, config: RequestConfig): ApiPromiseLike {
  return Dfr(function ajax(this: JQuery.Deferred<any>) {
    const success: AjaxResolver = config.type ?
      (instance: any, xhr: xhr) => this.resolve(instance, parseEtag(xhr), xhr.status) :
      (data: any, _s: string, xhr: xhr) => this.resolve(data, parseEtag(xhr), xhr.status);
    const failure: AjaxRejector = (xhr) => this.reject(parseError(xhr.responseJSON, xhr), xhr.status);

    exec().then(success, failure);
  }).promise();
}

function parseEtag(req: xhr): string { return (req.getResponseHeader("ETag") || "").replace(/--(gzip|deflate)/, ""); }

class ApiHelper {
  static GET: ApiCRUDMethod = (config: RequestConfig) => req(() => AjaxHelper.GET(config), config);
  static PUT: ApiCRUDMethod = (config: RequestConfig) => req(() => AjaxHelper.PUT(config), config);
  static POST: ApiCRUDMethod = (config: RequestConfig) => req(() => AjaxHelper.POST(config), config);
  static PATCH: ApiCRUDMethod = (config: RequestConfig) => req(() => AjaxHelper.PATCH(config), config);
  static DELETE: ApiCRUDMethod = (config: RequestConfig) => req(() => AjaxHelper.DELETE(config), config);
};

export {
  ApiPromiseLike
};

export default ApiHelper;
