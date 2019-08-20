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
import {SparkRoutes} from "helpers/spark_routes";
import {AgentConfigState, Agents} from "models/new-agent/agents";

export class AgentsCRUD {
  private static API_VERSION_HEADER = ApiVersion.v5;

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
                                   this.patchPayload(agentsUUID, AgentConfigState.Enabled));
  }

  static agentsToDisable(agentsUUID: string[]) {
    return ApiRequestBuilder.PATCH(SparkRoutes.agentsPath(),
                                   this.API_VERSION_HEADER,
                                   this.patchPayload(agentsUUID, AgentConfigState.Disabled));
  }

  private static patchPayload(agentsUUID: string[], agentConfigState: AgentConfigState) {
    return {
      payload: {
        uuids: agentsUUID,
        agent_config_state: AgentConfigState[agentConfigState]
      }
    };
  }
}
