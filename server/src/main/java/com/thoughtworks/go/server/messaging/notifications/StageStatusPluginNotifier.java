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

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.domain.StageStatusListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StageStatusPluginNotifier implements StageStatusListener {
    private NotificationPluginRegistry notificationPluginRegistry;
    private PluginNotificationService pluginNotificationService;

    @Autowired
    public StageStatusPluginNotifier(NotificationPluginRegistry notificationPluginRegistry, PluginNotificationService pluginNotificationService) {
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.pluginNotificationService = pluginNotificationService;
    }

    @Override
    public void stageStatusChanged(final Stage stage) {
        if (isAnyPluginInterestedInStageStatus() && isStageStateScheduledOrCompleted(stage)) {
            pluginNotificationService.notifyStageStatus(stage);
        }
    }

    private boolean isAnyPluginInterestedInStageStatus() {
        return notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION);
    }

    private boolean isStageStateScheduledOrCompleted(Stage stage) {
        return stage.isScheduled() || stage.isReRun() || stage.getState().completed();
    }
}
