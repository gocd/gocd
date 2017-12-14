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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ElasticAgentMessageConverter {

    String createAgentRequestBody(String autoRegisterKey, String environment, Map<String, String> configuration, JobIdentifier jobIdentifier);

    Boolean canHandlePluginResponseFromBody(String responseBody);

    String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, String environment, Map<String, String> configuration, JobIdentifier identifier);

    Boolean shouldAssignWorkResponseFromBody(String responseBody);

    String listAgentsResponseBody(Collection<AgentMetadata> metadata);

    Collection<AgentMetadata> deleteAndDisableAgentRequestBody(String requestBody);

    List<PluginConfiguration> getProfileMetadataResponseFromBody(String responseBody);

    String getProfileViewResponseFromBody(String responseBody);

    ValidationResult getValidationResultResponseFromBody(String responseBody);

    String validateRequestBody(Map<String, String> configuration);

    com.thoughtworks.go.plugin.domain.common.Image getImageResponseFromBody(String responseBody);
}
