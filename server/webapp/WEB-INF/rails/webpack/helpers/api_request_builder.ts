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

export interface HttpResponseWithEtag<T> {
  etag: string;
  object: T;
}

export class ApiRequestBuilder {
  static GET(url: string, apiVersion: ApiVersion, etag?: string) {
    return this.makeRequest(url, "GET", apiVersion, etag);
  }

  static PUT(url: string, apiVersion: ApiVersion, payload: any, etag?: string) {
    return this.makeRequest(url, "PUT", apiVersion, etag, payload);
  }

  static POST(url: string, apiVersion: ApiVersion, payload: any) {
    return this.makeRequest(url, "POST", apiVersion, undefined, payload);
  }

  static PATCH(url: string, apiVersion: ApiVersion, payload: any) {
    return this.makeRequest(url, "PATCH", apiVersion, undefined, payload);
  }

  static DELETE(url: string, apiVersion: ApiVersion, payload?: any) {
    return this.makeRequest(url, "DELETE", apiVersion, undefined, payload);
  }

  private static makeRequest(url: string, method: string, apiVersion: ApiVersion, etag?: string, payload?: any) {
    const headers: { [index: string]: string } = {Accept: this.versionHeader(apiVersion)};

    if (!_.isEmpty(etag)) {
      headers[this.etagHeaderName(method)] = etag as string;
    }

    return m.request<XMLHttpRequest>({url, method, headers, data: payload, extract: _.identity, deserialize: _.identity});
  }

  private static versionHeader(version: ApiVersion): string {
    return `application/vnd.go.cd.${ApiVersion[version]}+json`;
  }

  private static etagHeaderName(method: string) {
    return method.toLowerCase() === "get" || method.toLowerCase() === "head" ? "If-None-Match" : "If-Match";
  }
}
