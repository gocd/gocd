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

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import com.thoughtworks.go.server.ui.plugins.SCMPluginInfo;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.thoughtworks.go.server.service.plugins.builder.SCMPluginInfoBuilder.configurations;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SCMPluginInfoBuilderTest {

    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Test
    public void pluginInfoFor_ShouldProvidePluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        PluginManager pluginManager = mock(PluginManager.class);
        SCMMetadataStore scmMetadataStore = mock(SCMMetadataStore.class);

        when(scmMetadataStore.getPlugins()).thenReturn(Collections.singletonList(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

        SCMConfigurations configurations = new SCMConfigurations();
        configurations.add(new SCMConfiguration("key1"));
        configurations.add(new SCMConfiguration("key2"));

        SCMView view = new SCMView() {
            @Override
            public String displayValue() {
                return "SCM Display Value";
            }

            @Override
            public String template() {
                return "scm view template";
            }
        };

        SCMPreference scmPreference = new SCMPreference(configurations, view);

        when(scmMetadataStore.preferenceFor(plugin.id())).thenReturn(scmPreference);

        SCMPluginInfoBuilder builder = new SCMPluginInfoBuilder(pluginManager, scmMetadataStore);
        SCMPluginInfo pluginInfo = builder.pluginInfoFor(plugin.id());

        SCMPluginInfo expectedPluginInfo = new SCMPluginInfo(plugin, view.displayValue(), new PluggableInstanceSettings(configurations(configurations), new PluginView(view.template())));
        assertEquals(expectedPluginInfo, pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        SCMMetadataStore scmMetadataStore = mock(SCMMetadataStore.class);

        SCMPluginInfoBuilder builder = new SCMPluginInfoBuilder(pluginManager, scmMetadataStore);
        SCMPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);


        PluginManager pluginManager = mock(PluginManager.class);
        SCMMetadataStore scmMetadataStore = mock(SCMMetadataStore.class);

        when(scmMetadataStore.getPlugins()).thenReturn(Collections.singletonList(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

        SCMConfigurations configurations = new SCMConfigurations();
        configurations.add(new SCMConfiguration("key1"));
        configurations.add(new SCMConfiguration("key2"));

        SCMView view = new SCMView() {
            @Override
            public String displayValue() {
                return "SCM Display Value";
            }

            @Override
            public String template() {
                return "scm view template";
            }
        };

        SCMPreference scmPreference = new SCMPreference(configurations, view);

        when(scmMetadataStore.preferenceFor(plugin.id())).thenReturn(scmPreference);

        SCMPluginInfoBuilder builder = new SCMPluginInfoBuilder(pluginManager, scmMetadataStore);
        Collection<SCMPluginInfo> pluginInfos = builder.allPluginInfos();

        SCMPluginInfo expectedPluginInfo = new SCMPluginInfo(plugin, view.displayValue(), new PluggableInstanceSettings(configurations(configurations), new PluginView(view.template())));
        assertEquals(Arrays.asList(expectedPluginInfo), pluginInfos);
        assertEquals(Arrays.asList(expectedPluginInfo), pluginInfos);
    }

}