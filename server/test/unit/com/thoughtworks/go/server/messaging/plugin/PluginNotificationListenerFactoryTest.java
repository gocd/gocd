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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginNotificationListenerFactoryTest {
    @Mock
    private PluginNotificationQueue pluginNotificationQueue;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private PluginNotificationService pluginNotificationService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldAddRequiredNumberOfPluginNotificationListeners_init() throws Exception {
        when(systemEnvironment.getNumberOfPluginNotificationListener()).thenReturn(3);
        PluginNotificationListenerFactory pluginNotificationListenerFactory = new PluginNotificationListenerFactory(pluginNotificationQueue, systemEnvironment, pluginNotificationService);
        pluginNotificationListenerFactory.init();

        verify(pluginNotificationQueue, times(3)).addListener(any(PluginNotificationListener.class));
    }
}
