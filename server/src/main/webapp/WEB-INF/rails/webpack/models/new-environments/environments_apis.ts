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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {EnvironmentVariableJSON} from "models/environment_variables/types";
import {PipelineStructure} from "models/internal_pipeline_structure/pipeline_structure";
import {EnvironmentJSON, Environments, EnvironmentWithOrigin} from "models/new-environments/environments";

export class EnvironmentsAPIs {
  private static LATEST_API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalMergedEnvironmentsPath(), this.LATEST_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => Environments.fromJSON(JSON.parse(body)));
                            });
  }

  static allPipelines(groupAuthorization: "view" | "operate" | "administer",
                      templateAuthorization: "view" | "administer") {
    return ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalPipelinesListPath(groupAuthorization,
                                                                               templateAuthorization),
                                 this.LATEST_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => PipelineStructure.fromJSON(JSON.parse(body)));
                            });
  }

  static allPipelineGroups(groupAuthorization: "view" | "operate" | "administer") {
    return ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalPipelineGroupsPath(groupAuthorization), this.LATEST_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => PipelineStructure.fromJSON(JSON.parse(body)));
                            });
  }

  static delete(name: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.apiAdminEnvironmentsPath(name), this.LATEST_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static patch(name: string, payload: EnvironmentPatchJson) {
    return ApiRequestBuilder.PATCH(SparkRoutes.apiAdminEnvironmentsPath(name),
                                   this.LATEST_API_VERSION_HEADER,
                                   {payload});
  }

  static create(environment: EnvironmentWithOrigin) {
    return ApiRequestBuilder.POST(SparkRoutes.apiEnvironmentPath(),
                                  this.LATEST_API_VERSION_HEADER,
                                  {payload: environment})
                            .then(this.extractObjectWithEtag());
  }

  static updateAgentAssociation(envName: string, agentUuidsToAssociate: string[], agentUuidsToRemove: string[]) {
    return ApiRequestBuilder.PATCH(SparkRoutes.apiAdminInternalEnvironmentsPath(envName), this.LATEST_API_VERSION_HEADER, {
      payload: {
        agents: {
          add: agentUuidsToAssociate,
          remove: agentUuidsToRemove
        }
      }
    });
  }

  private static extractObjectWithEtag() {
    return (result: ApiResult<string>) => {
      return result.map((body) => {
        const environmentJSON = JSON.parse(body) as EnvironmentJSON;
        return {
          object: EnvironmentWithOrigin.fromJSON(environmentJSON),
          etag: result.getEtag()
        } as ObjectWithEtag<EnvironmentWithOrigin>;
      });
    };
  }
}

export interface EnvironmentPatchJson {
  environment_variables?: {
    add: EnvironmentVariableJSON[],
    remove: string[]
  };
  pipelines?: {
    add: string[],
    remove: string[]
  };
}
