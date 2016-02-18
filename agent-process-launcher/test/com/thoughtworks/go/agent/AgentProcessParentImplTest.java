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

package com.thoughtworks.go.agent;

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.common.util.LoggingHelper;
import com.thoughtworks.go.agent.testhelper.FakeBootstrapperServer;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import com.thoughtworks.go.util.FileDigester;
import com.thoughtworks.go.util.LogFixture;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static java.lang.System.getProperty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(FakeBootstrapperServer.class)
public class AgentProcessParentImplTest {

    private final File stderrLog = new File(AgentProcessParentImpl.GO_AGENT_STDERR_LOG);
    private final File stdoutLog = new File(AgentProcessParentImpl.GO_AGENT_STDOUT_LOG);

    @BeforeClass
    public static void setup() {
        System.setProperty(LoggingHelper.LOG_DIR, ".");
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(stdoutLog);
        FileUtils.deleteQuietly(stderrLog);
    }

    @Test
    public void shouldStartSubprocessWithCommandLine() throws InterruptedException, IOException {
        final List<String> cmd = new ArrayList<String>();
        String expectedAgentMd5 = FileDigester.md5DigestOfFile(new File("testdata/test-agent.jar"));
        String expectedAgentPluginsMd5 = FileDigester.md5DigestOfFile(new File("testdata/agent-plugins.zip"));
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd);
        int returnCode = bootstrapper.run("launcher_version", "bar", getURLGenerator(), new HashMap<String, String>());
        assertThat(returnCode, is(42));
        assertThat(cmd.get(0), is(getProperty("java.home") + getProperty("file.separator") + "bin" + getProperty("file.separator") + "java"));
        assertThat(cmd.get(1), is("-Dagent.launcher.version=launcher_version"));
        assertThat(cmd.get(2), is("-Dagent.plugins.md5=" + expectedAgentPluginsMd5));
        assertThat(cmd.get(3), is("-Dagent.binary.md5=" + expectedAgentMd5));
        assertThat(cmd.get(4), is("-Dagent.launcher.md5=bar"));
        assertThat(cmd.get(5), is("-jar"));
        assertThat(cmd.get(6), is("agent.jar"));
        assertThat(cmd.get(7), is("https://localhost:9443/go/"));
    }

    private Process mockProcess() throws InterruptedException {
        return mockProcess(new ByteArrayInputStream(new byte[0]), new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
    }

    private Process mockProcess(final InputStream outputStream, final InputStream errorStream, final OutputStream inputStream) throws InterruptedException {
        final Process subProcess = mock(Process.class);
        when(subProcess.waitFor()).thenReturn(42);
        when(subProcess.getInputStream()).thenReturn(outputStream);
        when(subProcess.getErrorStream()).thenReturn(errorStream);
        when(subProcess.getOutputStream()).thenReturn(inputStream);
        return subProcess;
    }

    @Test
    public void shouldStartSubprocess_withOverriddenArgs() throws InterruptedException, IOException {
        final List<String> cmd = new ArrayList<String>();
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd);
        int returnCode = bootstrapper.run("launcher_version", "bar", getURLGenerator(), m(AgentProcessParentImpl.AGENT_STARTUP_ARGS, "foo bar  baz with%20some%20space"));
        String expectedAgentMd5 = FileDigester.md5DigestOfFile(new File("testdata/test-agent.jar"));
        String expectedAgentPluginsMd5 = FileDigester.md5DigestOfFile(new File("testdata/agent-plugins.zip"));
        assertThat(returnCode, is(42));
        assertThat(cmd.get(0), is(getProperty("java.home") + getProperty("file.separator") + "bin" + getProperty("file.separator") + "java"));
        assertThat(cmd.get(1), is("foo"));
        assertThat(cmd.get(2), is("bar"));
        assertThat(cmd.get(3), is("baz"));
        assertThat(cmd.get(4), is("with some space"));
        assertThat(cmd.get(5), is("-Dagent.launcher.version=launcher_version"));
        assertThat(cmd.get(6), is("-Dagent.plugins.md5=" + expectedAgentPluginsMd5));
        assertThat(cmd.get(7), is("-Dagent.binary.md5=" + expectedAgentMd5));
        assertThat(cmd.get(8), is("-Dagent.launcher.md5=bar"));
        assertThat(cmd.get(9), is("-jar"));
        assertThat(cmd.get(10), is("agent.jar"));
        assertThat(cmd.get(11), is("https://localhost:9443/go/"));
    }

    private AgentProcessParentImpl createBootstrapper(final List<String> cmd) throws InterruptedException {
        final Process subProcess = mockProcess();
        return createBootstrapper(cmd, subProcess);
    }

    private AgentProcessParentImpl createBootstrapper(final List<String> cmd, final Process subProcess) {
        return new AgentProcessParentImpl() {
            @Override
            Process invoke(String[] command) throws IOException {
                cmd.addAll(Arrays.asList(command));
                return subProcess;
            }
        };
    }

    @Test
    public void shouldLogIntrruptOnAgentProcess() throws InterruptedException {
        final List<String> cmd = new ArrayList<String>();
        LogFixture logFixture = LogFixture.startListening();
        try {
            Process subProcess = mockProcess();
            when(subProcess.waitFor()).thenThrow(new InterruptedException("bang bang!"));
            AgentProcessParentImpl bootstrapper = createBootstrapper(cmd, subProcess);
            int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<String, String>());
            assertThat(returnCode, is(0));
            assertThat(logFixture.contains(Level.ERROR, "Agent was interrupted. Terminating agent and respawning. java.lang.InterruptedException: bang bang!"), is(true));
            verify(subProcess).destroy();
        } finally {
            logFixture.stopListening();
        }
    }

    @Test(timeout = 10 * 1000)//if it fails with timeout, that means stderr was not flushed -jj
    public void shouldLogErrorStreamOfSubprocess() throws InterruptedException, IOException {
        final List<String> cmd = new ArrayList<String>();
        Process subProcess = mockProcess();
        String stdErrMsg = "Mr. Agent writes to stderr!";
        when(subProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(stdErrMsg.getBytes()));
        String stdOutMsg = "Mr. Agent writes to stdout!";
        when(subProcess.getInputStream()).thenReturn(new ByteArrayInputStream(stdOutMsg.getBytes()));
        when(subProcess.waitFor()).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return 42;
            }
        });
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd, subProcess);
        int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<String, String>());
        assertThat(returnCode, is(42));
        assertThat(FileUtils.readFileToString(stderrLog).contains(stdErrMsg), is(true));
        assertThat(FileUtils.readFileToString(stdoutLog).contains(stdOutMsg), is(true));
    }

    @Test
    public void shouldLogFailureToStartSubprocess() throws InterruptedException {
        final List<String> cmd = new ArrayList<String>();
        LogFixture logFixture = LogFixture.startListening();
        try {
            AgentProcessParentImpl bootstrapper = new AgentProcessParentImpl() {
                @Override
                Process invoke(String[] command) throws IOException {
                    cmd.addAll(Arrays.asList(command));
                    throw new RuntimeException("something failed!");
                }
            };
            int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<String, String>());
            assertThat(returnCode, is(-373));
            assertThat(logFixture.contains(Level.ERROR, "Exception while executing command: " + StringUtils.join(cmd, " ") + " - java.lang.RuntimeException: something failed!"), is(true));
        } finally {
            logFixture.stopListening();
        }
    }

    @Test
    public void shouldClose_STDIN_and_STDOUT_ofSubprocess() throws InterruptedException {
        final List<String> cmd = new ArrayList<String>();
        LogFixture logFixture = LogFixture.startListening();
        try {
            final OutputStream stdin = mock(OutputStream.class);
            Process subProcess = mockProcess(new ByteArrayInputStream(new byte[0]), new ByteArrayInputStream(new byte[0]), stdin);
            when(subProcess.waitFor()).thenAnswer(new Answer<Object>() {
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    verify(stdin).close();
                    return 21;
                }
            });
            AgentProcessParentImpl bootstrapper = createBootstrapper(cmd, subProcess);
            int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<String, String>());
            assertThat(returnCode, is(21));
        } finally {
            logFixture.stopListening();
        }
    }

    public static final OSChecker OS_CHECKER = new OSChecker(OSChecker.WINDOWS);

    @Test
    public void shouldNotDownloadPluginsZipIfPresent() throws Exception {
        if (!OS_CHECKER.satisfy()) {
            File pluginZip = null;
            File agentLauncher = null;
            try {
                FileUtils.copyFile(new File("testdata/agent-plugins.zip"), pluginZip = new File(Downloader.AGENT_PLUGINS));
                pluginZip.setLastModified(System.currentTimeMillis() - 10 * 1000);
                long expectedModifiedDate = pluginZip.lastModified();
                AgentProcessParentImpl bootstrapper = createBootstrapper(new ArrayList<String>());
                bootstrapper.run("launcher_version", "bar", getURLGenerator(), m(AgentProcessParentImpl.AGENT_STARTUP_ARGS, "foo bar  baz with%20some%20space"));
                assertThat(new File(Downloader.AGENT_PLUGINS).lastModified(), is(expectedModifiedDate));
            } finally {
                delete(pluginZip, agentLauncher);
            }
        }
    }

    @Test
    public void shouldDownloadPluginsZipIfMissing() throws Exception {
        if (!OS_CHECKER.satisfy()) {
            File stalePluginZip = null;
            File agentLauncher = null;
            try {
                stalePluginZip = randomFile("agent-plugins.zip");
                long original = stalePluginZip.length();

                AgentProcessParentImpl bootstrapper = createBootstrapper(new ArrayList<String>());
                bootstrapper.run("launcher_version", "bar", getURLGenerator(), m(AgentProcessParentImpl.AGENT_STARTUP_ARGS, "foo bar  baz with%20some%20space"));

                assertThat(stalePluginZip.length(), not(original));
            } finally {
                delete(stalePluginZip, agentLauncher);
            }
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

    private ServerUrlGenerator getURLGenerator() {
        return ServerUrlGeneratorMother.generatorFor("localhost", 9090);
    }
}
