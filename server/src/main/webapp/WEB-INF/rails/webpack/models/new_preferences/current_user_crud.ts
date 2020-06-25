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
import {CurrentUser, CurrentUserJSON} from "./current_user";

export class CurrentUserCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static get() {
    return ApiRequestBuilder.GET(SparkRoutes.currentUserAPI(), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static update(user: CurrentUser, etag: string) {
    return ApiRequestBuilder.PATCH(SparkRoutes.currentUserAPI(),
                                   this.API_VERSION_HEADER,
                                   {payload: user.toUpdateApiJSON(), etag})
                            .then(this.extractObjectWithEtag);
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const userJSON = JSON.parse(body) as CurrentUserJSON;
      return {
        object: CurrentUser.fromJSON(userJSON),
        etag:   result.getEtag()
      } as ObjectWithEtag<CurrentUser>;
    });
  }
}
