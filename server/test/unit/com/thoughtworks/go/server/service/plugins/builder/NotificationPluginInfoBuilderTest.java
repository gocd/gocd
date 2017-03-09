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

import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.NotificationPluginInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationPluginInfoBuilderTest {
    @Test
    public void pluginInfoFor_ShouldProvidePluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        PluginManager pluginManager = mock(PluginManager.class);
        NotificationPluginRegistry registry = mock(NotificationPluginRegistry.class);
        when(registry.getNotificationPlugins()).thenReturn(Collections.singleton(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

        NotificationPluginInfoBuilder builder = new NotificationPluginInfoBuilder(pluginManager, registry);
        NotificationPluginInfo pluginInfo = builder.pluginInfoFor(plugin.id());

        assertEquals(new NotificationPluginInfo(plugin), pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        NotificationPluginRegistry registry = mock(NotificationPluginRegistry.class);

        NotificationPluginInfoBuilder builder = new NotificationPluginInfoBuilder(pluginManager, registry);
        NotificationPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);


        PluginManager pluginManager = mock(PluginManager.class);
        NotificationPluginRegistry registry = mock(NotificationPluginRegistry.class);
        when(registry.getNotificationPlugins()).thenReturn(Collections.singleton(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

        NotificationPluginInfoBuilder builder = new NotificationPluginInfoBuilder(pluginManager, registry);
        Collection<NotificationPluginInfo> pluginInfos = builder.allPluginInfos();

        assertEquals(Arrays.asList(new NotificationPluginInfo(plugin)), pluginInfos);
    }


}