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

package com.thoughtworks.go.agent.launcher;

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptor;
import com.thoughtworks.cruise.agent.common.launcher.AgentLauncher;
import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.common.util.JarUtil;
import com.thoughtworks.go.agent.testhelper.FakeBootstrapperServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(FakeBootstrapperServer.class)
public class AgentLauncherImplTest {

    public static final OSChecker OS_CHECKER = new OSChecker(OSChecker.WINDOWS);

    @Before
    public void setUp() throws IOException {
        createLauncherFile(new File("testdata/agent-launcher.jar"));
        new Lockfile(new File(AgentLauncherImpl.AGENT_BOOTSTRAPPER_LOCK_FILE)).delete();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File("testdata/agent-launcher.jar"));
        FileUtils.deleteQuietly(new File(Downloader.AGENT_BINARY));
        FileUtils.deleteQuietly(new File(Downloader.AGENT_LAUNCHER));
        new Lockfile(new File(AgentLauncherImpl.AGENT_BOOTSTRAPPER_LOCK_FILE)).delete();
    }

    @Test
    public void shouldPassLauncherVersionToAgent() throws InterruptedException, IOException {
        File agentLauncher = null;
        final String version = "12.3";
        final List<String> actualVersion = new ArrayList<String>();
        final AgentLauncher launcher = new AgentLauncherImpl(new AgentLauncherImpl.AgentProcessParentRunner() {
            public int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlConstructor, Map<String, String> environmentVariables, Map context) {
                actualVersion.add(launcherVersion);
                return 0;
            }
        });
        try {
            FileUtils.copyFile(new File("testdata/agent-launcher.jar"), agentLauncher = new File(Downloader.AGENT_LAUNCHER));
            launcher.launch(launchDescriptor());
        } finally {
            delete(agentLauncher);
        }

        assertThat(actualVersion.size(), is(1));
        assertThat(actualVersion.get(0), is(version));
    }

    @Test
    public void shouldNotThrowException_insteedReturnAppropriateErrorCode_whenSomethingGoesWrongInLaunch() {
        AgentLaunchDescriptor launchDesc = mock(AgentLaunchDescriptor.class);
        when((String) launchDesc.context().get(AgentBootstrapperArgs.SERVER_URL)).thenThrow(new RuntimeException("Ouch!"));
        try {
            assertThat(new AgentLauncherImpl().launch(launchDesc), is(-273));
        } catch (Exception e) {
            fail("should not have blown up, because it directly interfaces with bootstrapper");
        }
    }

    private AgentLaunchDescriptor launchDescriptor() {
        AgentLaunchDescriptor launchDescriptor = mock(AgentLaunchDescriptor.class);
        Map contextMap = new ConcurrentHashMap();
        contextMap.put(AgentBootstrapperArgs.SERVER_URL, "http://localhost:9090/go");
        contextMap.put(AgentBootstrapperArgs.SSL_VERIFICATION_MODE, "NONE");
        when(launchDescriptor.context()).thenReturn(contextMap);
        return launchDescriptor;
    }

    private void createLauncherFile(File file) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name(JarUtil.GO_VERSION), "12.3");
        JarOutputStream out = new JarOutputStream(new FileOutputStream(file), manifest);
        IOUtils.closeQuietly(out);
    }

    @Test
    public void shouldDownloadLauncherJarIfLocalCopyIsStale() {
        //because new invocation will take care of pulling latest agent down, and will then operate on it with the latest launcher -jj
        File agentFile = null;
        File launcher = null;
        try {
            launcher = randomFile("agent-launcher.jar");
            long original = launcher.length();
            new AgentLauncherImpl().launch(launchDescriptor());
            assertThat(launcher.length(), not(original));
        } finally {
            delete(agentFile, launcher);
        }
    }

    @Test
    public void shouldDownload_AgentJar_IfTheCurrentJarIsStale() throws Exception {
        if (!OS_CHECKER.satisfy()) {
            File staleAgentJar = null;
            File agentLauncher = null;
            try {
                FileUtils.copyFile(new File("testdata/agent-launcher.jar"), agentLauncher = new File(Downloader.AGENT_LAUNCHER));
                staleAgentJar = randomFile("agent.jar");
                long original = staleAgentJar.length();
                new AgentLauncherImpl().launch(launchDescriptor());
                assertThat(staleAgentJar.length(), not(original));
            } finally {
                delete(staleAgentJar, agentLauncher);
            }
        }
    }

    @Test
    public void should_NOT_Download_AgentJar_IfTheCurrentJarIsUpToDate() throws Exception {
        if (!OS_CHECKER.satisfy()) {
            File agentJar = null;
            File agentLauncher = null;
            try {
                agentLauncher = new File(Downloader.AGENT_LAUNCHER);
                FileUtils.copyFile(new File("testdata/agent-launcher.jar"), agentLauncher);
                FileUtils.copyFile(new File("testdata/test-agent.jar"), new File(Downloader.AGENT_BINARY));
                new File(Downloader.AGENT_BINARY).setLastModified(System.currentTimeMillis() - 10 * 1000);//10 seconds ago
                long lastModifiedAt = new File(Downloader.AGENT_BINARY).lastModified();
                agentJar = new File(Downloader.AGENT_BINARY);
                new AgentLauncherImpl().launch(launchDescriptor());
                assertThat(agentJar.lastModified(), is(lastModifiedAt));
            } finally {
                delete(agentJar, agentLauncher);
            }
        }

    }

    @Test
    public void shouldDownloadLauncherJarIfLocalCopyIsStale_butShouldReturnWithoutDownloadingOrLaunchingAgent() {
        //because new invocation will take care of pulling latest agent down, and will then operate on it with the latest launcher -jj
        File agentFile = null;
        File launcher = null;
        try {
            launcher = randomFile("agent-launcher.jar");
            long original = launcher.length();
            agentFile = randomFile("agent.jar");
            long originalAgentLength = agentFile.length();
            new AgentLauncherImpl().launch(launchDescriptor());
            assertThat(launcher.length(), not(original));
            assertThat(agentFile.length(), is(originalAgentLength));
        } finally {
            delete(agentFile, launcher);
        }
    }

    private File randomFile(final String pathname) {
        File agentJar = new File(pathname);
        agentJar.delete();
        createRandomFile(agentJar, "some rubbish");
        return agentJar;
    }

    private void createRandomFile(File agentJar, final String data) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(agentJar);
            IOUtils.write(data, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    private void delete(File... files) {
        for (File file : files) {
            if (file != null) {
                file.delete();
            }
        }
    }
}
