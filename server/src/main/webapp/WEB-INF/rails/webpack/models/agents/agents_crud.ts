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

import {ApiRequestBuilder, ApiResult, ApiVersion, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {AgentConfigState, Agents} from "models/agents/agents";

export class AgentsCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.agentsPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return Agents.fromJSON(JSON.parse(body));
                            }));
  }

  static delete(agentUUID: string[]) {
    return ApiRequestBuilder.DELETE(SparkRoutes.agentsPath(), this.API_VERSION_HEADER, {payload: {uuids: agentUUID}});
  }

  static agentsToEnable(agentsUUID: string[]) {
    return ApiRequestBuilder.PATCH(SparkRoutes.agentsPath(),
                                   this.API_VERSION_HEADER,
                                   this.patchPayloadWithConfigState(agentsUUID, AgentConfigState.Enabled));
  }

  static agentsToDisable(agentsUUID: string[]) {
    return ApiRequestBuilder.PATCH(SparkRoutes.agentsPath(),
                                   this.API_VERSION_HEADER,
                                   this.patchPayloadWithConfigState(agentsUUID, AgentConfigState.Disabled));
  }

  static updateEnvironmentsAssociation(agentsUUID: string[],
                                       environmentsToAdd: string[],
                                       environmentsToRemove: string[]) {
    return ApiRequestBuilder.PATCH(SparkRoutes.agentsPath(),
                                   this.API_VERSION_HEADER,
                                   this.patchPayloadEnvironments(agentsUUID, environmentsToAdd, environmentsToRemove));
  }

  static updateResources(agentsUUID: string[],
                         resourcesToAdd: string[], resourcesToRemove: string[]) {
    return ApiRequestBuilder.PATCH(SparkRoutes.agentsPath(),
                                   this.API_VERSION_HEADER,
                                   this.patchPayloadResources(agentsUUID, resourcesToAdd, resourcesToRemove));
  }

  private static patchPayloadWithConfigState(agentsUUID: string[], agentConfigState: AgentConfigState) {
    return {payload: {uuids: agentsUUID, agent_config_state: AgentConfigState[agentConfigState]}};
  }

  private static patchPayloadEnvironments(agentsUUID: string[], environmentsToAdd: string[],
                                          environmentsToRemove: string[]) {
    return {
      payload: {
        uuids: agentsUUID,
        operations: {environments: {add: environmentsToAdd, remove: environmentsToRemove}}
      }
    };
  }

  private static patchPayloadResources(agentsUUID: string[], resourcesToAdd: string[], resourcesToRemove: string[]) {
    return {
      payload: {
        uuids: agentsUUID,
        operations: {resources: {add: resourcesToAdd, remove: resourcesToRemove}}
      }
    };
  }
}

export interface GetAllService {
  all(onSuccess: (data: string) => void, onError: (message: string) => void): void;
}

export class EnvironmentsService implements GetAllService {
  all(onSuccess: (data: string) => void, onError: (message: string) => void): void {
    ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalEnvironmentsPath(), ApiVersion.v1)
                     .then((result: ApiResult<string>) => {
                       result.do((successResponse: SuccessResponse<string>) => onSuccess(successResponse.body),
                                 (errorResponse: ErrorResponse) => onError(errorResponse.message));
                     });
  }
}

export class ResourcesService implements GetAllService {
  all(onSuccess: (data: string) => void, onError: (message: string) => void): void {
    ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalResourcesPath(), ApiVersion.v1)
                     .then((result: ApiResult<string>) => {
                       result.do((successResponse: SuccessResponse<string>) => onSuccess(successResponse.body),
                                 (errorResponse: ErrorResponse) => onError(errorResponse.message));
                     });
  }
}
