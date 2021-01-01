/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {CaseInsensitiveMap} from "helpers/collections";
import _ from "lodash";
import m from "mithril";

export enum ApiVersion { latest, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10}

export interface ObjectWithEtag<T> {
  etag: string;
  object: T;
}

export interface SuccessResponse<T> {
  body: T;
}

export interface ErrorResponse {
  message: string; //more fields can be added if needed
  body?: string;
  data?: object;
}

export class ApiResult<T> {
  private readonly successResponse?: SuccessResponse<T>;
  private readonly errorResponse?: ErrorResponse;
  private readonly statusCode: number;
  private readonly headers: Map<string, string>;

  private constructor(successResponse: SuccessResponse<T> | undefined,
                      errorResponse: ErrorResponse | undefined,
                      statusCode: number,
                      headers: Map<string, string>) {
    this.successResponse = successResponse;
    this.errorResponse   = errorResponse;
    this.statusCode      = statusCode;
    this.headers         = headers;
  }

  static from(xhr: XMLHttpRequest) {
    return this.parseResponse(xhr);
  }

  static success(body: string, statusCode: number, headers: Map<string, string>) {
    return new ApiResult<string>({body}, undefined, statusCode, headers);
  }

  static accepted(body: string, statusCode: number, headers: Map<string, string>) {
    return new ApiResult<string>({body}, undefined, statusCode, headers);
  }

  static error(body: string, message: string, statusCode: number, headers: Map<string, string>) {
    return new ApiResult<string>(undefined, {body, message}, statusCode, headers);
  }

  getStatusCode(): number {
    if (this.statusCode) {
      return this.statusCode;
    }
    return -1;
  }

  header(name: string) {
    return this.headers.get(name);
  }

  getEtag(): string | null {
    return this.header("etag") || null;
  }

  getRedirectUrl(): string {
    return this.header("Location") || "";
  }

  getRetryAfterIntervalInMillis(): number {
    return Number(this.header("retry-after") || 0) * 1000;
  }

  map<U>(func: (x: T) => U): ApiResult<U> {
    if (this.successResponse) {
      const transformedBody = func(this.successResponse.body);
      return new ApiResult<U>({body: transformedBody}, this.errorResponse, this.statusCode, this.headers);
    } else {
      return new ApiResult<U>(this.successResponse, this.errorResponse, this.statusCode, this.headers);
    }
  }

  do(onSuccess: (successResponse: SuccessResponse<T>) => any, onError: (errorResponse: ErrorResponse) => any = () => { /* donothing */}) {
    if (this.successResponse) {
      return onSuccess(this.successResponse);
    } else if (this.errorResponse) {
      return onError(this.errorResponse);
    }
  }

  unwrap() {
    if (this.successResponse) {
      return this.successResponse;
    } else {
      if (this.errorResponse!.body) {
        try {
          return JSON.parse(this.errorResponse!.body);
        } catch (e) {
          //may be parse of the json failed, return the string response as is..
          return this.errorResponse!.body;
        }
      }

      return this.errorResponse;
    }
  }

  getOrThrow() {
    if (this.successResponse) {
      return this.successResponse.body;
    } else if (this.errorResponse) {
      throw new Error(JSON.parse(this.errorResponse.body!).message);
    } else {
      throw new Error();
    }
  }

  private static parseResponse(xhr: XMLHttpRequest): ApiResult<string> {
    const headers = allHeaders(xhr);

    switch (xhr.status) {
      case 200:
      case 201:
        return ApiResult.success(xhr.responseText, xhr.status, headers);
      case 202:
        return ApiResult.accepted(xhr.responseText, xhr.status, headers);
      case 422:
      case 503:
        return ApiResult.error(xhr.responseText, parseMessage(xhr), xhr.status, headers);
    }

    return ApiResult.error(xhr.responseText,
                           `There was an unknown error performing the operation. Possible reason (${xhr.statusText})`,
                           xhr.status, headers);
  }
}

function allHeaders(xhr: XMLHttpRequest): Map<string, string> {
  const payload = xhr.getAllResponseHeaders();
  const headers = new CaseInsensitiveMap<string>();

  for (const line of payload.trim().split(/[\r\n]+/)) {
    const parts = line.split(": ");
    headers.set(parts.shift()!, parts.join(": "));
  }

  return headers;
}

function parseMessage(xhr: XMLHttpRequest) {
  if (xhr.response.data && xhr.response.data.message) {
    return xhr.response.data.message;
  }
  return `There was an unknown error performing the operation. Possible reason (${xhr.statusText})`;
}

interface Headers {
  [key: string]: string;
}

export interface RequestOptions {
  etag?: string;
  payload: any;
  headers?: Headers;
  xhrHandle?: (xhr: XMLHttpRequest) => void; //A reference to the underlying XHR object, which can be used to abort the request
}

export class ApiRequestBuilder {
  static GET(url: string, apiVersion?: ApiVersion, options?: Partial<RequestOptions>) {
    return this.makeRequest(url, "GET", apiVersion, options);
  }

  static PUT(url: string, apiVersion?: ApiVersion, options?: Partial<RequestOptions>) {
    return this.makeRequest(url, "PUT", apiVersion, options);
  }

  static POST(url: string, apiVersion?: ApiVersion, options?: Partial<RequestOptions>) {
    return this.makeRequest(url, "POST", apiVersion, options);
  }

  static PATCH(url: string, apiVersion?: ApiVersion, options?: Partial<RequestOptions>) {
    return this.makeRequest(url, "PATCH", apiVersion, options);
  }

  static DELETE(url: string, apiVersion?: ApiVersion, options?: Partial<RequestOptions>) {
    return this.makeRequest(url, "DELETE", apiVersion, options);
  }

  static versionHeader(version: ApiVersion): string {
    if (version === ApiVersion.latest) {
      return `application/vnd.go.cd+json`;
    } else {
      return `application/vnd.go.cd.${ApiVersion[version]}+json`;
    }
  }

  private static makeRequest(url: string,
                             method: string,
                             apiVersion?: ApiVersion,
                             options?: Partial<RequestOptions>): Promise<ApiResult<string>> {
    const headers = this.buildHeaders(method, apiVersion, options);

    let payload: any;
    if (options && options.payload) {
      payload = options.payload;
    }

    return m.request<XMLHttpRequest>({
                                       url,
                                       method,
                                       headers,
                                       body: payload,
                                       extract: _.identity,
                                       deserialize: _.identity,
                                       config: (xhr) => {
                                         if (options && options.xhrHandle) {
                                           options.xhrHandle(xhr);
                                         }
                                       }
                                     }).then((xhr: XMLHttpRequest) => {
      return ApiResult.from(xhr);
    }).catch((reason) => {
      const unknownError = "There was an unknown error performing the operation.";
      try {
        return ApiResult.error(reason.responseText,
                               JSON.parse(reason.message).message || unknownError,
                               reason.status,
                               new Map());
      } catch {
        return ApiResult.error(reason.responseText, unknownError, reason.status, new Map());
      }
    });
  }

  private static buildHeaders(method: string, apiVersion?: ApiVersion, options?: Partial<RequestOptions>) {
    let headers: Headers = {};
    if (options && options.headers) {
      headers = _.assign({}, options.headers);
    }

    if (apiVersion !== undefined) {
      headers.Accept              = this.versionHeader(apiVersion as ApiVersion);
      headers["X-Requested-With"] = "XMLHttpRequest";
    }

    if (options && !_.isEmpty(options.etag)) {
      headers[this.etagHeaderName(method)] = options.etag as string;
    }

    if ((!options || !options.payload) && ApiRequestBuilder.isAnUpdate(method)) {
      headers["X-GoCD-Confirm"] = "true";
    }

    return headers;
  }

  private static isAnUpdate(method: string) {
    const updateMethods = ["PUT", "POST", "DELETE", "PATCH"];
    return updateMethods.includes(method.toUpperCase());
  }

  private static etagHeaderName(method: string) {
    return method.toLowerCase() === "get" || method.toLowerCase() === "head" ? "If-None-Match" : "If-Match";
  }
}
