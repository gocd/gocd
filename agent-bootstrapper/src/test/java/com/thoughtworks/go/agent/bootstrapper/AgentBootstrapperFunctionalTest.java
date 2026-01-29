/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.agent.testhelper.FakeGoServerExtension;
import com.thoughtworks.go.agent.testhelper.GoTestResource;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;

import static com.thoughtworks.go.agent.common.util.Downloader.*;
import static com.thoughtworks.go.agent.testhelper.FakeGoServer.TestResource.TEST_AGENT_LAUNCHER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FakeGoServerExtension.class)
public class AgentBootstrapperFunctionalTest {

    @GoTestResource
    FakeGoServer server;

    @BeforeEach
    public void setUp() throws IOException {
        new File(".agent-bootstrapper.running").delete();
        TEST_AGENT_LAUNCHER.copyTo(AGENT_LAUNCHER_JAR);
        System.setProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS, "0");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.deleteIfExists(AGENT_LAUNCHER_JAR.toPath());
        Files.deleteIfExists(AGENT_BINARY_JAR.toPath());
        Files.deleteIfExists(TFS_IMPL_JAR.toPath());
        Files.deleteIfExists(AGENT_PLUGINS_ZIP.toPath());
        System.clearProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS);
    }

    @Test
    public void shouldCheckout_Bundled_agentLauncher() throws IOException {
        try {
            AGENT_LAUNCHER_JAR.delete();
            new AgentBootstrapper().validate();
            assertEquals("agent launcher from default files", Files.readString(AGENT_LAUNCHER_JAR.toPath(), UTF_8).trim());
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
            new AgentBootstrapper(true).go(new AgentBootstrapperArgs().setServerUrl(getServerUrl()).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
            agentJar.delete();
            assertThat(os.toString()).contains("Hello World Fellas!");
        } finally {
            System.setErr(err);
        }
    }

    private @NonNull URL getServerUrl() throws MalformedURLException {
        return URI.create("http://localhost:" + server.getPort() + "/go").toURL();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void shouldDownloadJarIfItDoesNotExist() throws Exception {
        File agentJar = new File("agent.jar");
        agentJar.delete();
        new AgentBootstrapper(true).go(new AgentBootstrapperArgs().setServerUrl(getServerUrl()).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
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
        new AgentBootstrapper(true).go(new AgentBootstrapperArgs().setServerUrl(getServerUrl()).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
        assertThat(agentJar.length()).isNotEqualTo(original);
        agentJar.delete();
    }

    private void createRandomFile(File agentJar) throws IOException {
        Files.writeString(agentJar.toPath(), UUID.randomUUID().toString(), UTF_8);
    }
}
