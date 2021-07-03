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
package com.thoughtworks.go.plugin.domain.common;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo;
import com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.*;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class CombinedPluginInfoTest {
    @Test
    public void shouldGetExtensionNamesOfAllExtensionsContainedWithin() throws Exception {
        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(asList(
                new PluggableTaskPluginInfo(null, null, null),
                new NotificationPluginInfo(null, null)));

        assertThat(pluginInfo.extensionNames().size(), is(2));
        assertThat(pluginInfo.extensionNames(), hasItems(NOTIFICATION_EXTENSION, PLUGGABLE_TASK_EXTENSION));
    }

    @Test
    public void shouldGetDescriptorOfPluginUsingAnyPluginInfo() throws Exception {
        PluginDescriptor descriptor = mock(PluginDescriptor.class);

        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(descriptor, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(descriptor, null, null);

        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(asList(pluggableTaskPluginInfo, notificationPluginInfo));

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
    }

    @Test
    public void shouldFailWhenThereIsNoPluginInfoToGetTheDescriptorFrom() throws Exception {
        CombinedPluginInfo pluginInfo = new CombinedPluginInfo();

        try {
            pluginInfo.getDescriptor();
            fail("Should have failed since there are no plugins found.");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Cannot get descriptor"));
        }
    }

    @Test
    public void shouldGetAllIndividualExtensionInfos() throws Exception {
        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(null, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(null, null, null);

        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(asList(pluggableTaskPluginInfo, notificationPluginInfo));

        assertThat(pluginInfo.getExtensionInfos(), containsInAnyOrder(notificationPluginInfo, pluggableTaskPluginInfo));
    }

    @Test
    public void shouldFindFirstExtensionWithImageIfPluginImplementsAtleastOneExtensionWithImage() throws Exception {
        Image image1 = new Image("c1", "d1", "hash1");
        Image image2 = new Image("c2", "d2", "hash2");
        Image image3 = new Image("c3", "d3", "hash3");

        ElasticAgentPluginInfo elasticAgentPluginInfo = new ElasticAgentPluginInfo(null, null, null, image1, null, null);
        AuthorizationPluginInfo authorizationPluginInfo = new AuthorizationPluginInfo(null, null, null, image2, null);
        AnalyticsPluginInfo analyticsPluginInfo = new AnalyticsPluginInfo(null, image3, null, null);

        assertThat(new CombinedPluginInfo(elasticAgentPluginInfo).getImage(), is(image1));
        assertThat(new CombinedPluginInfo(authorizationPluginInfo).getImage(), is(image2));
        assertThat(new CombinedPluginInfo(analyticsPluginInfo).getImage(), is(image3));

        assertThat(new CombinedPluginInfo(asList(elasticAgentPluginInfo, authorizationPluginInfo)).getImage(), anyOf(is(image1), is(image2)));
        assertThat(new CombinedPluginInfo(asList(analyticsPluginInfo, authorizationPluginInfo)).getImage(), anyOf(is(image2), is(image3)));
    }

    @Test
    public void shouldNotFindImageIfPluginDoesNotImplementAnExtensionWhichHasImages() throws Exception {
        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(null, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(null, null, null);

        assertThat(new CombinedPluginInfo(notificationPluginInfo).getImage(), is(nullValue()));
        assertThat(new CombinedPluginInfo(pluggableTaskPluginInfo).getImage(), is(nullValue()));
        assertThat(new CombinedPluginInfo(asList(pluggableTaskPluginInfo, notificationPluginInfo)).getImage(), is(nullValue()));
    }

    @Test
    public void shouldFindAnExtensionOfAGivenTypeIfItExists() throws Exception {
        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(null, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(null, null, null);

        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(asList(pluggableTaskPluginInfo, notificationPluginInfo));

        assertThat(pluginInfo.extensionFor(NOTIFICATION_EXTENSION), is(notificationPluginInfo));
        assertThat(pluginInfo.extensionFor(PLUGGABLE_TASK_EXTENSION), is(pluggableTaskPluginInfo));
        assertThat(pluginInfo.extensionFor(ANALYTICS_EXTENSION), is(nullValue()));
    }
}
