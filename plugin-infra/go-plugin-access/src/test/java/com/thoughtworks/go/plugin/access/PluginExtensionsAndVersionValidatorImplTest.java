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
package com.thoughtworks.go.plugin.access;

import com.thoughtworks.go.plugin.infra.PluginPostLoadHook;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHORIZATION_EXTENSION;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginExtensionsAndVersionValidatorImplTest {
    private static final String PLUGIN_ID = "Some-Plugin-Id";
    @Mock(lenient = true)
    private ExtensionsRegistry extensionsRegistry;
    @Mock(lenient = true)
    private GoPluginDescriptor descriptor;
    private PluginExtensionsAndVersionValidatorImpl pluginExtensionsAndVersionValidator;

    @BeforeEach
    void setUp() {
        when(descriptor.id()).thenReturn(PLUGIN_ID);
        when(extensionsRegistry.allRegisteredExtensions())
                .thenReturn(Stream.of(ELASTIC_AGENT_EXTENSION, AUTHORIZATION_EXTENSION).collect(Collectors.toSet()));
        when(extensionsRegistry.gocdSupportedExtensionVersions(ELASTIC_AGENT_EXTENSION))
                .thenReturn(Arrays.asList("1.0", "2.0"));
        when(extensionsRegistry.gocdSupportedExtensionVersions(AUTHORIZATION_EXTENSION))
                .thenReturn(singletonList("2.0"));

        pluginExtensionsAndVersionValidator = new PluginExtensionsAndVersionValidatorImpl(extensionsRegistry);
    }

    @Test
    void shouldNotAddErrorOnSuccessfulValidation() {
        final PluginPostLoadHook.Result validationResult = pluginExtensionsAndVersionValidator.run(descriptor, Collections.singletonMap(ELASTIC_AGENT_EXTENSION, singletonList("2.0")));

        assertThat(validationResult.isAFailure()).isFalse();
        assertThat(validationResult.getMessage()).isNull();
    }

    @Test
    void shouldAddErrorAndReturnValidationResultWhenPluginRequiredExtensionIsNotSupportedByGoCD() {
        final PluginPostLoadHook.Result validationResult = pluginExtensionsAndVersionValidator.run(descriptor, Collections.singletonMap("some-invalid-extension", singletonList("2.0")));

        assertThat(validationResult.isAFailure()).isTrue();
        assertThat(validationResult.getMessage()).isEqualTo("Extension incompatibility detected between plugin(Some-Plugin-Id) and GoCD:\n" +
                "  Extension(s) [some-invalid-extension] used by the plugin is not supported. GoCD Supported extensions are [authorization, elastic-agent].");
    }

    @Test
    void shouldAddErrorAndReturnValidationResultWhenPluginRequiredExtensionVersionIsNotSupportedByGoCD() {
        final PluginPostLoadHook.Result validationResult = pluginExtensionsAndVersionValidator.run(descriptor, Collections.singletonMap(ELASTIC_AGENT_EXTENSION, singletonList("3.0")));

        assertThat(validationResult.isAFailure()).isTrue();
        assertThat(validationResult.getMessage())
                .isEqualTo("Extension incompatibility detected between plugin(Some-Plugin-Id) and GoCD:\n" +
                        "  Expected elastic-agent extension version(s) [3.0] by plugin is unsupported. GoCD Supported versions are [1.0, 2.0].");
    }

    @Test
    void shouldConsiderPluginValidWhenOneOfTheExtensionVersionUsedByThePluginIsSupportedByGoCD() {
        final PluginPostLoadHook.Result validationResult = pluginExtensionsAndVersionValidator.run(descriptor, Collections.singletonMap(ELASTIC_AGENT_EXTENSION, Arrays.asList("a.b", "2.0")));

        assertThat(validationResult.isAFailure()).isFalse();
    }
}