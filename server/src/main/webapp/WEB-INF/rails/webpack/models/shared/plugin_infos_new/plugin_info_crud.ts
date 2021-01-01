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
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";

export interface PluginInfoQuery {
  include_bad?: boolean;
  type?: ExtensionTypeString;
}

export class PluginInfoCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all(options: PluginInfoQuery): Promise<ApiResult<PluginInfos>> {
    return ApiRequestBuilder.GET(SparkRoutes.apiPluginInfoPath(options), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((str) => {
                                const data = JSON.parse(str);
                                return PluginInfos.fromJSON(data);
                              });
                            });
  }
}
