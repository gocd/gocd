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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginNotificationListenerFactory {
    private PluginNotificationQueue pluginNotificationQueue;
    private SystemEnvironment systemEnvironment;
    private PluginNotificationService pluginNotificationService;

    @Autowired
    public PluginNotificationListenerFactory(PluginNotificationQueue pluginNotificationQueue, SystemEnvironment systemEnvironment, PluginNotificationService pluginNotificationService) {
        this.pluginNotificationQueue = pluginNotificationQueue;
        this.systemEnvironment = systemEnvironment;
        this.pluginNotificationService = pluginNotificationService;
    }

    public void init() {
        int numberOfListeners = systemEnvironment.getNumberOfPluginNotificationListener();

        for (int i = 0; i < numberOfListeners; i++) {
            pluginNotificationQueue.addListener(new PluginNotificationListener(pluginNotificationService));
        }
    }
}
