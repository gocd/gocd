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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.DirHandler;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThat;

public class BuildWorkArtifactFetchingTest {
    private static final String PIPELINE_NAME = "pipeline1";
    private static final String STAGE_NAME = "mingle";
    private static final String JOB_NAME = "run-ant";
    private static final String DEST = "lib";

    private static final String AGENT_UUID = "uuid";

    private static final String WITH_FETCH_FILE =
            "<job name=\"" + JOB_NAME + "\">\n"
                    + "  <tasks>\n"
                    + "    <fetchartifact artifactOrigin='gocd' stage='pre-mingle' job='" + JOB_NAME + "' srcfile='lib/hello.jar' dest='" + DEST + "'/>\n"
                    + "    <ant target=\"--help\" >\n"
                    + "      <runif status=\"failed\" />\n"
                    + "    </ant>\n"
                    + "  </tasks>\n"
                    + "</job>";

    private static final String WITH_FETCH_FOLDER =
            "<job name=\"" + JOB_NAME + "\">\n"
                    + "  <tasks>\n"
                    + "    <fetchartifact artifactOrigin='gocd' stage='pre-mingle' job='" + JOB_NAME + "' srcdir='lib' dest='" + DEST + "'/>\n"
                    + "    <ant target=\"--help\" >\n"
                    + "      <runif status=\"failed\" />\n"
                    + "    </ant>\n"
                    + "  </tasks>\n"
                    + "</job>";

    private BuildWork buildWork;
    private AgentIdentifier agentIdentifier;
    private com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub buildRepository;
    private EnvironmentVariableContext environmentVariableContext;

    @BeforeEach
    public void setUp() {
        PipelineConfigMother.createPipelineConfig(PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", AGENT_UUID);
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub();
        environmentVariableContext = new EnvironmentVariableContext();
    }

    @Test
    public void shouldFailTheJobWhenFetchingArtifactFails() throws Exception {
        buildWork = BuildWorkTest.getWork(WITH_FETCH_FILE, PIPELINE_NAME);
        com.thoughtworks.go.remote.work.FailedToDownloadPublisherStub stubPublisher = new com.thoughtworks.go.remote.work.FailedToDownloadPublisherStub();
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, stubPublisher, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), null, null, null, null));

        assertThat(stubPublisher.consoleOut()).contains("[go] Task: fetch artifact [lib/hello.jar] => [lib] from [pipeline1/pre-mingle/run-ant]");
        assertThat(stubPublisher.consoleOut()).contains("[go] Task status: failed");
        assertThat(stubPublisher.consoleOut()).contains("[go] Current job status: failed");

        assertThat(buildRepository.results)
            .doesNotContain(Passed)
            .contains(Failed);
    }

    @Test
    public void shouldTransitIntoFailedStatusWhenFetchingArtifactFailed() throws Exception {
        buildWork = BuildWorkTest.getWork(WITH_FETCH_FILE, PIPELINE_NAME);
        com.thoughtworks.go.remote.work.FailedToDownloadPublisherStub stubPublisher = new FailedToDownloadPublisherStub();
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, stubPublisher, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), null, null, null, null));

        assertThat(stubPublisher.consoleOut()).contains("[go] Task: fetch artifact [lib/hello.jar] => [lib] from [pipeline1/pre-mingle/run-ant]");
        assertThat(stubPublisher.consoleOut()).contains("[go] Task status: failed");
        assertThat(stubPublisher.consoleOut()).contains("[go] Task: ant --help");
        assertThat(stubPublisher.consoleOut()).contains("[go] Current job status: failed");
    }

    @Test
    public void shouldFetchFolder() throws Exception {
        buildWork = BuildWorkTest.getWork(WITH_FETCH_FOLDER, PIPELINE_NAME);
        GoArtifactsManipulatorStub stubManipulator = new GoArtifactsManipulatorStub();
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, stubManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), null, null, null, null));

        assertThat(stubManipulator.artifact().get(0)).isEqualTo(new DirHandler("lib", new File(String.join(File.separator, "pipelines", PIPELINE_NAME, DEST))));
    }
}
