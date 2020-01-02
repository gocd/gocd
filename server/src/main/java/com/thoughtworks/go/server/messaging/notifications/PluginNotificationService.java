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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static com.thoughtworks.go.util.SystemEnvironment.NOTIFICATION_PLUGIN_MESSAGES_TTL;

@Component
public class PluginNotificationService {
    private final NotificationPluginRegistry notificationPluginRegistry;
    private final PluginNotificationsQueueHandler pluginNotificationsQueueHandler;
    private final GoConfigService goConfigService;
    private final PipelineDao pipelineSqlMapDao;
    private StageDao stageDao;
    private SystemEnvironment systemEnvironment;
    private final HashMap<String, NotificationDataCreator> map = new HashMap<>();

    @Autowired
    public PluginNotificationService(NotificationPluginRegistry notificationPluginRegistry,
                                     PluginNotificationsQueueHandler pluginNotificationsQueueHandler,
                                     GoConfigService goConfigService,
                                     PipelineDao pipelineSqlMapDao, StageDao stageDao, SystemEnvironment systemEnvironment) {
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.pluginNotificationsQueueHandler = pluginNotificationsQueueHandler;
        this.goConfigService = goConfigService;
        this.pipelineSqlMapDao = pipelineSqlMapDao;
        this.stageDao = stageDao;
        this.systemEnvironment = systemEnvironment;
        map.put(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION, new StageNotificationDataCreator());
        map.put(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION, new AgentNotificationDataCreator());
    }

    public void notifyAgentStatus(AgentInstance agentInstance) {
        notify(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION, agentInstance);
    }

    public void notifyStageStatus(Stage stage) {
        notify(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION, stage);
    }

    private <T> void notify(String requestName, T instance) {
        Set<String> interestedPlugins = notificationPluginRegistry.getPluginsInterestedIn(requestName);
        Long timeToLive = systemEnvironment.get(NOTIFICATION_PLUGIN_MESSAGES_TTL);
        for (String pluginId : interestedPlugins) {
            PluginNotificationMessage message = new PluginNotificationMessage<>(pluginId, requestName, map.get(requestName).notificationDataFor(instance));
            pluginNotificationsQueueHandler.post(message, timeToLive);
        }
    }

    private class AgentNotificationDataCreator implements NotificationDataCreator<AgentInstance, AgentNotificationData> {
        @Override
        public AgentNotificationData notificationDataFor(AgentInstance agentInstance) {
            return new AgentNotificationData(agentInstance.getUuid(),
                    agentInstance.getHostname(),
                    agentInstance.isElastic(),
                    agentInstance.getIpAddress(),
                    agentInstance.getOperatingSystem(),
                    agentInstance.freeDiskSpace().toString(),
                    agentInstance.getAgentConfigStatus().name(),
                    agentInstance.getRuntimeStatus().agentState().name(),
                    agentInstance.getRuntimeStatus().buildState().name(),
                    new Date()
            );
        }
    }

    private class StageNotificationDataCreator implements NotificationDataCreator<Stage, StageNotificationData> {
        @Override
        public StageNotificationData notificationDataFor(Stage stage) {
            String pipelineName = stage.getIdentifier().getPipelineName();
            String pipelineGroup = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
            BuildCause buildCause = pipelineSqlMapDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, stage.getIdentifier().getPipelineCounter());

            if (goConfigService.hasPreviousStage(pipelineName, stage.getName())) {
                stage.setPreviousStage(previousStage(stage));
            }

            return new StageNotificationData(stage, buildCause, pipelineGroup);
        }
    }

    private StageIdentifier previousStage(Stage stage) {
        StageIdentifier previousStageIdentifier = null;
        StageIdentifier currentStageIdentifier = stage.getIdentifier();
        String pipelineName = currentStageIdentifier.getPipelineName();

        if (isStageAutomaticallyTriggered(stage) || (isManualStage(stage) && !isStageReRun(stage))) {
            StageConfig previousStage = goConfigService.previousStage(pipelineName, stage.getName());
            int latestCounter = stageDao.findLatestStageCounter(stage.getIdentifier().pipelineIdentifier(), previousStage.name().toString());
            previousStageIdentifier = new StageIdentifier(currentStageIdentifier.pipelineIdentifier(), previousStage.name().toString(), Integer.toString(latestCounter));
        }

        return previousStageIdentifier;
    }

    private boolean isStageReRun(Stage stage) {
        return stage.getCounter() > 1;
    }

    private boolean isStageAutomaticallyTriggered(Stage stage) {
        return "changes".equalsIgnoreCase(stage.getApprovedBy());
    }

    private boolean isManualStage(Stage stage) {
        return "manual".equalsIgnoreCase(stage.getApprovalType());
    }

    private interface NotificationDataCreator<T, V extends Serializable> {
        V notificationDataFor(T t);
    }
}
