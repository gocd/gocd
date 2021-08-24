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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.testhelper.FakeGoServer;
import com.thoughtworks.go.agent.testhelper.FakeGoServerExtension;
import com.thoughtworks.go.agent.testhelper.GoTestResource;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.LogFixture;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.thoughtworks.go.agent.common.util.Downloader.*;
import static com.thoughtworks.go.agent.testhelper.FakeGoServer.TestResource.*;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(FakeGoServerExtension.class)
public class AgentProcessParentImplTest {

    @GoTestResource
    public FakeGoServer server;

    private final File stderrLog = new File("logs", AgentProcessParentImpl.GO_AGENT_STDERR_LOG);
    private final File stdoutLog = new File("logs", AgentProcessParentImpl.GO_AGENT_STDOUT_LOG);

    @BeforeAll
    public static void setup() {
        System.setProperty("sleep.for.download", "10");
    }

    @BeforeEach
    public void setUp() {
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("sleep.for.download");
        FileUtils.deleteQuietly(stdoutLog);
        FileUtils.deleteQuietly(stderrLog);

        cleanup();
    }

    private void cleanup() {
        FileUtils.deleteQuietly(AGENT_BINARY_JAR);
        FileUtils.deleteQuietly(AGENT_PLUGINS_ZIP);
        FileUtils.deleteQuietly(AGENT_LAUNCHER_JAR);
        FileUtils.deleteQuietly(TFS_IMPL_JAR);
    }

    @Test
    public void shouldStartSubprocessWithCommandLine() throws InterruptedException {
        final List<String> cmd = new ArrayList<>();
        String expectedAgentMd5 = TEST_AGENT.getMd5();
        String expectedAgentPluginsMd5 = TEST_AGENT_PLUGINS.getMd5();
        String expectedTfsMd5 = TEST_TFS_IMPL.getMd5();
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd);
        int returnCode = bootstrapper.run("launcher_version", "bar", getURLGenerator(), new HashMap<>(), context());
        assertThat(returnCode, is(42));
        assertThat(cmd.toArray(new String[]{}), equalTo(new String[]{
                (getProperty("java.home") + getProperty("file.separator") + "bin" + getProperty("file.separator") + "java"),
                "-Dagent.plugins.md5=" + expectedAgentPluginsMd5,
                "-Dagent.binary.md5=" + expectedAgentMd5,
                "-Dagent.launcher.md5=bar",
                "-Dagent.tfs.md5=" + expectedTfsMd5,
                "-Dagent.bootstrapper.version=UNKNOWN",
                "-jar",
                "agent.jar",
                "-serverUrl",
                "http://localhost:" + server.getPort() + "/go/",
                "-sslVerificationMode",
                "NONE",
                "-rootCertFile",
                new File("/path/to/cert.pem").getAbsolutePath()
        }));
    }

    @Test
    public void shouldAddBootstrapperVersionAsPropertyIfFoundInContext() throws InterruptedException {
        final List<String> cmd = new ArrayList<>();
        String expectedAgentMd5 = TEST_AGENT.getMd5();
        String expectedAgentPluginsMd5 = TEST_AGENT_PLUGINS.getMd5();
        String expectedTfsMd5 = TEST_TFS_IMPL.getMd5();

        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd);
        Map context = context();
        context.put(GoConstants.AGENT_BOOTSTRAPPER_VERSION, "20.3.0-1234");
        int returnCode = bootstrapper.run("launcher_version", "bar", getURLGenerator(), new HashMap<>(), context);

        assertThat(cmd.toArray(new String[]{}), equalTo(new String[]{
                (getProperty("java.home") + getProperty("file.separator") + "bin" + getProperty("file.separator") + "java"),
                "-Dagent.plugins.md5=" + expectedAgentPluginsMd5,
                "-Dagent.binary.md5=" + expectedAgentMd5,
                "-Dagent.launcher.md5=bar",
                "-Dagent.tfs.md5=" + expectedTfsMd5,
                "-Dagent.bootstrapper.version=20.3.0-1234",
                "-jar",
                "agent.jar",
                "-serverUrl",
                "http://localhost:" + server.getPort() + "/go/",
                "-sslVerificationMode",
                "NONE",
                "-rootCertFile",
                new File("/path/to/cert.pem").getAbsolutePath()
        }));
    }

    @Test
    public void shouldAddAnyExtraPropertiesFoundToTheAgentInvocation() throws InterruptedException {
        final List<String> cmd = new ArrayList<>();
        String expectedAgentMd5 = TEST_AGENT.getMd5();
        String expectedAgentPluginsMd5 = TEST_AGENT_PLUGINS.getMd5();
        String expectedTfsMd5 = TEST_TFS_IMPL.getMd5();

        server.setExtraPropertiesHeaderValue("extra.property=value1%20with%20space extra%20property%20with%20space=value2%20with%20space");
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd);
        int returnCode = bootstrapper.run("launcher_version", "bar", getURLGenerator(), new HashMap<>(), context());

        assertThat(returnCode, is(42));
        assertThat(cmd.toArray(new String[]{}), equalTo(new String[]{
                (getProperty("java.home") + getProperty("file.separator") + "bin" + getProperty("file.separator") + "java"),
                "-Dextra.property=value1 with space",
                "-Dextra property with space=value2 with space",
                "-Dagent.plugins.md5=" + expectedAgentPluginsMd5,
                "-Dagent.binary.md5=" + expectedAgentMd5,
                "-Dagent.launcher.md5=bar",
                "-Dagent.tfs.md5=" + expectedTfsMd5,
                "-Dagent.bootstrapper.version=UNKNOWN",
                "-jar",
                "agent.jar",
                "-serverUrl",
                "http://localhost:" + server.getPort() + "/go/",
                "-sslVerificationMode",
                "NONE",
                "-rootCertFile",
                new File("/path/to/cert.pem").getAbsolutePath()
        }));
    }

    @Test
    public void shouldAddSSLConfigurationIfProvided() throws InterruptedException {
        final List<String> cmd = new ArrayList<>();
        String expectedAgentMd5 = TEST_AGENT.getMd5();
        String expectedAgentPluginsMd5 = TEST_AGENT_PLUGINS.getMd5();
        String expectedTfsMd5 = TEST_TFS_IMPL.getMd5();
        Map context = context();

        context.put(AgentBootstrapperArgs.PRIVATE_KEY, "/path/to/private.key");
        context.put(AgentBootstrapperArgs.PRIVATE_KEY_PASSPHRASE_FILE, "/path/to/private_key_passphrase.key");
        context.put(AgentBootstrapperArgs.SSL_CERTIFICATE, "/path/to/ssl_certificate.pem");

        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd);
        int returnCode = bootstrapper.run("launcher_version", "bar", getURLGenerator(), new HashMap<>(), context);

        assertThat(returnCode, is(42));
        assertThat(cmd.toArray(new String[]{}), equalTo(new String[]{
                (getProperty("java.home") + getProperty("file.separator") + "bin" + getProperty("file.separator") + "java"),
                "-Dagent.plugins.md5=" + expectedAgentPluginsMd5,
                "-Dagent.binary.md5=" + expectedAgentMd5,
                "-Dagent.launcher.md5=bar",
                "-Dagent.tfs.md5=" + expectedTfsMd5,
                "-Dagent.bootstrapper.version=UNKNOWN",
                "-jar",
                "agent.jar",
                "-serverUrl",
                "http://localhost:" + server.getPort() + "/go/",
                "-sslVerificationMode",
                "NONE",
                "-rootCertFile",
                new File("/path/to/cert.pem").getAbsolutePath(),
                "-sslCertificateFile",
                new File("/path/to/ssl_certificate.pem").getAbsolutePath(),
                "-sslPrivateKeyFile",
                new File("/path/to/private.key").getAbsolutePath(),
                "-sslPrivateKeyPassphraseFile",
                new File("/path/to/private_key_passphrase.key").getAbsolutePath()
        }));
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
    public void shouldStartSubprocess_withOverriddenArgs() throws InterruptedException {
        final List<String> cmd = new ArrayList<>();
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd);
        int returnCode = bootstrapper.run("launcher_version", "bar", getURLGenerator(), m(AgentProcessParentImpl.AGENT_STARTUP_ARGS, "foo bar  baz with%20some%20space"), context());
        String expectedAgentMd5 = TEST_AGENT.getMd5();
        String expectedAgentPluginsMd5 = TEST_AGENT_PLUGINS.getMd5();
        String expectedTfsMd5 = TEST_TFS_IMPL.getMd5();

        assertThat(returnCode, is(42));
        assertThat(cmd.toArray(new String[]{}), equalTo(new String[]{
                (getProperty("java.home") + getProperty("file.separator") + "bin" + getProperty("file.separator") + "java"),
                "foo",
                "bar",
                "baz",
                "with some space",
                "-Dagent.plugins.md5=" + expectedAgentPluginsMd5,
                "-Dagent.binary.md5=" + expectedAgentMd5,
                "-Dagent.launcher.md5=bar",
                "-Dagent.tfs.md5=" + expectedTfsMd5,
                "-Dagent.bootstrapper.version=UNKNOWN",
                "-jar",
                "agent.jar",
                "-serverUrl",
                "http://localhost:" + server.getPort() + "/go/",
                "-sslVerificationMode",
                "NONE",
                "-rootCertFile",
                new File("/path/to/cert.pem").getAbsolutePath()
        }));
    }

    private Map context() {
        HashMap hashMap = new HashMap();
        hashMap.put(AgentBootstrapperArgs.SERVER_URL, getURLGenerator().serverUrlFor(""));
        hashMap.put(AgentBootstrapperArgs.SSL_VERIFICATION_MODE, "NONE");
        hashMap.put(AgentBootstrapperArgs.ROOT_CERT_FILE, "/path/to/cert.pem");
        return hashMap;
    }

    private AgentProcessParentImpl createBootstrapper(final List<String> cmd) throws InterruptedException {
        final Process subProcess = mockProcess();
        return createBootstrapper(cmd, subProcess);
    }

    private AgentProcessParentImpl createBootstrapper(final List<String> cmd, final Process subProcess) {
        return new AgentProcessParentImpl() {
            @Override
            Process invoke(String[] command) {
                cmd.addAll(Arrays.asList(command));
                return subProcess;
            }
        };
    }

    @Test
    public void shouldLogInterruptOnAgentProcess() throws InterruptedException {
        final List<String> cmd = new ArrayList<>();
        try (LogFixture logFixture = logFixtureFor(AgentProcessParentImpl.class, Level.DEBUG)) {
            Process subProcess = mockProcess();
            when(subProcess.waitFor()).thenThrow(new InterruptedException("bang bang!"));
            AgentProcessParentImpl bootstrapper = createBootstrapper(cmd, subProcess);
            int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<>(), context());
            assertThat(returnCode, is(0));
            assertThat(logFixture.contains(Level.ERROR, "Agent was interrupted. Terminating agent and respawning. java.lang.InterruptedException: bang bang!"), is(true));
            verify(subProcess).destroy();
        }
    }

    @Test
    @Timeout(10) //if it fails with timeout, that means stderr was not flushed
    public void shouldLogErrorStreamOfSubprocess() throws InterruptedException, IOException {
        final List<String> cmd = new ArrayList<>();
        Process subProcess = mockProcess();
        String stdErrMsg = "Mr. Agent writes to stderr!";
        when(subProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(stdErrMsg.getBytes()));
        String stdOutMsg = "Mr. Agent writes to stdout!";
        when(subProcess.getInputStream()).thenReturn(new ByteArrayInputStream(stdOutMsg.getBytes()));
        when(subProcess.waitFor()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return 42;
            }
        });
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd, subProcess);
        int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<>(), context());
        assertThat(returnCode, is(42));
        assertThat(FileUtils.readFileToString(stderrLog, UTF_8).contains(stdErrMsg), is(true));
        assertThat(FileUtils.readFileToString(stdoutLog, UTF_8).contains(stdOutMsg), is(true));
    }

    @Test
    public void shouldLogFailureToStartSubprocess() {
        final List<String> cmd = new ArrayList<>();

        try (LogFixture logFixture = logFixtureFor(AgentProcessParentImpl.class, Level.DEBUG)) {
            AgentProcessParentImpl bootstrapper = new AgentProcessParentImpl() {
                @Override
                Process invoke(String[] command) {
                    cmd.addAll(Arrays.asList(command));
                    throw new RuntimeException("something failed!");
                }
            };
            int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<>(), context());
            assertThat(returnCode, is(-373));
            assertThat(logFixture.contains(Level.ERROR, "Exception while executing command: " + StringUtils.join(cmd, " ") + " - java.lang.RuntimeException: something failed!"), is(true));
        }
    }

    @Test
    public void shouldClose_STDIN_and_STDOUT_ofSubprocess() throws InterruptedException {
        final List<String> cmd = new ArrayList<>();
        final OutputStream stdin = mock(OutputStream.class);
        Process subProcess = mockProcess(new ByteArrayInputStream(new byte[0]), new ByteArrayInputStream(new byte[0]), stdin);
        when(subProcess.waitFor()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                verify(stdin).close();
                return 21;
            }
        });
        AgentProcessParentImpl bootstrapper = createBootstrapper(cmd, subProcess);
        int returnCode = bootstrapper.run("bootstrapper_version", "bar", getURLGenerator(), new HashMap<>(), context());
        assertThat(returnCode, is(21));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void shouldNotDownloadPluginsZipIfPresent() throws Exception {
        TEST_AGENT_PLUGINS.copyTo(AGENT_PLUGINS_ZIP);
        AGENT_PLUGINS_ZIP.setLastModified(System.currentTimeMillis() - 10 * 1000);

        long expectedModifiedDate = AGENT_PLUGINS_ZIP.lastModified();
        AgentProcessParentImpl bootstrapper = createBootstrapper(new ArrayList<>());
        bootstrapper.run("launcher_version", "bar", getURLGenerator(), m(AgentProcessParentImpl.AGENT_STARTUP_ARGS, "foo bar  baz with%20some%20space"), context());
        assertThat(Downloader.AGENT_PLUGINS_ZIP.lastModified(), is(expectedModifiedDate));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void shouldDownloadPluginsZipIfMissing() throws Exception {
        File stalePluginZip = randomFile(AGENT_PLUGINS_ZIP);
        long original = stalePluginZip.length();

        AgentProcessParentImpl bootstrapper = createBootstrapper(new ArrayList<>());
        bootstrapper.run("launcher_version", "bar", getURLGenerator(), m(AgentProcessParentImpl.AGENT_STARTUP_ARGS, "foo bar  baz with%20some%20space"), context());

        assertThat(stalePluginZip.length(), not(original));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void shouldDownload_TfsImplJar_IfTheCurrentJarIsStale() throws Exception {
        File staleFile = randomFile(TFS_IMPL_JAR);
        long original = staleFile.length();

        AgentProcessParentImpl bootstrapper = createBootstrapper(new ArrayList<>());
        bootstrapper.run("launcher_version", "bar", getURLGenerator(), m(AgentProcessParentImpl.AGENT_STARTUP_ARGS, "foo bar  baz with%20some%20space"), context());

        assertThat(staleFile.length(), not(original));
    }

    private File randomFile(final File pathname) throws IOException {
        FileUtils.write(pathname, "some rubbish", StandardCharsets.UTF_8);
        return pathname;
    }

    private ServerUrlGenerator getURLGenerator() {
        return ServerUrlGeneratorMother.generatorFor("localhost", server.getPort());
    }
}
