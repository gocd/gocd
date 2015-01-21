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

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.MockitoAnnotations.initMocks;

public class NotificationPluginRegistryTest {
    public static final String PLUGIN_ID_1 = "plugin-id-1";
    public static final String PLUGIN_ID_2 = "plugin-id-2";
    public static final String PLUGIN_ID_3 = "plugin-id-3";
    public static final String PLUGIN_ID_4 = "plugin-id-4";

    public static final String PIPELINE_STATUS = "pipeline-status";
    public static final String STAGE_STATUS = "stage-status";
    public static final String JOB_STATUS = "job-status";
    public static final String UNKNOWN_NOTIFICATION = "unknown-notification";

    @Before
    public void setUp() {
        initMocks(this);

        NotificationPluginRegistry.getInstance().clear();
    }

    @After
    public void tearDown() {
        NotificationPluginRegistry.getInstance().clear();
    }

    @Test
    public void should_registerPluginInterests_getPluginsInterestedIn_getPluginInterests_Correctly() {
        NotificationPluginRegistry.getInstance().registerPluginInterests(PLUGIN_ID_1, Arrays.asList(new String[]{PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS}));
        NotificationPluginRegistry.getInstance().registerPluginInterests(PLUGIN_ID_2, Arrays.asList(new String[]{PIPELINE_STATUS}));
        NotificationPluginRegistry.getInstance().registerPluginInterests(PLUGIN_ID_3, Arrays.asList(new String[]{STAGE_STATUS}));

        assertThat(NotificationPluginRegistry.getInstance().getPluginsInterestedIn(PIPELINE_STATUS), containsInAnyOrder(PLUGIN_ID_1, PLUGIN_ID_2));
        assertThat(NotificationPluginRegistry.getInstance().getPluginsInterestedIn(STAGE_STATUS), containsInAnyOrder(PLUGIN_ID_1, PLUGIN_ID_3));
        assertThat(NotificationPluginRegistry.getInstance().getPluginsInterestedIn(JOB_STATUS), containsInAnyOrder(PLUGIN_ID_1));
        assertThat(NotificationPluginRegistry.getInstance().getPluginsInterestedIn(UNKNOWN_NOTIFICATION), containsInAnyOrder());

        assertThat(NotificationPluginRegistry.getInstance().getPluginInterests(PLUGIN_ID_1), containsInAnyOrder(PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS));
        assertThat(NotificationPluginRegistry.getInstance().getPluginInterests(PLUGIN_ID_2), containsInAnyOrder(PIPELINE_STATUS));
        assertThat(NotificationPluginRegistry.getInstance().getPluginInterests(PLUGIN_ID_3), containsInAnyOrder(STAGE_STATUS));
        assertThat(NotificationPluginRegistry.getInstance().getPluginInterests(PLUGIN_ID_4), containsInAnyOrder());
    }
}