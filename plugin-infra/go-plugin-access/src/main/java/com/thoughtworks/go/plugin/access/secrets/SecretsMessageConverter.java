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
package com.thoughtworks.go.plugin.access.secrets;

import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.secrets.Secret;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SecretsMessageConverter {
    Image getImageFromResponseBody(String responseBody);

    List<PluginConfiguration> getSecretsConfigMetadataFromResponse(String responseBody);

    String getSecretsConfigViewFromResponse(String responseBody);

    ValidationResult getSecretsConfigValidationResultFromResponse(String responseBody);

    String validatePluginConfigurationRequestBody(Map<String, String> configuration);

    String lookupSecretsRequestBody(Set<String> lookupStrings, Map<String, String> configurationAsMap);

    List<Secret> getSecretsFromResponse(String responseBody);

    String getErrorMessageFromResponse(String responseBody);
}
