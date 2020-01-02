/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.agent.bootstrapper;

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.testhelper.FakeGoServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.*;
import java.net.URL;

import static com.thoughtworks.go.agent.common.util.Downloader.*;
import static com.thoughtworks.go.agent.testhelper.FakeGoServer.TestResource.TEST_AGENT_LAUNCHER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class AgentBootstrapperFunctionalTest {

    @Rule
    public FakeGoServer server = new FakeGoServer();

    public static final OSChecker OS_CHECKER = new OSChecker(OSChecker.WINDOWS);

    @Before
    public void setUp() throws IOException {
        new File(".agent-bootstrapper.running").delete();
        TEST_AGENT_LAUNCHER.copyTo(AGENT_LAUNCHER_JAR);
        System.setProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS, "0");
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(AGENT_LAUNCHER_JAR);
        FileUtils.deleteQuietly(AGENT_BINARY_JAR);
        FileUtils.deleteQuietly(TFS_IMPL_JAR);
        FileUtils.deleteQuietly(AGENT_PLUGINS_ZIP);
        System.clearProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS);
    }

    @Test
    public void shouldCheckout_Bundled_agentLauncher() throws IOException {
        try {
            AGENT_LAUNCHER_JAR.delete();
            new AgentBootstrapper().validate();
            assertEquals("agent launcher from default files", FileUtils.readFileToString(AGENT_LAUNCHER_JAR, UTF_8).trim());
        } finally {
            AGENT_LAUNCHER_JAR.delete();
        }
    }

    @Test
    public void shouldLoadAndBootstrapJarUsingAgentBootstrapCode_specifiedInAgentManifestFile() throws Exception {
        if (!OS_CHECKER.satisfy()) {
            PrintStream err = System.err;
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                System.setErr(new PrintStream(os));
                File agentJar = new File("agent.jar");
                agentJar.delete();
                new AgentBootstrapper() {
                    @Override
                    void jvmExit(int returnValue) {
                    }
                }.go(false, new AgentBootstrapperArgs(new URL("http://" + "localhost" + ":" + server.getPort() + "/go"), null, AgentBootstrapperArgs.SslMode.NONE));
                agentJar.delete();
                assertThat(new String(os.toByteArray()), containsString("Hello World Fellas!"));
            } finally {
                System.setErr(err);
            }
        }
    }

    @Test
    public void shouldDownloadJarIfItDoesNotExist() throws Exception {
        if (!OS_CHECKER.satisfy()) {
            File agentJar = new File("agent.jar");
            agentJar.delete();
            new AgentBootstrapper() {
                @Override
                void jvmExit(int returnValue) {
                }
            }.go(false, new AgentBootstrapperArgs(new URL("http://" + "localhost" + ":" + server.getPort() + "/go"), null, AgentBootstrapperArgs.SslMode.NONE));
            assertTrue("No agent downloaded", agentJar.exists());
            agentJar.delete();
        }
    }

    @Test
    public void shouldDownloadJarIfTheCurrentOneIsWrong() throws Exception {
        if (!OS_CHECKER.satisfy()) {
            File agentJar = new File("agent.jar");
            agentJar.delete();
            createRandomFile(agentJar);
            long original = agentJar.length();
            new AgentBootstrapper() {
                @Override
                void jvmExit(int returnValue) {
                }
            }.go(false, new AgentBootstrapperArgs(new URL("http://" + "localhost" + ":" + server.getPort() + "/go"), null, AgentBootstrapperArgs.SslMode.NONE));
            assertThat(agentJar.length(), not(original));
            agentJar.delete();
        }
    }

    private void createRandomFile(File agentJar) throws IOException {
        FileUtils.writeStringToFile(agentJar, RandomStringUtils.random((int) (Math.random() * 100)), UTF_8);
    }
}
