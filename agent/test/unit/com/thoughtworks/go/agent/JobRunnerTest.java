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

package com.thoughtworks.go.agent;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.Tasks;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.helper.BuilderMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.GoArtifactsManipulatorStub;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.work.FakeWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(JunitExtRunner.class)
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
        runner.handleInstruction(new AgentInstruction(false), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
        assertThat(work.getCallCount(), is(0));
    }

    @Test
    public void shouldCancelOncePerJob() {
        runner.setWork(work);
        runner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
        assertThat(work.getCallCount(), is(1));

        runner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
        assertThat(work.getCallCount(), is(1));
    }

    @Test
    public void shouldReturnTrueOnGetJobIsCancelledWhenJobIsCancelled() {
        assertThat(runner.isJobCancelled(), is(false));
        runner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
        assertThat(runner.isJobCancelled(), is(true));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    @Ignore("it will random pass if we sleep a short time, but sleep 15 seconds is too long in unit test")
    public void shouldCancelCurrentJob() throws Exception {
        final JobRunner jobRunner = new JobRunner();

        ExecTask secondExec = new ExecTask("sleep", "10", (String) null);
        secondExec.setCancelTask(new ExecTask("echo", "cancel in progress", (String) null));
        buildWork = getWork(new JobConfig(new CaseInsensitiveString(JOB_PLAN_NAME), new Resources(), new ArtifactPlans(), new Tasks(
                new ExecTask("echo", "should run me before cancellation", (String) null),
                secondExec,
                new ExecTask("echo", "should not run after cancellation", (String) null))));

        Thread worker = new Thread(new Runnable() {
            public void run() {
                jobRunner.run(buildWork, agentIdentifier,
                        new BuildRepositoryRemoteStub(), stubPublisher(properties, consoleOut),
                        new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), null, null, null);
            }
        });
        Thread cancel = new Thread(new Runnable() {
            public void run() {
                jobRunner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
            }
        });

        worker.start();
        // sleep 3 secs so the worker thread gets a chance to run first two tasks
        try {
            Thread.sleep(5000);
        } catch (Exception ignore) {
        }
        cancel.start();
        cancel.join();

        worker.join();

        assertThat(consoleOut.toString(), containsString("should run me before cancellation"));
        assertThat(consoleOut.toString(),
                containsString("Start to execute cancel task: <exec command=\"echo\" args=\"cancel in progress\" />"));
        assertThat(consoleOut.toString(), containsString("cancel in progress"));
        assertThat(consoleOut.toString(), containsString("Task is cancelled"));
        assertThat(consoleOut.toString(), not(containsString("should not run after cancellation")));

        assertThat(statesAndResult.toString(), statesAndResult.contains(JobResult.Cancelled), is(true));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    @Ignore("it will random pass if we sleep a short time, but sleep 15 seconds is too long in unit test")
    public void shouldWaitForCancelTaskToFinish() throws Exception {
        final JobRunner jobRunner = new JobRunner();

        ExecTask secondExec = new ExecTask("sleep", "10", (String) null);
        secondExec.setCancelTask(new ExecTask("sleep", "15", (String) null));
        buildWork = getWork(new JobConfig(new CaseInsensitiveString(JOB_PLAN_NAME), new Resources(), new ArtifactPlans(), new Tasks(
                new ExecTask("echo", "should run me before cancellation", (String) null),
                secondExec,
                new ExecTask("echo", "should not run after cancellation", (String) null))));

        Thread worker = new Thread(new Runnable() {
            public void run() {
                jobRunner.run(buildWork, agentIdentifier,
                        new BuildRepositoryRemoteStub(), stubPublisher(properties, consoleOut),
                        new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), null, null, null);
            }
        });
        Thread cancel = new Thread(new Runnable() {
            public void run() {
                jobRunner.handleInstruction(new AgentInstruction(true), new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
            }
        });

        worker.start();
        // sleep 2 secs so the worker thread gets a chance to run first two tasks
        try {
            Thread.sleep(5000);
        } catch (Exception ignore) {
        }
        cancel.start();

        cancel.join();
        worker.join();

        String output = consoleOut.toString();
        assertThat(output.indexOf("Task is cancelled") < output.indexOf("Job completed"), is(true));

        assertThat(statesAndResult.toString(), statesAndResult.contains(JobResult.Cancelled), is(true));
    }

    private GoArtifactsManipulatorStub stubPublisher(final List<Property> properties,
                                                     final List<String> consoleOuts) {
        return new GoArtifactsManipulatorStub(properties, consoleOuts);
    }

}
