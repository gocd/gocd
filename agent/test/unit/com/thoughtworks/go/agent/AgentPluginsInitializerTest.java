/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.launcher.DownloadableFile;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AgentPluginsInitializerTest {
    @Mock
    private ZipUtil zipUtil;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private DefaultPluginJarLocationMonitor pluginJarLocationMonitor;
    @Mock
    private SystemEnvironment systemEnvironment;

    private AgentPluginsInitializer agentPluginsInitializer;

    @Before
    public void setUp() throws Exception {
        agentPluginsInitializer = new AgentPluginsInitializer(pluginManager, pluginJarLocationMonitor, zipUtil, systemEnvironment);
        when(systemEnvironment.get(SystemEnvironment.AGENT_PLUGINS_PATH)).thenReturn(SystemEnvironment.PLUGINS_PATH);
    }

    @Test
    public void shouldExtractPluginZip() throws Exception {
        agentPluginsInitializer.onApplicationEvent(null);
        verify(zipUtil).unzip(new File(DownloadableFile.AGENT_PLUGINS.getLocalFileName()), new File(SystemEnvironment.PLUGINS_PATH));
    }

    @Test
    public void shouldInitializePluginJarLocationMonitorAndStartPluginInfrastructureAfterPluginZipExtracted() throws Exception {
        InOrder inOrder = inOrder(zipUtil, pluginManager, pluginJarLocationMonitor);
        agentPluginsInitializer.onApplicationEvent(null);
        inOrder.verify(zipUtil).unzip(new File(DownloadableFile.AGENT_PLUGINS.getLocalFileName()), new File(SystemEnvironment.PLUGINS_PATH));
        inOrder.verify(pluginJarLocationMonitor).initialize();
        inOrder.verify(pluginManager).startInfrastructure();
        verify(pluginManager, never()).registerPluginsFolderChangeListener();
    }

    @Test
    public void shouldHandleIOExceptionQuietly() throws Exception {
        doThrow(new IOException()).when(zipUtil).unzip(new File(DownloadableFile.AGENT_PLUGINS.getLocalFileName()), new File(SystemEnvironment.PLUGINS_PATH));
        try {
            agentPluginsInitializer.onApplicationEvent(null);
        } catch (Exception e) {
            fail("should have handled IOException");
        }
    }

    @Test
    public void shouldAllExceptionsExceptionQuietly() throws Exception {
        doThrow(new IOException()).when(zipUtil).unzip(new File(DownloadableFile.AGENT_PLUGINS.getLocalFileName()), new File(SystemEnvironment.PLUGINS_PATH));
        try {
            doThrow(new RuntimeException("message")).when(pluginJarLocationMonitor).initialize();
            agentPluginsInitializer.onApplicationEvent(null);
        } catch (Exception e) {
            fail("should have handled IOException");
        }
    }
}
