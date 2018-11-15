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

import * as _ from "lodash";
import * as m from "mithril";

export enum ApiVersion {v1, v2, v3, v4, v5, v6, v7, v8}

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
}

export class ApiResult<T> {
  private readonly successResponse?: SuccessResponse<T>;
  private readonly errorResponse?: ErrorResponse;
  private readonly statusCode?: number;
  private readonly etag: string | null;

  private constructor(successResponse?: SuccessResponse<T>,
                      errorResponse?: ErrorResponse,
                      statusCode?: number,
                      etag?: string | null) {
    this.successResponse = successResponse;
    this.errorResponse   = errorResponse;
    this.statusCode      = statusCode;
    this.etag            = etag ? etag : null;
  }

  static from(xhr: XMLHttpRequest) {
    return this.parseResponse(xhr);
  }

  static success(body: string, statusCode: number, etag: string | null) {
    return new ApiResult<string>({body}, undefined, statusCode, etag);
  }

  static error(body: string, message: string, statusCode?: number) {
    return new ApiResult<string>(undefined, {body, message}, statusCode);
  }

  getStatusCode(): number {
    if (this.statusCode) {
      return this.statusCode;
    }
    return -1;
  }

  getEtag(): string | null {
    return this.etag;
  }

  map<U>(func: (x: T) => U): ApiResult<U> {
    if (this.successResponse) {
      const transformedBody = func(this.successResponse.body);
      return new ApiResult<U>({body: transformedBody}, this.errorResponse, this.statusCode, this.etag);
    } else {
      return new ApiResult<U>(this.successResponse, this.errorResponse, this.statusCode, this.etag);
    }
  }

  do(onSuccess: (successResponse: SuccessResponse<T>) => any, onError: (errorResponse: ErrorResponse) => any) {
    if (this.successResponse) {
      onSuccess(this.successResponse);
    } else if (this.errorResponse) {
      onError(this.errorResponse);
    }
  }

  unwrap() {
    if (this.successResponse) {
      return this.successResponse;
    } else {
      return this.errorResponse;
    }
  }

  getOrThrow() {
    if (this.successResponse) {
      return this.successResponse.body;
    } else if (this.errorResponse) {
      throw new Error(this.errorResponse.message);
    } else {
      throw new Error();
    }
  }

  private static parseResponse(xhr: XMLHttpRequest): ApiResult<string> {
    switch (xhr.status) {
      case 200:
      case 201:
        return ApiResult.success(xhr.responseText, xhr.status, xhr.getResponseHeader("etag"));
      case 422:
        return ApiResult.error(xhr.responseText, this.parseMessage(xhr), xhr.status);
    }
    return ApiResult.error(xhr.responseText, `There was an unknown error performing the operation. Possible reason (${xhr.statusText})`, xhr.status);
  }

  private static parseMessage(xhr: XMLHttpRequest) {
    if (xhr.response.data && xhr.response.data.message) {
      return xhr.response.data.message;
    }
    return `There was an unknown error performing the operation. Possible reason (${xhr.statusText})`;
  }
}

interface Headers {
  [key: string]: string;
}

export class ApiRequestBuilder {
  static GET(url: string, apiVersion?: ApiVersion, etag?: string, headers?: Headers) {
    return this.makeRequest(url, "GET", apiVersion, etag, headers);
  }

  static PUT(url: string, apiVersion?: ApiVersion, payload?: any, etag?: string, headers?: Headers) {
    return this.makeRequest(url, "PUT", apiVersion, etag, payload, headers);
  }

  static POST(url: string, apiVersion?: ApiVersion, payload?: any, headers?: Headers) {
    return this.makeRequest(url, "POST", apiVersion, undefined, payload, headers);
  }

  static PATCH(url: string, apiVersion?: ApiVersion, payload?: any, headers?: Headers) {
    return this.makeRequest(url, "PATCH", apiVersion, undefined, payload, headers);
  }

  static DELETE(url: string, apiVersion?: ApiVersion, payload?: any, headers?: Headers) {
    return this.makeRequest(url, "DELETE", apiVersion, undefined, payload, headers);
  }

  private static makeRequest(url: string,
                             method: string,
                             apiVersion?: ApiVersion,
                             etag?: string,
                             payload?: any,
                             headers?: Headers): Promise<ApiResult<string>> {
    headers = _.assign({}, headers);

    if (apiVersion !== null) {
      headers.Accept = this.versionHeader(apiVersion as ApiVersion);
    }

    if (!_.isEmpty(etag)) {
      headers[this.etagHeaderName(method)] = etag as string;
    }

    return m.request<XMLHttpRequest>({
      url,
      method,
      headers,
      data: payload,
      extract: _.identity,
      deserialize: _.identity
    }).then((xhr: XMLHttpRequest) => {
      return ApiResult.from(xhr);
    }).catch((reason) => {
      try {
        return ApiResult.error(reason.responseText, JSON.parse(reason.message).data.message, reason.status);
      } catch {
        return ApiResult.error(reason.responseText, "There was an unknown error performing the operation.", reason.status);
      }
    });
  }

  private static versionHeader(version: ApiVersion): string {
    return `application/vnd.go.cd.${ApiVersion[version]}+json`;
  }

  private static etagHeaderName(method: string) {
    return method.toLowerCase() === "get" || method.toLowerCase() === "head" ? "If-None-Match" : "If-Match";
  }
}
