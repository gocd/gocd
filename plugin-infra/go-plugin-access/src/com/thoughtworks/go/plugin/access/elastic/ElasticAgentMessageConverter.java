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

public interface ElasticAgentMessageConverter {
    String canHandlePluginRequestBody(Collection<String> resources, String environment);

    String createAgentRequestBody(String autoRegisterKey, Collection<String> resources, String environment);

    Boolean canHandlePluginResponseFromBody(String responseBody);

    String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, Collection<String> resources, String environment);

    Boolean shouldAssignWorkResponseFromBody(String responseBody);

    String notifyAgentBusyRequestBody(AgentMetadata elasticAgent);

    String notifyAgentIdleRequestBody(AgentMetadata elasticAgent);

    String serverPingRequestBody(Collection<AgentMetadata> metadata);

    Collection<AgentMetadata> deleteAgentRequestBody(String requestBody);
}
