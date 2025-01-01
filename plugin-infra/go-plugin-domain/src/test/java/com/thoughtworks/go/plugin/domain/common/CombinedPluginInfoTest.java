/*
 * Copyright Thoughtworks, Inc.
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

import java.util.List;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class CombinedPluginInfoTest {
    @Test
    public void shouldGetExtensionNamesOfAllExtensionsContainedWithin() {
        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(List.of(
                new PluggableTaskPluginInfo(null, null, null),
                new NotificationPluginInfo(null, null)));

        assertThat(pluginInfo.extensionNames()).containsExactlyInAnyOrder(PLUGGABLE_TASK_EXTENSION, NOTIFICATION_EXTENSION);
    }

    @Test
    public void shouldGetDescriptorOfPluginUsingAnyPluginInfo() {
        PluginDescriptor descriptor = mock(PluginDescriptor.class);

        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(descriptor, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(descriptor, null, null);

        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(List.of(pluggableTaskPluginInfo, notificationPluginInfo));

        assertThat(pluginInfo.getDescriptor()).isEqualTo(descriptor);
    }

    @Test
    public void shouldFailWhenThereIsNoPluginInfoToGetTheDescriptorFrom() {
        CombinedPluginInfo pluginInfo = new CombinedPluginInfo();

        try {
            pluginInfo.getDescriptor();
            fail("Should have failed since there are no plugins found.");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Cannot get descriptor");
        }
    }

    @Test
    public void shouldGetAllIndividualExtensionInfos() {
        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(null, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(null, null, null);

        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(List.of(pluggableTaskPluginInfo, notificationPluginInfo));

        assertThat(pluginInfo.getExtensionInfos()).containsExactlyInAnyOrder(notificationPluginInfo, pluggableTaskPluginInfo);
    }

    @Test
    public void shouldFindFirstExtensionWithImageIfPluginImplementsAtLeastOneExtensionWithImage() {
        Image image1 = new Image("c1", "d1", "hash1");
        Image image2 = new Image("c2", "d2", "hash2");
        Image image3 = new Image("c3", "d3", "hash3");

        ElasticAgentPluginInfo elasticAgentPluginInfo = new ElasticAgentPluginInfo(null, null, null, image1, null, null);
        AuthorizationPluginInfo authorizationPluginInfo = new AuthorizationPluginInfo(null, null, null, image2, null);
        AnalyticsPluginInfo analyticsPluginInfo = new AnalyticsPluginInfo(null, image3, null, null);

        assertThat(new CombinedPluginInfo(elasticAgentPluginInfo).getImage()).isEqualTo(image1);
        assertThat(new CombinedPluginInfo(authorizationPluginInfo).getImage()).isEqualTo(image2);
        assertThat(new CombinedPluginInfo(analyticsPluginInfo).getImage()).isEqualTo(image3);

        assertThat(new CombinedPluginInfo(List.of(elasticAgentPluginInfo, authorizationPluginInfo)).getImage()).isIn(image1, image2);
        assertThat(new CombinedPluginInfo(List.of(analyticsPluginInfo, authorizationPluginInfo)).getImage()).isIn(image2,image3);
    }

    @Test
    public void shouldNotFindImageIfPluginDoesNotImplementAnExtensionWhichHasImages() {
        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(null, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(null, null, null);

        assertThat(new CombinedPluginInfo(notificationPluginInfo).getImage()).isNull();
        assertThat(new CombinedPluginInfo(pluggableTaskPluginInfo).getImage()).isNull();
        assertThat(new CombinedPluginInfo(List.of(pluggableTaskPluginInfo, notificationPluginInfo)).getImage()).isNull();
    }

    @Test
    public void shouldFindAnExtensionOfAGivenTypeIfItExists() {
        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(null, null);
        PluggableTaskPluginInfo pluggableTaskPluginInfo = new PluggableTaskPluginInfo(null, null, null);

        CombinedPluginInfo pluginInfo = new CombinedPluginInfo(List.of(pluggableTaskPluginInfo, notificationPluginInfo));

        assertThat(pluginInfo.extensionFor(NOTIFICATION_EXTENSION)).isEqualTo(notificationPluginInfo);
        assertThat(pluginInfo.extensionFor(PLUGGABLE_TASK_EXTENSION)).isEqualTo(pluggableTaskPluginInfo);
        assertThat(pluginInfo.extensionFor(ANALYTICS_EXTENSION)).isNull();
    }
}
