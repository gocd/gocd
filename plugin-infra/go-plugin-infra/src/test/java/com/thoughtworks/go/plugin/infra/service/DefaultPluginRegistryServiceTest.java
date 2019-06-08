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
package com.thoughtworks.go.plugin.infra.service;

import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class DefaultPluginRegistryServiceTest {

    private DefaultPluginRegistryService serviceDefault;
    private PluginRegistry pluginRegistry;

    @Before
    public void setUp() {
        pluginRegistry = mock(PluginRegistry.class);
        serviceDefault = new DefaultPluginRegistryService(pluginRegistry);
    }

    @Test
    public void shouldMarkPluginAsInvalidWhenServiceReportsAnError() {
        String bundleSymbolicName = "plugin-id";
        String message = "plugin is broken beyond repair";
        List<String> reasons = asList(message);
        doNothing().when(pluginRegistry).markPluginInvalid(bundleSymbolicName, reasons);
        serviceDefault.reportErrorAndInvalidate(bundleSymbolicName, reasons);
        verify(pluginRegistry).markPluginInvalid(bundleSymbolicName, reasons);
    }

    @Test
    public void shouldNotThrowExceptionWhenPluginIsNotFound() {
        String bundleSymbolicName = "invalid-plugin";
        String message = "some msg";
        List<String> reasons = asList(message);
        doThrow(new RuntimeException()).when(pluginRegistry).markPluginInvalid(bundleSymbolicName, reasons);
        try {
            serviceDefault.reportErrorAndInvalidate(bundleSymbolicName, reasons);
        } catch(Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    public void shouldGetIDOfFirstPluginInBundle() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        when(pluginRegistry.getBundleDescriptor(bundleDescriptor.bundleSymbolicName())).thenReturn(bundleDescriptor);

        final String pluginIDOfFirstPluginInBundle = serviceDefault.getPluginIDOfFirstPluginInBundle(bundleDescriptor.bundleSymbolicName());

        assertThat(pluginIDOfFirstPluginInBundle, is("plugin.1"));
    }

    @Test
    public void shouldGetPluginIDForAGivenBundleAndExtensionClass() {
        when(pluginRegistry.pluginIDFor("SYM_1", "com.path.to.MyClass")).thenReturn("plugin_1");

        assertThat(serviceDefault.pluginIDFor("SYM_1", "com.path.to.MyClass"), is("plugin_1"));
    }
}
