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
package com.thoughtworks.go.agent.bootstrapper;

import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.testhelper.FakeGoServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import static com.thoughtworks.go.agent.common.util.Downloader.*;
import static com.thoughtworks.go.agent.testhelper.FakeGoServer.TestResource.TEST_AGENT_LAUNCHER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

@EnableRuleMigrationSupport
public class AgentBootstrapperFunctionalTest {

    @Rule
    public FakeGoServer server = new FakeGoServer();

    @BeforeEach
    public void setUp() throws IOException {
        new File(".agent-bootstrapper.running").delete();
        TEST_AGENT_LAUNCHER.copyTo(AGENT_LAUNCHER_JAR);
        System.setProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS, "0");
    }

    @AfterEach
    public void tearDown() {
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
    @DisabledOnOs(OS.WINDOWS)
    public void shouldLoadAndBootstrapJarUsingAgentBootstrapCode_specifiedInAgentManifestFile() throws Exception {
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
            }.go(false, new AgentBootstrapperArgs().setServerUrl(new URL("http://" + "localhost" + ":" + server.getPort() + "/go")).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
            agentJar.delete();
            assertThat(os.toString(), containsString("Hello World Fellas!"));
        } finally {
            System.setErr(err);
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void shouldDownloadJarIfItDoesNotExist() throws Exception {
        File agentJar = new File("agent.jar");
        agentJar.delete();
        new AgentBootstrapper() {
            @Override
            void jvmExit(int returnValue) {
            }
        }.go(false, new AgentBootstrapperArgs().setServerUrl(new URL("http://" + "localhost" + ":" + server.getPort() + "/go")).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
        assertTrue(agentJar.exists(), "No agent downloaded");
        agentJar.delete();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void shouldDownloadJarIfTheCurrentOneIsWrong() throws Exception {
        File agentJar = new File("agent.jar");
        agentJar.delete();
        createRandomFile(agentJar);
        long original = agentJar.length();
        new AgentBootstrapper() {
            @Override
            void jvmExit(int returnValue) {
            }
        }.go(false, new AgentBootstrapperArgs().setServerUrl(new URL("http://" + "localhost" + ":" + server.getPort() + "/go")).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
        assertThat(agentJar.length(), not(original));
        agentJar.delete();
    }

    private void createRandomFile(File agentJar) throws IOException {
        FileUtils.writeStringToFile(agentJar, RandomStringUtils.random((int) (Math.random() * 100)), UTF_8);
    }
}
