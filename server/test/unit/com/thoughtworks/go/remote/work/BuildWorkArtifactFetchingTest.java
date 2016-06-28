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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.DirHandler;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.matchers.ConsoleOutMatcher.containsResult;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class BuildWorkArtifactFetchingTest {
    public static final String PIPELINE_NAME = "pipeline1";
    public static final String PIPELINE_LABEL = "100";
    public static final String STAGE_NAME = "mingle";
    public static final String JOB_NAME = "run-ant";
    private static final String DEST = "lib";

    private static final String AGENT_UUID = "uuid";
    File buildWorkingDirectory;

    private static final String WITH_FETCH_FILE =
                      "<job name=\"" + JOB_NAME + "\">\n"
                    + "  <tasks>\n"
                    + "    <fetchartifact stage='pre-mingle' job='" + JOB_NAME + "' srcfile='lib/hello.jar' dest='" + DEST + "'/>\n"
                    + "    <ant target=\"--help\" >\n"
                    + "      <runif status=\"failed\" />\n"
                    + "    </ant>\n"
                    + "  </tasks>\n"
                    + "</job>";

    private static final String WITH_FETCH_FOLDER =
            "<job name=\"" + JOB_NAME + "\">\n"
                    + "  <tasks>\n"
                    + "    <fetchartifact stage='pre-mingle' job='" + JOB_NAME + "' srcdir='lib' dest='" + DEST + "'/>\n"
                    + "    <ant target=\"--help\" >\n"
                    + "      <runif status=\"failed\" />\n"
                    + "    </ant>\n"
                    + "  </tasks>\n"
                    + "</job>";

    private BuildWork buildWork;
    private AgentIdentifier agentIdentifier;
    private com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub buildRepository;
    private EnvironmentVariableContext environmentVariableContext;

    @Before
    public void setUp() throws IOException {
        buildWorkingDirectory = new File("tmp");
        PipelineConfigMother.createPipelineConfig(PIPELINE_NAME, STAGE_NAME, JOB_NAME);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", AGENT_UUID);
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub();
        environmentVariableContext = new EnvironmentVariableContext();
    }

    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        FileUtil.deleteFolder(buildWorkingDirectory);
    }

    @Test
    public void shouldFailTheJobWhenFetchingArtifactFails() throws Exception {
        buildWork = (BuildWork) BuildWorkTest.getWork(WITH_FETCH_FILE, PIPELINE_NAME);
        com.thoughtworks.go.remote.work.FailedToDownloadPublisherStub stubPublisher = new com.thoughtworks.go.remote.work.FailedToDownloadPublisherStub();
        buildWork.doWork(agentIdentifier, buildRepository, stubPublisher, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), null, null, null);

        assertThat(stubPublisher.consoleOut(), containsString("[go] Current job status: passed."));
        assertThat(stubPublisher.consoleOut(), containsString("[go] Start to execute task: <fetchartifact pipeline=\"pipeline1\" stage=\"pre-mingle\" job=\"" + JOB_NAME + "\" srcfile=\"lib/hello.jar\" dest=\"lib\" />."));

        assertThat(buildRepository.results, not(containsResult(Passed)));
        assertThat(buildRepository.results, containsResult(Failed));
    }

    @Test
    public void shouldTransitIntoFailedStatusWhenFetchingArtifactFailed() throws Exception {
        buildWork = (BuildWork) BuildWorkTest.getWork(WITH_FETCH_FILE, PIPELINE_NAME);
        com.thoughtworks.go.remote.work.FailedToDownloadPublisherStub stubPublisher = new FailedToDownloadPublisherStub();
        buildWork.doWork(agentIdentifier, buildRepository, stubPublisher, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), null, null, null);

        assertThat(stubPublisher.consoleOut(), containsString("[go] Current job status: failed."));
        assertThat(stubPublisher.consoleOut(), containsString("[go] Start to execute task: <ant target=\"--help\" />."));
    }

    @Test
    public void shouldFetchFolder() throws Exception {
        buildWork = (BuildWork) BuildWorkTest.getWork(WITH_FETCH_FOLDER, PIPELINE_NAME);
        GoArtifactsManipulatorStub stubManipulator = new GoArtifactsManipulatorStub();
        buildWork.doWork(agentIdentifier, buildRepository, stubManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), null, null, null);

        assertThat((DirHandler) stubManipulator.artifact().get(0), is(new DirHandler("lib",new File("pipelines" + File.separator + PIPELINE_NAME + File.separator + DEST))));
    }
}
