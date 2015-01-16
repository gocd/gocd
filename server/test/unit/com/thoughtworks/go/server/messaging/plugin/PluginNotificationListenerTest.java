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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginNotificationListenerTest {
    @Mock
    private PluginNotificationService pluginNotificationService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldNotifyPlugins_onMessage() throws Exception {
        PluginNotificationListener pluginNotificationListener = new PluginNotificationListener(pluginNotificationService);

        Map dataMap = new HashMap();
        dataMap.put("pipeline-status", "scheduled");
        PluginNotificationMessage message = new PluginNotificationMessage("request-name", dataMap);
        pluginNotificationListener.onMessage(message);

        verify(pluginNotificationService).notifyPlugins(message);
    }
}