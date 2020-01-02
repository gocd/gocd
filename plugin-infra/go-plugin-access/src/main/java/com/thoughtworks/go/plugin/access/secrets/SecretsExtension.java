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

import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.plugin.access.secrets.v1.SecretsExtensionV1;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.secrets.Secret;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.apache.commons.collections4.SetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.SECRETS_EXTENSION;

@Component
public class SecretsExtension extends AbstractExtension {
    public static final List<String> SUPPORTED_VERSIONS = Arrays.asList(SecretsExtensionV1.VERSION);
    private Map<String, VersionedSecretsExtension> secretsExtensionMap = new HashMap<>();

    @Autowired
    public SecretsExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, SECRETS_EXTENSION), SECRETS_EXTENSION);

        secretsExtensionMap.put(SecretsExtensionV1.VERSION, new SecretsExtensionV1(pluginRequestHelper));
    }

    protected SecretsExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry, Map<String, VersionedSecretsExtension> secretsExtensionMap) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, SECRETS_EXTENSION), SECRETS_EXTENSION);

        this.secretsExtensionMap = secretsExtensionMap;
    }

    @Override
    public List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }

    public Image getIcon(String pluginId) {
        return getVersionedSecretsExtension(pluginId).getIcon(pluginId);
    }

    public List<PluginConfiguration> getSecretsConfigMetadata(String pluginId) {
        return getVersionedSecretsExtension(pluginId).getSecretsConfigMetadata(pluginId);
    }

    public String getSecretsConfigView(String pluginId) {
        return getVersionedSecretsExtension(pluginId).getSecretsConfigView(pluginId);
    }

    public ValidationResult validateSecretsConfig(final String pluginId, final Map<String, String> configuration) {
        return getVersionedSecretsExtension(pluginId).validateSecretsConfig(pluginId, configuration);
    }

    public List<Secret> lookupSecrets(String pluginId, SecretConfig secretConfig, Set<String> keys) {
        final List<Secret> secrets = getVersionedSecretsExtension(pluginId).lookupSecrets(pluginId, secretConfig, keys);
        final Set<String> resolvedSecrets = secrets.stream().map(Secret::getKey).collect(Collectors.toSet());

        final Set<String> additionalSecretsInResponse = SetUtils.difference(resolvedSecrets, keys).toSet();
        if (!additionalSecretsInResponse.isEmpty()) {
            throw SecretResolutionFailureException.withUnwantedSecretParams(secretConfig.getId(), keys, additionalSecretsInResponse);
        }

        if (resolvedSecrets.containsAll(keys)) {
            return secrets;
        }

        final Set<String> missingSecrets = SetUtils.disjunction(resolvedSecrets, keys).toSet();
        throw SecretResolutionFailureException.withMissingSecretParams(secretConfig.getId(), keys, missingSecrets);
    }

    protected VersionedSecretsExtension getVersionedSecretsExtension(String pluginId) {
        final String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, SECRETS_EXTENSION, goSupportedVersions());
        return secretsExtensionMap.get(resolvedExtensionVersion);
    }
}
