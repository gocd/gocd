/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.notification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class NotificationPluginRegistryTest {
    public static final String PLUGIN_ID_1 = "plugin-id-1";
    public static final String PLUGIN_ID_2 = "plugin-id-2";
    public static final String PLUGIN_ID_3 = "plugin-id-3";
    public static final String PLUGIN_ID_4 = "plugin-id-4";

    public static final String PIPELINE_STATUS = "pipeline-status";
    public static final String STAGE_STATUS = "stage-status";
    public static final String JOB_STATUS = "job-status";
    public static final String UNKNOWN_NOTIFICATION = "unknown-notification";

    private NotificationPluginRegistry notificationPluginRegistry;

    @Before
    public void setUp() {
        notificationPluginRegistry = new NotificationPluginRegistry();

        notificationPluginRegistry.registerPluginInterests(PLUGIN_ID_1, asList(PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS));
        notificationPluginRegistry.registerPluginInterests(PLUGIN_ID_2, asList(PIPELINE_STATUS));
        notificationPluginRegistry.registerPluginInterests(PLUGIN_ID_3, asList(STAGE_STATUS));
    }

    @After
    public void tearDown() {
        notificationPluginRegistry.clear();
    }

    @Test
    public void should_getPluginsInterestedIn_Correctly() {
        assertThat(notificationPluginRegistry.getPluginsInterestedIn(PIPELINE_STATUS), containsInAnyOrder(PLUGIN_ID_1, PLUGIN_ID_2));
        assertThat(notificationPluginRegistry.getPluginsInterestedIn(STAGE_STATUS), containsInAnyOrder(PLUGIN_ID_1, PLUGIN_ID_3));
        assertThat(notificationPluginRegistry.getPluginsInterestedIn(JOB_STATUS), containsInAnyOrder(PLUGIN_ID_1));
        assertThat(notificationPluginRegistry.getPluginsInterestedIn(UNKNOWN_NOTIFICATION), containsInAnyOrder());
    }

    @Test
    public void should_getPluginInterests_Correctly() {
        assertThat(notificationPluginRegistry.getPluginInterests(PLUGIN_ID_1), containsInAnyOrder(PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS));
        assertThat(notificationPluginRegistry.getPluginInterests(PLUGIN_ID_2), containsInAnyOrder(PIPELINE_STATUS));
        assertThat(notificationPluginRegistry.getPluginInterests(PLUGIN_ID_3), containsInAnyOrder(STAGE_STATUS));
        assertThat(notificationPluginRegistry.getPluginInterests(PLUGIN_ID_4), containsInAnyOrder());
    }

    @Test
    public void should_removePluginInterests_Correctly() {
        notificationPluginRegistry.removePluginInterests(PLUGIN_ID_1);

        assertThat(notificationPluginRegistry.getPluginsInterestedIn(PIPELINE_STATUS), containsInAnyOrder(PLUGIN_ID_2));
        assertThat(notificationPluginRegistry.getPluginsInterestedIn(STAGE_STATUS), containsInAnyOrder(PLUGIN_ID_3));
        assertThat(notificationPluginRegistry.getPluginsInterestedIn(JOB_STATUS), containsInAnyOrder());
        assertThat(notificationPluginRegistry.getPluginsInterestedIn(UNKNOWN_NOTIFICATION), containsInAnyOrder());
    }

    @Test
    public void should_isAnyPluginInterestedIn_Correctly() {
        assertThat(notificationPluginRegistry.isAnyPluginInterestedIn(PIPELINE_STATUS), is(true));
        assertThat(notificationPluginRegistry.isAnyPluginInterestedIn(UNKNOWN_NOTIFICATION), is(false));
    }

    @Test
    public void shouldListRegisteredPlugins() {
        notificationPluginRegistry.registerPlugin("plugin_id_1");
        notificationPluginRegistry.registerPlugin("plugin_id_2");

        assertThat(notificationPluginRegistry.getNotificationPlugins().size(), is(2));
        assertTrue(notificationPluginRegistry.getNotificationPlugins().contains("plugin_id_1"));
        assertTrue(notificationPluginRegistry.getNotificationPlugins().contains("plugin_id_2"));
    }

    @Test
    public void shouldNotRegisterDuplicatePlugins() {
        notificationPluginRegistry.registerPlugin("plugin_id_1");
        notificationPluginRegistry.registerPlugin("plugin_id_1");

        assertThat(notificationPluginRegistry.getNotificationPlugins().size(), is(1));
        assertTrue(notificationPluginRegistry.getNotificationPlugins().contains("plugin_id_1"));
    }

    @Test
    public void shouldNotListDeRegisteredPlugins() {
        notificationPluginRegistry.registerPlugin("plugin_id_1");
        notificationPluginRegistry.deregisterPlugin("plugin_id_1");

        assertTrue(notificationPluginRegistry.getNotificationPlugins().isEmpty());
    }
}
