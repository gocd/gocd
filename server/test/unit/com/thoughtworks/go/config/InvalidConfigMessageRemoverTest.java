/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Type;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InvalidConfigMessageRemoverTest {
    @Test
    public void shouldRemoveServerHealthServiceMessageAboutStartedWithInvalidConfigurationOnPipelineConfigSave() {
        GoConfigService goConfigService = mock(GoConfigService.class);
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        ArgumentCaptor<ConfigChangedListener> configChangedListenerArgumentCaptor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(configChangedListenerArgumentCaptor.capture());
        InvalidConfigMessageRemover invalidConfigMessageRemover = new InvalidConfigMessageRemover(goConfigService, serverHealthService);
        invalidConfigMessageRemover.initialize();
        invalidConfigMessageRemover.onConfigChange(null);
        List<ConfigChangedListener> listeners = configChangedListenerArgumentCaptor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener listener = (EntityConfigChangedListener) listeners.get(1);
        assertThat(listener.shouldCareAbout(mock(PipelineConfig.class)), is(true));

        invalidConfigMessageRemover.pipelineConfigChangedListener().onEntityConfigChange(mock(PipelineConfig.class));

        ArgumentCaptor<HealthStateScope> captor = ArgumentCaptor.forClass(HealthStateScope.class);
        verify(serverHealthService).removeByScope(captor.capture());
        assertThat(captor.getValue().compareTo(HealthStateScope.forInvalidConfig()), is(0));
    }

}