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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {JsonUtils} from "helpers/json_utils";
import {SparkRoutes} from "helpers/spark_routes";
import {MailServer} from "models/mail_server/types";

export class MailServerCrud {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static get() {
    return ApiRequestBuilder.GET(SparkRoutes.mailServerConfigPath(), this.API_VERSION_HEADER)
                            .then(this.extractMailServer());
  }

  static createOrUpdate(mailServerConfig: MailServer) {
    return ApiRequestBuilder.POST(SparkRoutes.mailServerConfigPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: JsonUtils.toSnakeCasedObject(mailServerConfig)})
                            .then(this.extractMailServer());
  }

  static delete() {
    return ApiRequestBuilder.DELETE(SparkRoutes.mailServerConfigPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  private static extractMailServer() {
    return (result: ApiResult<string>) => result.map((body) => {
      return MailServer.fromJSON(JsonUtils.toCamelCasedObject(JSON.parse(body)));
    });
  }

}
