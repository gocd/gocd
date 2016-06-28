/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthenticationViewModelBuilderTest {
    @Mock
    AuthenticationPluginRegistry registry;

    @Mock
    PluginManager manager;

    private AuthenticationViewModelBuilder builder;
    private GoPluginDescriptor githubDescriptor;
    private GoPluginDescriptor googleDescriptor;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new AuthenticationViewModelBuilder(manager, registry);
        githubDescriptor = new GoPluginDescriptor("github.oauth", "version1",
                new GoPluginDescriptor.About("Github OAuth Plugin", "1.0", null, null, null,null),
                null, null, false);

        googleDescriptor = new GoPluginDescriptor("google.oauth", "version1",
                new GoPluginDescriptor.About("auth_plugin", "2.0", null, null, null,null),
                null, null, false);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() {
        HashSet<String> pluginIds = new HashSet<>(Arrays.asList("github.oauth", "google.oauth"));

        when(registry.getAuthenticationPlugins()).thenReturn(pluginIds);
        when(manager.getPluginDescriptorFor("github.oauth")).thenReturn(githubDescriptor);
        when(manager.getPluginDescriptorFor("google.oauth")).thenReturn(googleDescriptor);

        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));
        PluginInfo pluginInfo = pluginInfos.get(0).getId() == "github.oauth" ? pluginInfos.get(0) : pluginInfos.get(1);
        assertThat(pluginInfo.getId(), is("github.oauth"));
        assertThat(pluginInfo.getType(), is("authentication"));
        assertThat(pluginInfo.getName(), is(githubDescriptor.about().name()));
        assertThat(pluginInfo.getVersion(), is(githubDescriptor.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings());
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenId() {
        HashSet<String> pluginIds = new HashSet<>(Arrays.asList("github.oauth", "google.oauth"));

        when(registry.getAuthenticationPlugins()).thenReturn(pluginIds);
        when(manager.getPluginDescriptorFor("github.oauth")).thenReturn(githubDescriptor);

        PluginInfo pluginInfo = builder.pluginInfoFor("github.oauth");

        assertThat(pluginInfo.getId(), is("github.oauth"));
        assertThat(pluginInfo.getType(), is("authentication"));
        assertThat(pluginInfo.getName(), is(githubDescriptor.about().name()));
        assertThat(pluginInfo.getVersion(), is(githubDescriptor.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings());
    }

    @Test
    public void shouldBeNullIfPluginNotRegistered() {
        HashSet<String> pluginIds = new HashSet<>(Arrays.asList("github.oauth", "google.oauth"));
        when(registry.getAuthenticationPlugins()).thenReturn(pluginIds);

        assertNull(builder.pluginInfoFor("unregistered_plugin"));
    }
}
