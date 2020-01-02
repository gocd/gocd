/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class NotificationPluginRegistrarTest {
    private static final String PLUGIN_ID_1 = "plugin-id-1";
    private static final String PLUGIN_ID_2 = "plugin-id-2";
    private static final String PLUGIN_ID_3 = "plugin-id-3";
    private static final String PLUGIN_ID_4 = "plugin-id-4";

    private static final String PIPELINE_STATUS = "pipeline-status";
    private static final String STAGE_STATUS = "stage-status";
    private static final String JOB_STATUS = "job-status";

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

        when(notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID_1)).thenReturn(List.of(PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS));
        when(notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID_2)).thenReturn(List.of(PIPELINE_STATUS));
        when(notificationExtension.getNotificationsOfInterestFor(PLUGIN_ID_3)).thenReturn(List.of(STAGE_STATUS));
    }

    @Test
    public void shouldRegisterPluginInterestsOnPluginLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_1).isBundledPlugin(true).build());
        verify(notificationPluginRegistry).registerPluginInterests(PLUGIN_ID_1, List.of(PIPELINE_STATUS, STAGE_STATUS, JOB_STATUS));

        notificationPluginRegistrar.pluginLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_2).isBundledPlugin(true).build());
        verify(notificationPluginRegistry).registerPluginInterests(PLUGIN_ID_2, List.of(PIPELINE_STATUS));

        notificationPluginRegistrar.pluginLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_3).isBundledPlugin(true).build());
        verify(notificationPluginRegistry).registerPluginInterests(PLUGIN_ID_3, List.of(STAGE_STATUS));
    }

    @Test
    public void shouldNotRegisterPluginInterestsOnPluginLoadIfPluginIfPluginIsNotOfNotificationType() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_4).isBundledPlugin(true).build());
        verify(notificationPluginRegistry, never()).registerPluginInterests(anyString(), anyList());
    }

    @Test
    public void shouldUnregisterPluginInterestsOnPluginUnLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginUnLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_1).isBundledPlugin(true).build());
        verify(notificationPluginRegistry).removePluginInterests(PLUGIN_ID_1);
    }

    @Test
    public void shouldNotUnregisterPluginInterestsOnPluginUnLoadIfPluginIsNotOfNotificationType() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginUnLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_4).isBundledPlugin(true).build());
        verify(notificationPluginRegistry, never()).removePluginInterests(PLUGIN_ID_4);
    }

    @Test
    public void shouldLogWarningIfPluginTriesToRegisterForInvalidNotificationType() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        Logger logger = mock(Logger.class);
        ReflectionUtil.setStaticField(NotificationPluginRegistrar.class, "LOGGER", logger);

        notificationPluginRegistrar.pluginLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_1).isBundledPlugin(true).build());

        verify(logger).warn("Plugin '{}' is trying to register for '{}' which is not a valid notification type. Valid notification types are: {}", "plugin-id-1", "pipeline-status", NotificationExtension.VALID_NOTIFICATION_TYPES);
        verify(logger).warn("Plugin '{}' is trying to register for '{}' which is not a valid notification type. Valid notification types are: {}", "plugin-id-1", "job-status", NotificationExtension.VALID_NOTIFICATION_TYPES);
    }

    @Test
    public void shouldRegisterPluginOnPluginLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_1).isBundledPlugin(true).build());

        verify(notificationPluginRegistry).registerPlugin(PLUGIN_ID_1);
    }

    @Test
    public void shouldUnregisterPluginOnPluginUnLoad() {
        NotificationPluginRegistrar notificationPluginRegistrar = new NotificationPluginRegistrar(pluginManager, notificationExtension, notificationPluginRegistry);

        notificationPluginRegistrar.pluginUnLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID_1).isBundledPlugin(true).build());

        verify(notificationPluginRegistry).deregisterPlugin(PLUGIN_ID_1);
    }
}
