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

package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class NotificationPluginRegistrarTest {
    public static final String PLUGIN_ID_1 = "plugin-id-1";
    public static final String PLUGIN_ID_2 = "plugin-id-2";
    public static final String PLUGIN_ID_3 = "plugin-id-3";
    public static final String PLUGIN_ID_4 = "plugin-id-4";

    public static final String PIPELINE_STATUS = "pipeline-status";
    public static final String STAGE_STATUS = "stage-status";
    public static final String JOB_STATUS = "job-status";

    @Mock
    private PluginManager pluginManager;
    @Mock
    private NotificationExtension notificationExtension;
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;

    @Before
    public void setUp() {
        initMocks(this);

        when(notificationExtension.canHandlePlugin(PLUGIN_ID_1)).thenReturn(true);
        when(notificationExtension.canHandlePlugin(PLUGIN_ID_2)).thenReturn(true);
        when(notificationExtension.canHandlePlugin(PLUGIN_ID_3)).thenReturn(true);
        when(notificationExtension.canHandlePlugin(PLUGIN_ID_4)).thenReturn(false);

        when(notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID_1)).thenReturn(asList(PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS));
        when(notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID_2)).thenReturn(asList(PIPELINE_STATUS));
        when(notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID_3)).thenReturn(asList(STAGE_STATUS));
    }

    @Test
    public void shouldRegisterPluginInterestsOnPluginLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginLoaded(new GoPluginDescriptor(PLUGIN_ID_1, null, null, null, null, true));
        verify(notificationPluginRegistry).registerPluginInterests(PLUGIN_ID_1, asList(PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS));

        notificationPluginRegistrar.pluginLoaded(new GoPluginDescriptor(PLUGIN_ID_2, null, null, null, null, true));
        verify(notificationPluginRegistry).registerPluginInterests(PLUGIN_ID_2, asList(PIPELINE_STATUS));

        notificationPluginRegistrar.pluginLoaded(new GoPluginDescriptor(PLUGIN_ID_3, null, null, null, null, true));
        verify(notificationPluginRegistry).registerPluginInterests(PLUGIN_ID_3, asList(STAGE_STATUS));
    }

    @Test
    public void shouldNotRegisterPluginInterestsOnPluginLoadIfPluginIfPluginIsNotOfNotificationType() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginLoaded(new GoPluginDescriptor(PLUGIN_ID_4, null, null, null, null, true));
        verify(notificationPluginRegistry, never()).registerPluginInterests(anyString(), anyList());
    }

    @Test
    public void shouldUnregisterPluginInterestsOnPluginUnLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginUnLoaded(new GoPluginDescriptor(PLUGIN_ID_1, null, null, null, null, true));
        verify(notificationPluginRegistry).removePluginInterests(PLUGIN_ID_1);
    }

    @Test
    public void shouldNotUnregisterPluginInterestsOnPluginUnLoadIfPluginIsNotOfNotificationType() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginUnLoaded(new GoPluginDescriptor(PLUGIN_ID_4, null, null, null, null, true));
        verify(notificationPluginRegistry, never()).removePluginInterests(PLUGIN_ID_4);
    }

    @Test
    public void shouldLogWarningIfPluginTriesToRegisterForInvalidNotificationType() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        Logger logger = mock(Logger.class);
        ReflectionUtil.setStaticField(NotificationPluginRegistrar.class, "LOGGER", logger);

        notificationPluginRegistrar.pluginLoaded(new GoPluginDescriptor(PLUGIN_ID_1, null, null, null, null, true));

        verify(logger).warn("Plugin 'plugin-id-1' is trying to register for 'pipeline-status' which is not a valid notification type. Valid notification types are: [stage-status]");
        verify(logger).warn("Plugin 'plugin-id-1' is trying to register for 'job-status' which is not a valid notification type. Valid notification types are: [stage-status]");
    }

    @Test
    public void shouldRegisterPluginOnPluginLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginLoaded(new GoPluginDescriptor(PLUGIN_ID_1, null, null, null, null, true));

        verify(notificationPluginRegistry).registerPlugin(PLUGIN_ID_1);
    }

    @Test
    public void shouldUnregisterPluginOnPluginUnLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginUnLoaded(new GoPluginDescriptor(PLUGIN_ID_1, null, null, null, null, true));

        verify(notificationPluginRegistry).deregisterPlugin(PLUGIN_ID_1);
    }
}
