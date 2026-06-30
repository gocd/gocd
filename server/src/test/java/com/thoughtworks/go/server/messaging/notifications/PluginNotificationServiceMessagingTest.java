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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.messaging.activemq.ActiveMqMessagingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.plugin.access.notification.NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION;
import static com.thoughtworks.go.plugin.access.notification.NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Exercises the full plugin notification flow against a real (in-process) ActiveMQ broker: a message posted by
 * {@link PluginNotificationService} is serialized onto the queue, delivered, deserialized, and handed to the
 * notification extension. This guards against notification payloads (e.g. {@link StageNotificationData}) regressing
 * to a type that cannot round-trip through Java serialization, which only surfaces at runtime as a failed broker send.
 */
@ExtendWith(MockitoExtension.class)
public class PluginNotificationServiceMessagingTest {
    private static final String PLUGIN_ID = "plugin-id";

    @Mock private NotificationExtension notificationExtension;
    @Mock private NotificationPluginRegistry notificationPluginRegistry;
    @Mock private GoConfigService goConfigService;
    @Mock private PipelineDao pipelineDao;
    @Mock private StageDao stageDao;
    @Mock private PluginManager pluginManager;
    @Mock private ServerHealthService serverHealthService;
    @Mock private DaemonThreadStatsCollector daemonThreadStatsCollector;

    private ActiveMqMessagingService messaging;
    private PluginNotificationService pluginNotificationService;

    @BeforeEach
    public void setUp() throws Exception {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        messaging = new ActiveMqMessagingService(daemonThreadStatsCollector, systemEnvironment, serverHealthService);

        PluginNotificationsQueueHandler queueHandler = new PluginNotificationsQueueHandler(messaging, notificationExtension, pluginManager, systemEnvironment, serverHealthService);
        pluginNotificationService = new PluginNotificationService(notificationPluginRegistry, queueHandler, goConfigService, pipelineDao, stageDao, systemEnvironment);

        // Spin up the per-plugin queue and its listening consumer, as would happen when the notification plugin loads.
        when(notificationExtension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        queueHandler.pluginLoaded(GoPluginDescriptor.builder().id(PLUGIN_ID).build());

        when(notificationExtension.notify(anyString(), anyString(), any())).thenReturn(new Result());
    }

    @AfterEach
    public void tearDown() throws Exception {
        messaging.stop();
    }

    @Test
    public void shouldSerializeAndDeserializeStageNotificationDataAcrossTheBroker() {
        Stage stage = StageMother.custom("Stage");
        BuildCause buildCause = BuildCause.createManualForced();
        when(notificationPluginRegistry.getPluginsInterestedIn(STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(List.of(PLUGIN_ID)));
        when(goConfigService.isFirstStage(stage.getIdentifier().getPipelineName(), stage.getName())).thenReturn(true);
        when(goConfigService.findGroupNameByPipelineOptional(cis(stage.getIdentifier().getPipelineName()))).thenReturn(Optional.of("group1"));
        when(pipelineDao.findBuildCauseOfPipelineByNameAndCounter(stage.getIdentifier().getPipelineName(), stage.getIdentifier().getPipelineCounter())).thenReturn(buildCause);

        pluginNotificationService.notifyStageStatus(stage);

        ArgumentCaptor<StageNotificationData> captor = ArgumentCaptor.forClass(StageNotificationData.class);
        verify(notificationExtension, timeout(5000)).notify(eq(PLUGIN_ID), eq(STAGE_STATUS_CHANGE_NOTIFICATION), captor.capture());

        StageNotificationData received = captor.getValue();
        assertThat(received.pipelineGroup()).isEqualTo("group1");
        assertThat(received.stage().getName()).isEqualTo(stage.getName());
        assertThat(received.stage().getIdentifier()).isEqualTo(stage.getIdentifier());
        assertThat(received.buildCause()).isNotNull();
    }

    @Test
    public void shouldSerializeAndDeserializeAgentNotificationDataAcrossTheBroker() {
        AgentInstance agentInstance = AgentInstanceMother.building();
        when(notificationPluginRegistry.getPluginsInterestedIn(AGENT_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(List.of(PLUGIN_ID)));

        pluginNotificationService.notifyAgentStatus(agentInstance);

        ArgumentCaptor<AgentNotificationData> captor = ArgumentCaptor.forClass(AgentNotificationData.class);
        verify(notificationExtension, timeout(5000)).notify(eq(PLUGIN_ID), eq(AGENT_STATUS_CHANGE_NOTIFICATION), captor.capture());

        AgentNotificationData received = captor.getValue();
        assertThat(received.uuid()).isEqualTo(agentInstance.getUuid());
        assertThat(received.hostName()).isEqualTo(agentInstance.getHostname());
        assertThat(received.agentState()).isEqualTo(agentInstance.getRuntimeStatus().agentState().name());
        assertThat(received.buildState()).isEqualTo(agentInstance.getRuntimeStatus().buildState().name());
    }
}
