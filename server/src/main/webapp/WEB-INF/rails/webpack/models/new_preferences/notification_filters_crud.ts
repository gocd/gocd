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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {NotificationFilter, NotificationFilterJSON, NotificationFilters, NotificationFiltersJSON} from "./notification_filters";

export class NotificationFiltersCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.notificationFilterAPIPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body) as NotificationFiltersJSON;
                              return NotificationFilters.fromJSON(data._embedded.filters);
                            }));
  }

  static get(repoId: number) {
    return ApiRequestBuilder.GET(SparkRoutes.notificationFilterAPIPath(repoId), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static create(filter: NotificationFilter) {
    return ApiRequestBuilder.POST(SparkRoutes.notificationFilterAPIPath(), this.API_VERSION_HEADER,
                                  {payload: filter})
                            .then(this.extractObjectWithEtag);
  }

  static update(notificationFilter: NotificationFilter) {
    return ApiRequestBuilder.PATCH(SparkRoutes.notificationFilterAPIPath(notificationFilter.id()),
                                   this.API_VERSION_HEADER,
                                   {payload: notificationFilter})
                            .then(this.extractObjectWithEtag);
  }

  static delete(filterId: number) {
    return ApiRequestBuilder.DELETE(SparkRoutes.notificationFilterAPIPath(filterId), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const notificationFilterJSON = JSON.parse(body) as NotificationFilterJSON;
      return {
        object: NotificationFilter.fromJSON(notificationFilterJSON),
        etag:   result.getEtag()
      } as ObjectWithEtag<NotificationFilter>;
    });
  }
}
