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

import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ExtensionsRegistryTest {
    @Mock
    private PluginManager pluginManager;
    @Mock
    private GoPluginExtension elasticExtension;
    @Mock
    private GoPluginExtension authorizationExtension;
    private ExtensionsRegistry registry;

    @BeforeEach
    void setUp() {
        initMocks(this);
        registry = new ExtensionsRegistry(pluginManager);

        when(authorizationExtension.extensionName()).thenReturn(AUTHORIZATION_EXTENSION);
        when(authorizationExtension.goSupportedVersions()).thenReturn(Arrays.asList("1.0", "2.0"));

        when(elasticExtension.extensionName()).thenReturn(ELASTIC_AGENT_EXTENSION);
        when(elasticExtension.goSupportedVersions()).thenReturn(Collections.singletonList("2.0"));
    }

    @Test
    void shouldReturnFalseWhenNoExtensionRegisteredForGivenName() {
        assertThat(registry.supportsExtensionVersion("Some-extension", "1.0")).isFalse();
    }

    @Test
    void shouldRegisterExtensionWithSupportedVersions() {
        when(pluginManager.resolveExtensionVersion("docker", ELASTIC_AGENT_EXTENSION, Collections.singletonList("2.0")))
                .thenReturn("2.0");

        registry.registerExtension(elasticExtension);

        assertThat(registry.supportsExtensionVersion("docker", ELASTIC_AGENT_EXTENSION)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRegisteredExtensionDoesNotSupportSpecifiedVersions() {
        when(pluginManager.resolveExtensionVersion("docker", ELASTIC_AGENT_EXTENSION, Collections.singletonList("2.0")))
                .thenThrow(new RuntimeException("Not supported by the plugin."));

        registry.registerExtension(elasticExtension);

        assertThat(registry.supportsExtensionVersion("docker", ELASTIC_AGENT_EXTENSION)).isFalse();
    }

    @Test
    void shouldReturnAllRegisteredExtensionsName() {
        registry.registerExtension(authorizationExtension);
        registry.registerExtension(elasticExtension);

        assertThat(registry.allRegisteredExtensions()).hasSize(2)
                .contains(ELASTIC_AGENT_EXTENSION, AUTHORIZATION_EXTENSION);
    }

    @Test
    void shouldGetGoCDSupportedExtensionVersionFromTheExtension() {
        registry.registerExtension(authorizationExtension);
        registry.registerExtension(elasticExtension);

        assertThat(registry.gocdSupportedExtensionVersions(ELASTIC_AGENT_EXTENSION)).hasSize(1)
                .contains("2.0");
    }

    @Test
    void shouldReturnEmptyListWhenExtensionIsNotSupportedByGoCD() {
        registry.registerExtension(authorizationExtension);
        registry.registerExtension(elasticExtension);

        assertThatThrownBy(() -> registry.gocdSupportedExtensionVersions(SCM_EXTENSION))
                .isInstanceOf(UnsupportedExtensionException.class)
                .hasMessageContaining("Requested extension 'scm' is not supported by GoCD. Supported extensions are [authorization, elastic-agent]");
    }
}