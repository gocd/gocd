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
import {AccessToken, AccessTokens, AccessTokensJSON} from "models/access_tokens/types";

export class AccessTokenCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static all(filter: "all" | "revoked" | "active" = "all") {
    return ApiRequestBuilder.GET(SparkRoutes.apiCurrentAccessTokensPath(filter), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return AccessTokens.fromJSON(JSON.parse(body) as AccessTokensJSON);
                              });
                            });
  }

  static create(accessToken: AccessToken) {
    return ApiRequestBuilder.POST(SparkRoutes.apiCurrentAccessTokensPath(), this.API_VERSION_HEADER,
                                  {payload: accessToken}).then(this.extractObjectWithEtag);
  }

  static revoke(accessToken: AccessToken, revoke_cause: string) {
    return ApiRequestBuilder.POST(SparkRoutes.apiCurrentAccessTokenRevokePath(accessToken.id()),
                                  this.API_VERSION_HEADER,
                                  {payload: {revoke_cause}}).then(this.extractObjectWithEtag);
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const accessTokenJSON = JSON.parse(body);
      return {
        object: AccessToken.fromJSON(accessTokenJSON),
        etag: result.getEtag()
      } as ObjectWithEtag<AccessToken>;
    });
  }
}
