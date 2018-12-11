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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import SparkRoutes from "helpers/spark_routes";
import {DrainModeSettings, DrainModeSettingsJSON} from "models/drain_mode/drain_mode_settings";
import {DrainModeInfo, DrainModeInfoJSON} from "models/drain_mode/types";

export class DrainModeCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static get() {
    return ApiRequestBuilder
      .GET(SparkRoutes.drainModeSettingsPath(), this.API_VERSION_HEADER)
      .then(this.extractSettingsObject());
  }

  static update(drainModeSettings: DrainModeSettings) {
    return ApiRequestBuilder.POST(
      SparkRoutes.drainModeSettingsPath(),
      this.API_VERSION_HEADER,
      {payload: drainModeSettings.toSnakeCaseJSON()}
    ).then(this.extractSettingsObject());
  }

  static info() {
    return ApiRequestBuilder.GET(SparkRoutes.drainModeInfoPath(), this.API_VERSION_HEADER)
                            .then(DrainModeCRUD.extractInfoObject());
  }

  private static extractSettingsObject() {
    return (result: ApiResult<string>): ApiResult<DrainModeSettings> => {
      return result.map<DrainModeSettings>((body) => DrainModeSettings.fromJSON(JSON.parse(body) as DrainModeSettingsJSON));
    };
  }

  private static extractInfoObject() {
    return (result: ApiResult<string>): ApiResult<DrainModeInfo> => {
      return result.map<DrainModeInfo>((body) => DrainModeInfo.fromJSON(JSON.parse(body) as DrainModeInfoJSON));
    };
  }
}
