/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.helper.BuilderMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.GoArtifactsManipulatorStub;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.work.FakeWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class JobRunnerTest {
    private static final String SERVER_URL = "somewhere-does-not-matter";
    private static final String JOB_PLAN_NAME = "run-ant";
    private JobRunner runner;
    private FakeWork work;
    private List<String> consoleOut;
    private List<Enum> statesAndResult;
    private List<Property> properties;
    private BuildWork buildWork;
    private AgentIdentifier agentIdentifier;
    private UpstreamPipelineResolver resolver;
    private TimeProvider timeProvider;

    public static String withJob(String jobXml) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                + GoConstants.CONFIG_SCHEMA_VERSION + "\">\n"
                + " <server artifactsdir=\"logs\"></server>"
                + "  <pipelines>\n"
                + "    <pipeline name=\"pipeline1\">\n"
                + "      <materials>\n"
                + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
                + "      </materials>\n"
                + "      <stage name=\"mingle\">\n"
                + "       <jobs>\n"
                + jobXml
                + "        </jobs>\n"
                + "      </stage>\n"
                + "    </pipeline>\n"
                + "  </pipelines>\n"
                + "  <agents>\n"
                + "    <agent hostname=\"agent1\" ipaddress=\"1.2.3.4\" uuid=\"ywZRuHFIKvw93TssFeWl8g==\" />\n"
                + "  </agents>"
                + "</cruise>";
    }

    @Before
    public void setUp() throws Exception {
        timeProvider = mock(TimeProvider.class);
        runner = new JobRunner();
        work = new FakeWork();
        consoleOut = new ArrayList<>();
        statesAndResult = new ArrayList<>();
        properties = new ArrayList<>();
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");

        new SystemEnvironment().setProperty("serviceUrl", SERVER_URL);
        resolver = mock(UpstreamPipelineResolver.class);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    private BuildWork getWork(JobConfig jobConfig) {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().setArtifactsDir("logs");
        String stageName = "mingle";
        String pipelineName = "pipeline1";
        config.addPipeline(BasicPipelineConfigs.DEFAULT_GROUP, new PipelineConfig(new CaseInsensitiveString(pipelineName), new MaterialConfigs(), new StageConfig(
                new CaseInsensitiveString(stageName), new JobConfigs(jobConfig))));

        String pipelineLabel = "100";
        JobPlan jobPlan = JobInstanceMother.createJobPlan(jobConfig, new JobIdentifier(pipelineName, -2, pipelineLabel, stageName, "100", JOB_PLAN_NAME, 0L), new DefaultSchedulingContext());
        jobPlan.setFetchMaterials(true);
        jobPlan.setCleanWorkingDir(false);

        List<Builder> builder = BuilderMother.createBuildersAssumingAllExecTasks(config, pipelineName, stageName, JOB_PLAN_NAME);

        BuildAssignment buildAssignment = BuildAssignment.create(jobPlan, BuildCause.createWithEmptyModifications(), builder, new File(CruiseConfig.WORKING_BASE_DIR + pipelineName));
        return new BuildWork(buildAssignment);
    }

    @Test
    public void shouldDoNothingWhenJobIsNotCancelled() {
        runner.setWork(work);
        runner.handleInstruction(new AgentInstruction(false), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider));
        assertThat(work.getCallCount(), is(0));
    }

    @Test
    public void shouldCancelOncePerJob() {
        runner.setWork(work);
        runner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider));
        assertThat(work.getCallCount(), is(1));

        runner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider));
        assertThat(work.getCallCount(), is(1));
    }

    @Test
    public void shouldReturnTrueOnGetJobIsCancelledWhenJobIsCancelled() {
        assertThat(runner.isJobCancelled(), is(false));
        runner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider));
        assertThat(runner.isJobCancelled(), is(true));
    }

    private GoArtifactsManipulatorStub stubPublisher(final List<Property> properties,
                                                     final List<String> consoleOuts) {
        return new GoArtifactsManipulatorStub(properties, consoleOuts);
    }

}
