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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "../../helpers/api_request_builder";
import {SparkRoutes} from "../../helpers/spark_routes";
import {PipelineGroup} from "./admin_pipelines";

export class PipelineGroupCRUD {
  static get(group: string) {
    return ApiRequestBuilder.GET(SparkRoutes.pipelineGroupsPath(group), ApiVersion.latest)
                            .then(this.extractObjectWithEtag);
  }

  static update(groupName: string, updatedPipelineGroup: PipelineGroup, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.pipelineGroupsPath(groupName),
                                 ApiVersion.latest,
                                 {
                                   payload: updatedPipelineGroup, etag
                                 })
                            .then(this.extractObjectWithEtag);
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((str) => {
      return {
        object: PipelineGroup.fromJSON(JSON.parse(str)),
        etag: result.getEtag()
      } as ObjectWithEtag<PipelineGroup>;
    });
  }
}
