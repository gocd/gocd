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
package com.thoughtworks.go.plugin.access.secrets;

import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.secrets.Secret;

import java.util.*;

public interface VersionedSecretsExtension {
    Image getIcon(String pluginId);

    List<PluginConfiguration> getSecretsConfigMetadata(String pluginId);

    String getSecretsConfigView(String pluginId);

    ValidationResult validateSecretsConfig(String pluginId, final Map<String, String> configuration);

    List<Secret> lookupSecrets(String pluginId, SecretConfig secretConfig, Set<String> lookupStrings);
}
