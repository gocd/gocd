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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {BulkUserOperationJSON, BulkUserUpdateJSON, User, UserJSON, Users, UsersJSON} from "models/users/users";

export class UsersCRUD {
  static API_VERSION_HEADER = ApiVersion.v3;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.apiUsersPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return Users.fromJSON(JSON.parse(body) as UsersJSON);
                              });
                            });
  }

  static create(user: UserJSON) {
    return ApiRequestBuilder.POST(SparkRoutes.apiUsersPath(), this.API_VERSION_HEADER, {payload: user})
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return User.fromJSON(JSON.parse(body) as UserJSON);
                              });
                            });
  }

  static get(username: string) {
    return ApiRequestBuilder.GET(SparkRoutes.apiUserPath(username), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return User.fromJSON(JSON.parse(body) as UserJSON);
                              });
                            });
  }

  static bulkUserStateUpdate(bulkStateChangeJson: BulkUserUpdateJSON) {
    return ApiRequestBuilder.PATCH(SparkRoutes.apiBulkUserStateUpdatePath(), this.API_VERSION_HEADER, {payload: bulkStateChangeJson});
  }

  static bulkUserDelete(bulkStateChangeJson: BulkUserOperationJSON) {
    return ApiRequestBuilder.DELETE(SparkRoutes.apiUsersPath(), this.API_VERSION_HEADER, {payload: bulkStateChangeJson});
  }

}

export class UserSearchCRUD {
  static API_VERSION_HEADER = ApiVersion.v1;

  static search(searchText: string) {
    return ApiRequestBuilder.GET(SparkRoutes.apiUsersSearchPath(searchText), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return (JSON.parse(body) as UsersJSON)._embedded.users;
                              });
                            });
  }
}
