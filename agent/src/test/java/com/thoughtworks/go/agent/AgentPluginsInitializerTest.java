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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.launcher.DownloadableFile;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentPluginsInitializerTest {
    @Mock
    private ZipUtil zipUtil;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private DefaultPluginJarLocationMonitor pluginJarLocationMonitor;
    @Mock
    private SystemEnvironment systemEnvironment;

    private AgentPluginsInitializer agentPluginsInitializer;

    @BeforeEach
    void setUp() {
        agentPluginsInitializer = new AgentPluginsInitializer(pluginManager, pluginJarLocationMonitor, zipUtil, systemEnvironment);
        when(systemEnvironment.get(SystemEnvironment.AGENT_PLUGINS_PATH)).thenReturn(SystemEnvironment.PLUGINS_PATH);
    }

    @Test
    void shouldExtractPluginZip() throws Exception {
        agentPluginsInitializer.onApplicationEvent(null);
        verify(zipUtil).unzip(DownloadableFile.AGENT_PLUGINS.getLocalFile(), new File(SystemEnvironment.PLUGINS_PATH));
    }

    @Test
    void shouldInitializePluginJarLocationMonitorAndStartPluginInfrastructureAfterPluginZipExtracted() throws Exception {
        InOrder inOrder = inOrder(zipUtil, pluginManager, pluginJarLocationMonitor);
        agentPluginsInitializer.onApplicationEvent(null);
        inOrder.verify(zipUtil).unzip(DownloadableFile.AGENT_PLUGINS.getLocalFile(), new File(SystemEnvironment.PLUGINS_PATH));
        inOrder.verify(pluginJarLocationMonitor).initialize();
        inOrder.verify(pluginManager).startInfrastructure(false);
    }

    @Test
    void shouldHandleIOExceptionQuietly() throws Exception {
        doThrow(new IOException()).when(zipUtil).unzip(DownloadableFile.AGENT_PLUGINS.getLocalFile(), new File(SystemEnvironment.PLUGINS_PATH));
        try {
            agentPluginsInitializer.onApplicationEvent(null);
        } catch (Exception e) {
            fail("should have handled IOException");
        }
    }

    @Test
    void shouldAllExceptionsExceptionQuietly() throws Exception {
        doThrow(new IOException()).when(zipUtil).unzip(DownloadableFile.AGENT_PLUGINS.getLocalFile(), new File(SystemEnvironment.PLUGINS_PATH));
        agentPluginsInitializer.onApplicationEvent(null);
    }
}
