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

import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
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

public class NotificationViewModelBuilderTest {
    @Mock
    NotificationPluginRegistry registry;

    @Mock
    PluginManager manager;

    private NotificationViewModelBuilder builder;
    private GoPluginDescriptor emailNotifier;
    private GoPluginDescriptor slackNotifier;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new NotificationViewModelBuilder(manager, registry);
        emailNotifier = new GoPluginDescriptor("email.notifier", "version1",
                new GoPluginDescriptor.About("Email Notifier", "1.0", null, null, null,null),
                null, null, false);

        slackNotifier = new GoPluginDescriptor("slack.notifier", "version1",
                new GoPluginDescriptor.About("Slack Notifier", "2.0", null, null, null,null),
                null, null, false);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() {
        HashSet<String> pluginIds = new HashSet<>(Arrays.asList("email.notifier", "slack.notifier"));

        when(registry.getNotificationPlugins()).thenReturn(pluginIds);
        when(manager.getPluginDescriptorFor("email.notifier")).thenReturn(emailNotifier);
        when(manager.getPluginDescriptorFor("slack.notifier")).thenReturn(slackNotifier);

        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));
        PluginInfo pluginInfo = pluginInfos.get(0).getId().equals("email.notifier") ? pluginInfos.get(0) : pluginInfos.get(1);
        assertThat(pluginInfo.getId(), is("email.notifier"));
        assertThat(pluginInfo.getType(), is("notification"));
        assertThat(pluginInfo.getName(), is(emailNotifier.about().name()));
        assertThat(pluginInfo.getVersion(), is(emailNotifier.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings());
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenId() {
        HashSet<String> pluginIds = new HashSet<>(Arrays.asList("email.notifier", "slack.notifier"));

        when(registry.getNotificationPlugins()).thenReturn(pluginIds);
        when(manager.getPluginDescriptorFor("email.notifier")).thenReturn(emailNotifier);

        PluginInfo pluginInfo = builder.pluginInfoFor("email.notifier");

        assertThat(pluginInfo.getId(), is("email.notifier"));
        assertThat(pluginInfo.getType(), is("notification"));
        assertThat(pluginInfo.getName(), is(emailNotifier.about().name()));
        assertThat(pluginInfo.getVersion(), is(emailNotifier.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings());
    }

    @Test
    public void shouldBeNullIfPluginNotRegistered() {
        HashSet<String> pluginIds = new HashSet<>(Arrays.asList("email.notifier", "slack.notifier"));

        when(registry.getNotificationPlugins()).thenReturn(pluginIds);

        assertNull(builder.pluginInfoFor("unregistered_plugin"));
    }
}
