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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.NOTIFICATION_PLUGIN_MESSAGES_TTL;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginNotificationServiceTest {
    public static final String PLUGIN_ID_1 = "plugin-id-1";
    public static final String PLUGIN_ID_2 = "plugin-id-2";
    public static final String PLUGIN_ID_3 = "plugin-id-3";

    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private PluginNotificationsQueueHandler pluginNotificationsQueueHandler;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PipelineDao pipelineDao;
    @Mock
    private StageDao stageDao;
    @Mock
    private SystemEnvironment systemEnvironment;
    private PluginNotificationService pluginNotificationService;

    @BeforeEach
    public void setUp() throws Exception {
        pluginNotificationService = new PluginNotificationService(notificationPluginRegistry, pluginNotificationsQueueHandler, goConfigService, pipelineDao, stageDao, systemEnvironment);
    }

    @Test
    public void shouldConstructDataForAgentNotification() {
        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);

        AgentInstance agentInstance = AgentInstanceMother.building();
        pluginNotificationService.notifyAgentStatus(agentInstance);
        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        verify(pluginNotificationsQueueHandler).post(captor.capture(), eq(1000L));

        PluginNotificationMessage message = captor.getValue();
        assertThat(message.pluginId(), is(PLUGIN_ID_1));
        assertThat(message.getRequestName(), is(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION));
        assertThat(message.getData() instanceof AgentNotificationData, is(true));
        AgentNotificationData data = (AgentNotificationData) message.getData();
        assertThat(data.getUuid(), is(agentInstance.getUuid()));
        assertThat(data.getHostName(), is(agentInstance.getHostname()));
        assertFalse(data.isElastic());
        assertThat(data.getIpAddress(), is(agentInstance.getIpAddress()));
        assertThat(data.getFreeSpace(), is(agentInstance.freeDiskSpace().toString()));
        assertThat(data.getAgentConfigState(), is(agentInstance.getAgentConfigStatus().name()));
        assertThat(data.getAgentState(), is(agentInstance.getRuntimeStatus().agentState().name()));
        assertThat(data.getBuildState(), is(agentInstance.getRuntimeStatus().buildState().name()));
    }

    @Test
    public void shouldConstructDataForElasticAgentNotification() {
        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);
        ElasticAgentRuntimeInfo agentRuntimeInfo = new ElasticAgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, "/foo/one", null, "42", "go.cd.elastic-agent-plugin.docker");

        Agent agent = new Agent("some-uuid");
        agent.setElasticAgentId("42");
        agent.setElasticPluginId("go.cd.elastic-agent-plugin.docker");
        agent.setIpaddress("127.0.0.1");

        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        agentInstance.update(agentRuntimeInfo);

        pluginNotificationService.notifyAgentStatus(agentInstance);
        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        verify(pluginNotificationsQueueHandler).post(captor.capture(), eq(1000L));

        PluginNotificationMessage message = captor.getValue();
        assertThat(message.pluginId(), is(PLUGIN_ID_1));
        assertThat(message.getRequestName(), is(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION));
        assertThat(message.getData() instanceof AgentNotificationData, is(true));
        AgentNotificationData data = (AgentNotificationData) message.getData();
        assertTrue(data.isElastic());
    }

    @Test
    public void shouldConstructDataForStageNotification() {
        Stage stage = StageMother.custom("Stage");
        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(stage.getIdentifier().getPipelineName()))).thenReturn("group1");
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);
        BuildCause buildCause = BuildCause.createManualForced();
        when(pipelineDao.findBuildCauseOfPipelineByNameAndCounter(stage.getIdentifier().getPipelineName(), stage.getIdentifier().getPipelineCounter())).thenReturn(buildCause);
        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);

        pluginNotificationService.notifyStageStatus(stage);
        verify(pluginNotificationsQueueHandler).post(captor.capture(), eq(1000L));

        PluginNotificationMessage message = captor.getValue();
        assertThat(message.pluginId(), is(PLUGIN_ID_1));
        assertThat(message.getRequestName(), is(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION));
        assertThat(message.getData() instanceof StageNotificationData, is(true));
        StageNotificationData data = (StageNotificationData) message.getData();
        assertThat(data.getStage(), is(stage));
        assertThat(data.getBuildCause(), is(buildCause));
        assertThat(data.getPipelineGroup(), is("group1"));
    }

    @Test
    public void populatePreviousStage_ifStageIsTriggeredByChangesAndHasAPreviousStage() {
        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        Stage stage = StageMother.custom("Stage");
        StageConfig previousStage = new StageConfig(new CaseInsensitiveString("previous_stage"), null);

        stage.setApprovedBy("changes");
        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(goConfigService.hasPreviousStage(stage.getIdentifier().getPipelineName(), stage.getName())).thenReturn(true);
        when(goConfigService.previousStage(stage.getIdentifier().getPipelineName(), stage.getName())).thenReturn(previousStage);
        when(stageDao.findLatestStageCounter(stage.getIdentifier().pipelineIdentifier(), previousStage.name().toString())).thenReturn(1);
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);

        pluginNotificationService.notifyStageStatus(stage);
        verify(pluginNotificationsQueueHandler).post(captor.capture(), eq(1000L));

        PluginNotificationMessage message = captor.getValue();
        StageNotificationData data = (StageNotificationData) message.getData();

        assertThat(data.getStage().getPreviousStage().getPipelineName(), is(stage.getIdentifier().getPipelineName()));
        assertThat(data.getStage().getPreviousStage().getPipelineCounter(), is(stage.getIdentifier().getPipelineCounter()));
        assertThat(data.getStage().getPreviousStage().getStageName(), is("previous_stage"));
        assertThat(data.getStage().getPreviousStage().getStageCounter(), is("1"));
    }

    @Test
    public void populatePreviousStage_forStageWithManualApprovalTypeAndHasAPreviousStage() {
        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        Stage stage = StageMother.custom("Stage");
        StageConfig previousStage = new StageConfig(new CaseInsensitiveString("previous_stage"), null);

        stage.setApprovedBy("admins");
        stage.setApprovalType("manual");
        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(goConfigService.hasPreviousStage(stage.getIdentifier().getPipelineName(), stage.getName())).thenReturn(true);
        when(goConfigService.previousStage(stage.getIdentifier().getPipelineName(), stage.getName())).thenReturn(previousStage);
        when(stageDao.findLatestStageCounter(stage.getIdentifier().pipelineIdentifier(), previousStage.name().toString())).thenReturn(1);
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);

        pluginNotificationService.notifyStageStatus(stage);
        verify(pluginNotificationsQueueHandler).post(captor.capture(), eq(1000L));

        PluginNotificationMessage message = captor.getValue();
        StageNotificationData data = (StageNotificationData) message.getData();

        assertThat(data.getStage().getPreviousStage().getPipelineName(), is(stage.getIdentifier().getPipelineName()));
        assertThat(data.getStage().getPreviousStage().getPipelineCounter(), is(stage.getIdentifier().getPipelineCounter()));
        assertThat(data.getStage().getPreviousStage().getStageName(), is("previous_stage"));
        assertThat(data.getStage().getPreviousStage().getStageCounter(), is("1"));
    }

    @Test
    public void shouldNotPopulatePreviousStage_forManualStageReRuns() {
        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        Stage stage = StageMother.custom("Stage");

        stage.setApprovedBy("admins");
        stage.setApprovalType("manual");
        stage.setCounter(2);
        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(goConfigService.hasPreviousStage(stage.getIdentifier().getPipelineName(), stage.getName())).thenReturn(true);
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);

        pluginNotificationService.notifyStageStatus(stage);
        verify(pluginNotificationsQueueHandler).post(captor.capture(), eq(1000L));

        PluginNotificationMessage message = captor.getValue();
        StageNotificationData data = (StageNotificationData) message.getData();

        assertNull(data.getStage().getPreviousStage());
    }

    @Test
    public void shouldNotPopulatePreviousStage_forStageWithoutAPreviousStage() {
        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        Stage stage = StageMother.custom("Stage");

        stage.setApprovedBy("admins");
        stage.setApprovalType("manual");

        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(goConfigService.hasPreviousStage(stage.getIdentifier().getPipelineName(), stage.getName())).thenReturn(false);
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);

        pluginNotificationService.notifyStageStatus(stage);
        verify(pluginNotificationsQueueHandler).post(captor.capture(), eq(1000L));

        PluginNotificationMessage message = captor.getValue();
        StageNotificationData data = (StageNotificationData) message.getData();

        assertNull(data.getStage().getPreviousStage());
    }

    @Test
    public void shouldNotifyInterestedPluginsCorrectly() {
        Result result = new Result();
        result.withSuccessMessages("success message");
        when(notificationPluginRegistry.getPluginsInterestedIn(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1, PLUGIN_ID_2)));
        when(systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL)).thenReturn(1000L);

        AgentInstance agentInstance = AgentInstanceMother.lostContact();
        pluginNotificationService.notifyAgentStatus(agentInstance);

        ArgumentCaptor<PluginNotificationMessage> captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        verify(pluginNotificationsQueueHandler, times(2)).post(captor.capture(), eq(1000L));
        List<PluginNotificationMessage> messages = captor.getAllValues();
        assertThat(messages.size(), is(2));
        assertMessage(messages.get(0), PLUGIN_ID_1, NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION, agentInstance);
        assertMessage(messages.get(1), PLUGIN_ID_2, NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION, agentInstance);
    }

    private void assertMessage(PluginNotificationMessage notificationMessage, String pluginId, String requestName, AgentInstance agentInstance) {
        assertThat(notificationMessage.pluginId(), is(pluginId));
        assertThat(notificationMessage.getRequestName(), is(requestName));
        assertThat(notificationMessage.getData(), instanceOf(AgentNotificationData.class));
        AgentNotificationData data = (AgentNotificationData) notificationMessage.getData();
        assertThat(data.getUuid(), is(agentInstance.getUuid()));
        assertThat(data.getAgentState(), is(agentInstance.getStatus().toString()));
    }
}
