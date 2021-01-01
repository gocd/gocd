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
package com.thoughtworks.go.plugin.infra.service;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class DefaultPluginRegistryServiceTest {

    private DefaultPluginRegistryService serviceDefault;
    private PluginRegistry pluginRegistry;

    @BeforeEach
    void setUp() {
        pluginRegistry = mock(PluginRegistry.class);
        serviceDefault = new DefaultPluginRegistryService(pluginRegistry);
    }

    @Test
    void shouldMarkPluginAsInvalidWhenServiceReportsAnError() {
        String bundleSymbolicName = "plugin-id";
        String message = "plugin is broken beyond repair";
        List<String> reasons = List.of(message);
        doNothing().when(pluginRegistry).markPluginInvalid(bundleSymbolicName, reasons);
        serviceDefault.reportErrorAndInvalidate(bundleSymbolicName, reasons);
        verify(pluginRegistry).markPluginInvalid(bundleSymbolicName, reasons);
    }

    @Test
    void shouldNotThrowExceptionWhenPluginIsNotFound() {
        String bundleSymbolicName = "invalid-plugin";
        String message = "some msg";
        List<String> reasons = List.of(message);
        doThrow(new RuntimeException()).when(pluginRegistry).markPluginInvalid(bundleSymbolicName, reasons);

        assertThatCode(() -> serviceDefault.reportErrorAndInvalidate(bundleSymbolicName, reasons))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldGetIDOfFirstPluginInBundle() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("plugin.1").build();
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("plugin.2").build();
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        when(pluginRegistry.getBundleDescriptor(bundleDescriptor.bundleSymbolicName())).thenReturn(bundleDescriptor);

        final String pluginIDOfFirstPluginInBundle = serviceDefault.getPluginIDOfFirstPluginInBundle(bundleDescriptor.bundleSymbolicName());

        assertThat(pluginIDOfFirstPluginInBundle).isEqualTo("plugin.1");
    }

    @Test
    void shouldGetPluginIDForAGivenBundleAndExtensionClass() {
        when(pluginRegistry.pluginIDFor("SYM_1", "com.path.to.MyClass")).thenReturn("plugin_1");

        assertThat(serviceDefault.pluginIDFor("SYM_1", "com.path.to.MyClass")).isEqualTo("plugin_1");
    }
}
