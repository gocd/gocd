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
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.JsonBasedPluggableTask;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMPreference;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginInfoBuilderTest {
    @Mock
    AuthenticationPluginRegistry authenticationPluginRegistry;

    @Mock
    NotificationPluginRegistry notificationPluginRegistry;

    @Mock
    ElasticAgentPluginRegistry elasticPluginRegistry;

    @Mock
    PluginManager manager;

    private GoPluginDescriptor githubDescriptor;
    private GoPluginDescriptor emailNotifier;
    private GoPluginDescriptor yumPoller;
    private GoPluginDescriptor xunitConvertor;
    private GoPluginDescriptor githubPR;
    private GoPluginDescriptor dockerElasticAgentPlugin;
    private PluginInfoBuilder pluginViewModelBuilder;

    @Before
    public void setUp() {
        initMocks(this);
        githubDescriptor = new GoPluginDescriptor("github.oauth", "version1",
                new GoPluginDescriptor.About("Github OAuth Plugin", "1.0", null, null, null, null),
                null, null, false);
        emailNotifier = new GoPluginDescriptor("email.notifier", "version1",
                new GoPluginDescriptor.About("Email Notifier", "1.0", null, null, null, null),
                null, null, false);
        yumPoller = new GoPluginDescriptor("yum.poller", "version1",
                new GoPluginDescriptor.About("Yum Poller", "1.0", null, null, null, null),
                null, null, false);
        xunitConvertor = new GoPluginDescriptor("xunit.convertor", "version1",
                new GoPluginDescriptor.About("Xunit Convertor", "1.0", null, null, null, null),
                null, null, false);
        githubPR = new GoPluginDescriptor("github.pr", "version1",
                new GoPluginDescriptor.About("Github PR", "1.0", null, null, null, null),
                null, null, false);

        dockerElasticAgentPlugin = new GoPluginDescriptor("cd.go.elastic-agent.docker", "1.0",
                new GoPluginDescriptor.About("GoCD Docker Elastic Agent Plugin", "1.0", null, null, null, null),
                null, null, false);

        JsonBasedPluggableTask jsonBasedPluggableTask = mock(JsonBasedPluggableTask.class);
        TaskView taskView = new TaskView() {
            @Override
            public String displayValue() {
                return "task display value";
            }

            @Override
            public String template() {
                return "pluggable task view template";
            }
        };


        when(authenticationPluginRegistry.getAuthenticationPlugins()).thenReturn(new HashSet<>(Arrays.asList("github.oauth")));
        when(notificationPluginRegistry.getNotificationPlugins()).thenReturn(new HashSet<>(Arrays.asList("email.notifier")));
        when(elasticPluginRegistry.getPlugins()).thenReturn(new ArrayList<PluginDescriptor>(Arrays.asList(dockerElasticAgentPlugin)));
        when(jsonBasedPluggableTask.view()).thenReturn(taskView);

        when(manager.getPluginDescriptorFor("github.oauth")).thenReturn(githubDescriptor);
        when(manager.getPluginDescriptorFor("email.notifier")).thenReturn(emailNotifier);
        when(manager.getPluginDescriptorFor("yum.poller")).thenReturn(yumPoller);
        when(manager.getPluginDescriptorFor("xunit.convertor")).thenReturn(xunitConvertor);
        when(manager.getPluginDescriptorFor("github.pr")).thenReturn(githubPR);
        when(manager.getPluginDescriptorFor("cd.go.elastic-agent.docker")).thenReturn(dockerElasticAgentPlugin);

        PackageMetadataStore.getInstance().addMetadataFor(yumPoller.id(), new PackageConfigurations());
        PluggableTaskConfigStore.store().setPreferenceFor("xunit.convertor", new TaskPreference(jsonBasedPluggableTask));
        SCMMetadataStore.getInstance().setPreferenceFor("github.pr", new SCMPreference(new SCMConfigurations(), mock(SCMView.class)));
        pluginViewModelBuilder = new PluginInfoBuilder(authenticationPluginRegistry, notificationPluginRegistry, elasticPluginRegistry, manager);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() {
        List<PluginInfo> pluginInfos = pluginViewModelBuilder.allPluginInfos(null);

        assertThat(pluginInfos.size(), is(6));
        List<String> expectedPlugins = new ArrayList<>();
        for (PluginInfo pluginInfo : pluginInfos) {
            expectedPlugins.add(pluginInfo.getId());
        }
        assertTrue(expectedPlugins.contains(githubDescriptor.id()));
        assertTrue(expectedPlugins.contains(emailNotifier.id()));
        assertTrue(expectedPlugins.contains(yumPoller.id()));
        assertTrue(expectedPlugins.contains(xunitConvertor.id()));
        assertTrue(expectedPlugins.contains(githubPR.id()));
        assertTrue(expectedPlugins.contains(dockerElasticAgentPlugin.id()));
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfosByType() {
        List<PluginInfo> pluginInfos = pluginViewModelBuilder.allPluginInfos("scm");

        assertThat(pluginInfos.size(), is(1));
        assertThat(pluginInfos.get(0).getId(), is(githubPR.id()));
    }

    @Test(expected = InvalidPluginTypeException.class)
    public void shouldErrorOutForInvalidPluginType() {
        assertThat(pluginViewModelBuilder.allPluginInfos("invalid_type").size(), is(0));
    }

    @Test
    public void shouldBeAbleToAPluginInfoById() {
        PluginInfo pluginInfo = pluginViewModelBuilder.pluginInfoFor("email.notifier");

        assertThat(pluginInfo.getId(), is(emailNotifier.id()));
    }

    @Test
    public void shouldReturnNullInAbsenceOfPluginForAGivenId() {
        assertNull(pluginViewModelBuilder.pluginInfoFor("plugin_not_present"));
    }
}
