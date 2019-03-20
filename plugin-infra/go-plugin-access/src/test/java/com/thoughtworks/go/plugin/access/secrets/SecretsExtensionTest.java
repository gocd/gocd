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

package com.thoughtworks.go.plugin.access.secrets;

import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.secrets.v1.SecretsExtensionV1;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.plugin.access.secrets.SecretsExtension.SUPPORTED_VERSIONS;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.SECRETS_EXTENSION;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SecretsExtensionTest {
    private static final String PLUGIN_ID = "cd.go.example.plugin";
    protected PluginManager pluginManager;
    private ExtensionsRegistry extensionsRegistry;
    protected GoPluginDescriptor descriptor;
    protected SecretsExtension extension;

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        extensionsRegistry = mock(ExtensionsRegistry.class);
        descriptor = mock(GoPluginDescriptor.class);
        extension = new SecretsExtension(pluginManager, extensionsRegistry);

        when(descriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginManager.getPluginDescriptorFor(PLUGIN_ID)).thenReturn(descriptor);
        when(pluginManager.isPluginOfType(SECRETS_EXTENSION, PLUGIN_ID)).thenReturn(true);
    }

    @Test
    public void shouldHaveVersionedSecretsExtensionForAllSupportedVersions() {
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            final String message = String.format("Must define versioned extension class for %s extension with version %s", SECRETS_EXTENSION, supportedVersion);

            when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SECRETS_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(supportedVersion);

            final VersionedSecretsExtension extension = this.extension.getVersionedSecretsExtension(PLUGIN_ID);

            assertNotNull(message, extension);
            assertThat(ReflectionUtil.getField(extension, "VERSION"), is(supportedVersion));
        }
    }

    @Test
    public void getIcon_shouldDelegateToVersionedExtension() {
        SecretsExtensionV1 secretsExtensionV1 = mock(SecretsExtensionV1.class);
        Map<String, VersionedSecretsExtension> secretsExtensionMap = singletonMap("1.0", secretsExtensionV1);
        extension = new SecretsExtension(pluginManager, extensionsRegistry, secretsExtensionMap);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SECRETS_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(SecretsExtensionV1.VERSION);

        this.extension.getIcon(PLUGIN_ID);

        verify(secretsExtensionV1).getIcon(PLUGIN_ID);
    }

    @Test
    public void getSecretsConfigMetadata_shouldDelegateToVersionedExtension() {
        SecretsExtensionV1 secretsExtensionV1 = mock(SecretsExtensionV1.class);
        Map<String, VersionedSecretsExtension> secretsExtensionMap = singletonMap("1.0", secretsExtensionV1);
        extension = new SecretsExtension(pluginManager, extensionsRegistry, secretsExtensionMap);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SECRETS_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(SecretsExtensionV1.VERSION);

        this.extension.getSecretsConfigMetadata(PLUGIN_ID);

        verify(secretsExtensionV1).getSecretsConfigMetadata(PLUGIN_ID);
    }

    @Test
    public void getSecretsConfigView_shouldDelegateToVersionedExtension() {
        SecretsExtensionV1 secretsExtensionV1 = mock(SecretsExtensionV1.class);
        Map<String, VersionedSecretsExtension> secretsExtensionMap = singletonMap("1.0", secretsExtensionV1);
        extension = new SecretsExtension(pluginManager, extensionsRegistry, secretsExtensionMap);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SECRETS_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(SecretsExtensionV1.VERSION);

        this.extension.getSecretsConfigView(PLUGIN_ID);

        verify(secretsExtensionV1).getSecretsConfigView(PLUGIN_ID);
    }

    @Test
    public void validateSecretsConfig_shouldDelegateToVersionedExtension() {
        SecretsExtensionV1 secretsExtensionV1 = mock(SecretsExtensionV1.class);
        Map<String, VersionedSecretsExtension> secretsExtensionMap = singletonMap("1.0", secretsExtensionV1);
        extension = new SecretsExtension(pluginManager, extensionsRegistry, secretsExtensionMap);
        Map<String, String> configuration = singletonMap("key", "val");

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SECRETS_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(SecretsExtensionV1.VERSION);

        this.extension.validateSecretsConfig(PLUGIN_ID, configuration);

        verify(secretsExtensionV1).validateSecretsConfig(PLUGIN_ID, configuration);
    }

    @Test
    public void lookupSecrets_shouldDelegateToVersionedExtension() {
        SecretsExtensionV1 secretsExtensionV1 = mock(SecretsExtensionV1.class);
        Map<String, VersionedSecretsExtension> secretsExtensionMap = singletonMap("1.0", secretsExtensionV1);
        extension = new SecretsExtension(pluginManager, extensionsRegistry, secretsExtensionMap);
        Set<String> keys = new HashSet<>(asList("key1", "key2"));

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SECRETS_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(SecretsExtensionV1.VERSION);

        this.extension.lookupSecrets(PLUGIN_ID, new SecretConfig(), keys);

        verify(secretsExtensionV1).lookupSecrets(PLUGIN_ID, new SecretConfig(), keys);
    }

    @Test
    public void shouldExtendAbstractExtension() {
        assertTrue(new SecretsExtension(pluginManager, extensionsRegistry) instanceof AbstractExtension);
    }
}
