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

package com.thoughtworks.go.agent.functional;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.agent.testhelpers.GoServerRunner;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashSet;
import java.util.Set;

import static com.thoughtworks.go.agent.testhelpers.FakeArtifactPublisherServlet.consoleOutput;
import static com.thoughtworks.go.agent.testhelpers.FakeArtifactPublisherServlet.receivedFiles;
import static com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote.JOB_PLAN_NAME;
import static com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote.PIPELINE_LABEL;
import static com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote.PIPELINE_NAME;
import static com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote.STAGE_NAME;
import static com.thoughtworks.go.agent.testhelpers.FakeBuildRepositoryRemote.waitUntilBuildCompleted;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@Ignore
@RunWith(GoServerRunner.class)
@ContextConfiguration(locations = {"/applicationContext.xml" })
public class AgentFunctionalTest {
    private static final String SERVER_URL = "https://localhost:8443/go";

    @BeforeClass
    public static void systemProperties() throws Exception {
        new SystemEnvironment().setProperty("serviceUrl", SERVER_URL);
        GoAgentServerHttpClientBuilder.AGENT_CERTIFICATE_FILE.delete();
        GoAgentServerHttpClientBuilder.AGENT_TRUST_FILE.delete();
    }

    @Test
    public void shouldRunBuildWhenWorkAssigned() throws Exception {
        waitUntilBuildCompleted();

        shouldPublishArtifactsToServer();
        shouldContainEnvironmentPropertiesWithServerUrl();
    }

    private void shouldPublishArtifactsToServer() throws Exception {
        assertThat(receivedFiles(), is(UPLOADED_FILENAMES));
    }

    private void shouldContainEnvironmentPropertiesWithServerUrl() throws Exception {
        String output = consoleOutput();
        assertThat(output, containsString("build.xml"));
        assertThat(output, containsString("[echo] Building all!"));
        assertThat(output, containsString("CRUISE_SERVER_URL: " + SERVER_URL));
        assertThat(output, containsString("CRUISE_PIPELINE_NAME: " + PIPELINE_NAME));
        assertThat(output, containsString("CRUISE_PIPELINE_LABEL: " + PIPELINE_LABEL));
        assertThat(output, containsString("CRUISE_STAGE_NAME: " + STAGE_NAME));
        assertThat(output, containsString("CRUISE_JOB_NAME: " + JOB_PLAN_NAME));
        assertThat(output, containsString("BUILD SUCCESSFUL"));
    }

    private static final Set<String> UPLOADED_FILENAMES = new HashSet<String>() {
        {
            add("log.xml.zip");
        }
    };
}
