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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StageStatusPluginNotifier implements StageStatusListener {
    private NotificationPluginRegistry notificationPluginRegistry;
    private GoConfigService goConfigService;
    private PipelineSqlMapDao pipelineSqlMapDao;
    private PluginNotificationQueue pluginNotificationQueue;

    @Autowired
    public StageStatusPluginNotifier(NotificationPluginRegistry notificationPluginRegistry, GoConfigService goConfigService, PipelineSqlMapDao pipelineSqlMapDao, PluginNotificationQueue pluginNotificationQueue) {
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.goConfigService = goConfigService;
        this.pipelineSqlMapDao = pipelineSqlMapDao;
        this.pluginNotificationQueue = pluginNotificationQueue;
    }

    @Override
    public void stageStatusChanged(final Stage stage) {
        if (isAnyPluginInterestedInStageStatus() && isStageStateScheduledOrCompleted(stage)) {
            String pipelineName = stage.getIdentifier().getPipelineName();
            String pipelineGroup = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
            BuildCause buildCause = pipelineSqlMapDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, stage.getIdentifier().getPipelineCounter());
            StageNotificationData data = new StageNotificationData(stage, buildCause, pipelineGroup);
            pluginNotificationQueue.post(new PluginNotificationMessage<>(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION, data));
        }
    }

    private boolean isAnyPluginInterestedInStageStatus() {
        return notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION);
    }

    private boolean isStageStateScheduledOrCompleted(Stage stage) {
        return stage.isScheduled() || stage.isReRun() || stage.getState().completed();
    }
}
