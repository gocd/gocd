/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

import java.util.Collection;
import java.util.Map;

public interface ElasticAgentMessageConverter {

    String createAgentRequestBody(String autoRegisterKey, String environment, Map<String, String> configuration);

    Boolean canHandlePluginResponseFromBody(String responseBody);

    String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, String environment, Map<String, String> configuration);

    Boolean shouldAssignWorkResponseFromBody(String responseBody);

    String listAgentsResponseBody(Collection<AgentMetadata> metadata);

    Collection<AgentMetadata> deleteAndDisableAgentRequestBody(String requestBody);

}
