/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.AuthenticationPluginInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationPluginInfoBuilderTest {
    @Test
    public void pluginInfoFor_ShouldProvidePluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        PluginManager pluginManager = mock(PluginManager.class);
        AuthenticationPluginRegistry registry = mock(AuthenticationPluginRegistry.class);
        when(registry.getAuthenticationPlugins()).thenReturn(Collections.singleton(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);
        
        AuthenticationPluginInfoBuilder builder = new AuthenticationPluginInfoBuilder(pluginManager, registry);
        AuthenticationPluginInfo pluginInfo = builder.pluginInfoFor(plugin.id());

        assertEquals(new AuthenticationPluginInfo(plugin), pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        AuthenticationPluginRegistry registry = mock(AuthenticationPluginRegistry.class);

        AuthenticationPluginInfoBuilder builder = new AuthenticationPluginInfoBuilder(pluginManager, registry);
        AuthenticationPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);


        PluginManager pluginManager = mock(PluginManager.class);
        AuthenticationPluginRegistry registry = mock(AuthenticationPluginRegistry.class);
        when(registry.getAuthenticationPlugins()).thenReturn(Collections.singleton(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

        AuthenticationPluginInfoBuilder builder = new AuthenticationPluginInfoBuilder(pluginManager, registry);
        Collection<AuthenticationPluginInfo> pluginInfos = builder.allPluginInfos();

        assertEquals(Arrays.asList(new AuthenticationPluginInfo(plugin)), pluginInfos);
    }

}