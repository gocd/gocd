/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "../../helpers/api_request_builder";
import {SparkRoutes} from "../../helpers/spark_routes";
import {deconstructJobIdentifier, JobIdentifier} from "../shared/job_identifier";
import {JobInstanceJSON} from "./job_detail";

export class JobDetailCRUD {
  static get(jobIdentifier: JobIdentifier): Promise<ApiResult<JobInstanceJSON>> {
    return ApiRequestBuilder.GET(SparkRoutes.apiJobInstancePath(...deconstructJobIdentifier(jobIdentifier)), ApiVersion.latest)
      .then((result: ApiResult<string>) => {
        return result.map((body) => {
          return JSON.parse(body) as JobInstanceJSON;
        });
      });
  }
}
