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

package com.thoughtworks.go.plugin.access;

import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHORIZATION_EXTENSION;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static com.thoughtworks.go.plugin.infra.PluginExtensionsAndVersionValidator.ValidationResult;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PluginExtensionsAndVersionValidatorImplTest {
    private static final String PLUGIN_ID = "Some-Plugin-Id";
    @Mock
    private ExtensionsRegistry extensionsRegistry;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private GoPluginDescriptor descriptor;
    private PluginExtensionsAndVersionValidatorImpl pluginExtensionsAndVersionValidator;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(extensionsRegistry.getPluginManager()).thenReturn(pluginManager);
        when(descriptor.id()).thenReturn(PLUGIN_ID);
        when(extensionsRegistry.allRegisteredExtensions())
                .thenReturn(Stream.of(ELASTIC_AGENT_EXTENSION, AUTHORIZATION_EXTENSION).collect(Collectors.toSet()));

        pluginExtensionsAndVersionValidator = new PluginExtensionsAndVersionValidatorImpl(extensionsRegistry);
    }

    @Test
    void shouldNotAddErrorOnSuccessfulValidation() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.getRequiredExtensionVersionsByPlugin(PLUGIN_ID, ELASTIC_AGENT_EXTENSION)).thenReturn(asList("1.0", "2.0"));
        when(extensionsRegistry.supportsExtensionVersion(PLUGIN_ID, ELASTIC_AGENT_EXTENSION)).thenReturn(true);

        final ValidationResult validationResult = pluginExtensionsAndVersionValidator.validate(descriptor);

        assertThat(validationResult.hasError()).isFalse();
    }

    @Test
    void shouldAddErrorOnSuccessfulValidation() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.getRequiredExtensionVersionsByPlugin(PLUGIN_ID, ELASTIC_AGENT_EXTENSION)).thenReturn(asList("1.0", "2.0"));
        when(extensionsRegistry.supportsExtensionVersion(PLUGIN_ID, ELASTIC_AGENT_EXTENSION)).thenReturn(false);
        when(extensionsRegistry.gocdSupportedExtensionVersions(ELASTIC_AGENT_EXTENSION)).thenReturn(singletonList("3.0"));

        final ValidationResult validationResult = pluginExtensionsAndVersionValidator.validate(descriptor);

        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.toErrorMessage())
                .isEqualTo("Could not find matching extension version between plugin(Some-Plugin-Id) and GoCD:\n" +
                        "  Expected elastic-agent extension version by plugin is [1.0, 2.0]. GoCD Supported versions are [3.0].");
    }
}