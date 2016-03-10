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

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.server.service.builders.*;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.domain.JobState.*;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.matchers.ConsoleOutMatcher.*;
import static com.thoughtworks.go.matchers.RegexMatcher.matches;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static com.thoughtworks.go.util.SystemUtil.isWindows;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(JunitExtRunner.class)
public class BuildWorkTest {

    public static final String PIPELINE_NAME = "pipeline1";
    public static final String PIPELINE_LABEL = "100";
    public static final String STAGE_NAME = "mingle";
    public static final String JOB_PLAN_NAME = "run-ant";
    private BuildWork buildWork;
    private AgentIdentifier agentIdentifier;

    private static final String NANT = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <nant target=\"-help\"/>\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String NANT_WITH_WORKINGDIR = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <nant target=\"-help\" workingdir=\"not-exists\" />\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String RAKE = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <rake target=\"--help\"/>\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String WILL_FAIL = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <ant target=\"something-not-really-exist\" />\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String WILL_PASS = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <ant target=\"-help\" />\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String WITH_ENV_VAR = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <environmentvariables>\n"
            + "    <variable name=\"JOB_ENV\">\n"
            + "      <value>foobar</value>\n"
            + "    </variable>\n"
            + "    <variable name=\"" + (isWindows() ? "Path": "PATH") +"\">\n"
            + "      <value>/tmp</value>\n"
            + "    </variable>\n"
            + "  </environmentvariables>\n"
            + "  <tasks>\n"
            + "    <ant target=\"-help\" />\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String WITH_SECRET_ENV_VAR = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <environmentvariables>\n"
            + "    <variable name=\"foo\">\n"
            + "      <value>foo(i am a secret)</value>\n"
            + "    </variable>\n"
            + "    <variable name=\"bar\" secure=\"true\">\n"
            + "      <value>i am a secret</value>\n"
            + "    </variable>\n"
            + "  </environmentvariables>\n"
            + "  <tasks>\n"
            + "    <ant target=\"-help\" />\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String SOMETHING_NOT_EXIST = "something-not-exist";

    private static final String CMD_NOT_EXIST = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <exec command=\"" + SOMETHING_NOT_EXIST + "\" />\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String WILL_NOT_RUN = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <exec command=\"echo\" args=\"run when status is failed\">\n"
            + "      <runif status=\"failed\" />\n"
            + "    </exec>\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String MULTIPLE_TASKS = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <exec command=\"command-not-found\" >\n"
            + "    </exec>\n"
            + "    <exec command=\"echo\" args=\"run when status is failed\">\n"
            + "      <runif status=\"failed\" />\n"
            + "    </exec>\n"
            + "    <exec command=\"echo\" args=\"run when status is passed\">\n"
            + "      <runif status=\"passed\" />\n"
            + "    </exec>\n"
            + "    <exec command=\"echo\" args=\"run when status is any\">\n"
            + "      <runif status=\"any\" />\n"
            + "    </exec>\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String MULTIPLE_RUN_IFS = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <exec command=\"echo\" args=\"run when status is failed, passed or cancelled\">\n"
            + "      <runif status=\"failed\" />\n"
            + "      <runif status=\"passed\" />\n"
            + "    </exec>\n"
            + "  </tasks>\n"
            + "</job>";


    private EnvironmentVariableContext environmentVariableContext;
    private com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub buildRepository;
    private GoArtifactsManipulatorStub artifactManipulator;
    private static BuilderFactory builderFactory = new BuilderFactory(new AntTaskBuilder(), new ExecTaskBuilder(), new NantTaskBuilder(), new RakeTaskBuilder(),
            new PluggableTaskBuilderCreator(mock(TaskExtension.class)), new KillAllChildProcessTaskBuilder(), new FetchTaskBuilder(), new NullTaskBuilder());
    @Mock
    private static UpstreamPipelineResolver resolver;
    @Mock
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;

    private static String willUpload(String file) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <artifact src=\"something-not-there.txt\" dest=\"dist\" />\n"
                + "      <artifact src=\"" + file + "\" dest=\"dist\\test\" />\n"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <ant target=\"-help\" />\n"
                + "  </tasks>\n"
                + "</job>";

    }

    private static String willUploadToDest(String file, String dest) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <artifact src=\"" + file + "\" dest=\"" + dest + "\" />\n"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <ant target=\"-help\" />\n"
                + "  </tasks>\n"
                + "</job>";

    }

    private static final int STAGE_COUNTER = 100;
    private static final String SERVER_URL = "somewhere-does-not-matter";
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier(PIPELINE_NAME, -3, PIPELINE_LABEL, STAGE_NAME, String.valueOf(STAGE_COUNTER), JOB_PLAN_NAME, 1L);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
        environmentVariableContext = new EnvironmentVariableContext();
        artifactManipulator = new GoArtifactsManipulatorStub();
        new SystemEnvironment().setProperty("serviceUrl", SERVER_URL);
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub();
    }

    @After
    public void tearDown() {
        new SystemEnvironment().clearProperty("serviceUrl");
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldReportStatus() throws Exception {
        buildWork = (BuildWork) getWork(WILL_FAIL, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(buildRepository.states, containsState(Preparing));
        assertThat(buildRepository.states, containsState(Building));
        assertThat(buildRepository.states, containsState(Completing));
    }

    @Test
    public void shouldNotRunTaskWhichConditionDoesNotMatch() throws Exception {
        buildWork = (BuildWork) getWork(WILL_NOT_RUN, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String actual = artifactManipulator.consoleOut();
        assertThat(actual, not(containsString("run when status is failed")));
    }

    @Test
    public void shouldRunTaskWhenConditionMatches() throws Exception {
        buildWork = (BuildWork) getWork(MULTIPLE_RUN_IFS, PIPELINE_NAME);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator,
                environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String actual = artifactManipulator.consoleOut();
        assertThat(actual, containsString("[go] Current job status: passed."));
        assertThat(actual, containsString("[go] Start to execute task: <exec command=\"echo\" args=\"run when status is failed, passed or cancelled\" />."));
        assertThat(actual, containsString("run when status is failed, passed or cancelled"));
    }

    @Test
    public void shouldRunTasksBasedOnConditions() throws Exception {
        buildWork = (BuildWork) getWork(MULTIPLE_TASKS, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String actual = artifactManipulator.consoleOut();

        assertThat(actual, containsString("run when status is failed"));
        assertThat(actual, printedExcRunIfInfo("command-not-found", "passed"));
        assertThat(actual, containsString("run when status is any"));
        assertThat(actual, printedExcRunIfInfo("echo", "run when status is any", "failed"));
        assertThat(actual, not(containsString("run when status is passed")));
        assertThat(actual, not(printedExcRunIfInfo("echo", "run when status is passed", "failed")));
        assertThat(actual, not(containsString("run when status is cancelled")));
        assertThat(actual, not(printedExcRunIfInfo("echo", "run when status is cancelled", "failed")));
    }

    @Test
    public void shouldReportBuildIsFailedWhenAntBuildFailed() throws Exception {
        buildWork = (BuildWork) getWork(WILL_FAIL, PIPELINE_NAME);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(buildRepository.results, containsResult(Failed));
    }

    @Test
    public void shouldReportDirectoryNotExists() throws Exception {
        buildWork = (BuildWork) getWork(NANT_WITH_WORKINGDIR, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(artifactManipulator.consoleOut(), containsString("not-exists\" is not a directory!"));
    }

    @Test
    public void shouldReportUploadMessageWhenUpload() throws Exception {
        String destFolder = "dest\\test\\sub-folder";
        final String url = String.format("%s/remoting/files/%s/%s/%s/%s/%s/%s?attempt=1&buildId=0", SERVER_URL, PIPELINE_NAME, PIPELINE_LABEL, STAGE_NAME, STAGE_COUNTER, JOB_PLAN_NAME,
                destFolder.replaceAll("\\\\", "/"));


        buildWork = (BuildWork) getWork(willUploadToDest("cruise-output/log.xml", destFolder), PIPELINE_NAME);
        com.thoughtworks.go.remote.work.HttpServiceStub httpService = new com.thoughtworks.go.remote.work.HttpServiceStub(HttpServletResponse.SC_OK);
        artifactManipulator = new GoArtifactsManipulatorStub(httpService);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String actual = artifactManipulator.consoleOut();
        artifactManipulator.printConsoleOut();
        File basedir = new File("pipelines/pipeline1");

        assertThat(actual.toLowerCase(), containsString(("Uploading artifacts from " + new File(basedir, "cruise-output/log.xml").getCanonicalPath()).toLowerCase()));

        Map<String, File> uploadedFiles = httpService.getUploadedFiles();

        assertThat(uploadedFiles.size(), is(1));
        assertThat(uploadedFiles.get(url).getAbsolutePath(), containsString("log.xml.zip"));
    }

    @Test
    public void shouldFailTheJobWhenFailedToUploadArtifact() throws Exception {
        buildWork = (BuildWork) getWork(willUpload("cruise-output/log.xml"), PIPELINE_NAME);
        artifactManipulator = new GoArtifactsManipulatorStub(new HttpServiceStub(SC_NOT_ACCEPTABLE));

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String actual = artifactManipulator.consoleOut();

        File basedir = new File("pipelines/pipeline1");
        assertThat(actual, printedUploadingFailure(new File(basedir, "cruise-output/log.xml")));
        assertThat(buildRepository.results, containsResult(Failed));
    }

    @Test
    public void shouldReportBuildIsFailedWhenAntBuildPassed() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(buildRepository.results, containsResult(Passed));
    }

    @Test
    public void shouldSendAResultStatusToServerWhenAThrowableErrorIsThrown() throws Exception {
        JobPlan jobPlan = mock(JobPlan.class);
        when(jobPlan.shouldFetchMaterials()).thenThrow(new AssertionError());
        when(jobPlan.getIdentifier()).thenReturn(JOB_IDENTIFIER);

        createBuildWorkWithJobPlan(jobPlan);

        try {
            buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
            fail("Should have thrown an assertion error");
        } catch (AssertionError e) {
            assertThat(buildRepository.results.isEmpty(), is(true));
            assertThat(buildRepository.states.size(), is(1));
            assertThat(buildRepository.states.get(0), is(JobState.Preparing));
        }
    }

    @Test
    public void shouldSendAResultStatusToServerWhenAnExceptionIsThrown() throws Exception {
        JobPlan jobPlan = mock(JobPlan.class);
        when(jobPlan.shouldFetchMaterials()).thenThrow(new RuntimeException());
        when(jobPlan.getIdentifier()).thenReturn(JOB_IDENTIFIER);

        createBuildWorkWithJobPlan(jobPlan);

        try {
            buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
            fail("Should have thrown an assertion error");
        } catch (AssertionError e) {
            assertThat(buildRepository.results.isEmpty(), is(false));
            assertThat(buildRepository.results.get(0), is(JobResult.Failed));
        }
    }

    @Test
    public void shouldUpdateOnlyStatusWhenBuildIsIgnored() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, "pipeline1");
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub(true);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(buildRepository.results.isEmpty(), is(true));
        assertThat(buildRepository.states, containsResult(JobState.Completed));
    }

    @Test
    public void shouldUpdateBothStatusAndResultWhenBuildHasPassed() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, "pipeline1");

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(buildRepository.results, containsResult(JobResult.Passed));
        assertThat(buildRepository.states, containsResult(JobState.Completed));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldReportErrorWhenComandIsNotExistOnLinux() throws Exception {
        buildWork = (BuildWork) getWork(CMD_NOT_EXIST, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(artifactManipulator.consoleOut(), printedAppsMissingInfoOnUnix(SOMETHING_NOT_EXIST));
        assertThat(buildRepository.results, containsResult(Failed));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldReportErrorWhenComandIsNotExistOnWindows() throws Exception {
        buildWork = (BuildWork) getWork(CMD_NOT_EXIST, PIPELINE_NAME);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator,
                environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(artifactManipulator.consoleOut(), printedAppsMissingInfoOnWindows(SOMETHING_NOT_EXIST));
        assertThat(buildRepository.results, containsResult(Failed));
    }

    @Test
    public void shouldReportConsoleout() throws Exception {
        buildWork = (BuildWork) getWork(WILL_FAIL, PIPELINE_NAME);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String consoleOutAsString = artifactManipulator.consoleOut();

        String locator = JOB_IDENTIFIER.buildLocator();
        assertThat(consoleOutAsString, printedPreparingInfo(locator));
        assertThat(consoleOutAsString, printedBuildingInfo(locator));
        assertThat(consoleOutAsString, printedUploadingInfo(locator));
        assertThat(consoleOutAsString, printedBuildFailed());
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void nantTest() throws Exception {
        buildWork = (BuildWork) getWork(NANT, PIPELINE_NAME);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator,
                environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(artifactManipulator.consoleOut(), containsString("Usage : NAnt [options] <target> <target> ..."));
    }

    @Test
    public void rakeTest() throws Exception {
        buildWork = (BuildWork) getWork(RAKE, PIPELINE_NAME);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator,
                environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        assertThat(artifactManipulator.consoleOut(), containsString("rake [-f rakefile] {options} targets..."));
    }

    @Test
    public void doWork_shouldSkipMaterialUpdateWhenFetchMaterialsIsSetToFalse() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME, false, false);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        assertThat(artifactManipulator.consoleOut(), containsString("Start to prepare"));
        assertThat(artifactManipulator.consoleOut(), not(containsString("Start updating")));
        assertThat(artifactManipulator.consoleOut(), containsString("Skipping material update since stage is configured not to fetch materials"));
        assertThat(buildRepository.states.contains(JobState.Preparing), is(true));
    }

    @Test
    public void doWork_shouldUpdateMaterialsWhenFetchMaterialsIsTrue() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME, true, false);
        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        assertThat(artifactManipulator.consoleOut(), containsString("Start to prepare"));
        assertThat(buildRepository.states.contains(JobState.Preparing), is(true));
        assertThat(artifactManipulator.consoleOut(), containsString("Start to update materials"));
    }

    @Test
    public void shouldCreateAgentWorkingDirectoryIfNotExist() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        assertThat(workingdir.exists(), is(false));
        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName);

        buildWork.doWork(agentIdentifier,
                buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        assertThat(artifactManipulator.consoleOut(),
                not(containsString("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory")));

        assertThat(buildRepository.results.contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
        FileUtils.deleteDirectory(workingdir);
    }

    @Test
    public void shouldNotBombWhenCreatingWorkingDirectoryIfCleanWorkingDirectoryFlagIsTrue() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        assertThat(workingdir.exists(), is(false));
        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName, true, true);

        buildWork.doWork(agentIdentifier,
                buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        assertThat(artifactManipulator.consoleOut(),
                not(containsString("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory")));

        assertThat(buildRepository.results.contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
    }

    @Test
    public void shouldCreateAgentWorkingDirectoryIfNotExistWhenFetchMaterialsIsFalse() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        assertThat(workingdir.exists(), is(false));
        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName, false, false);

        buildWork.doWork(agentIdentifier,
                buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        assertThat(artifactManipulator.consoleOut(), not(containsString("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory")));
        assertThat(buildRepository.results.contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
    }

    @Test
    public void shouldCleanAgentWorkingDirectoryIfExistsWhenCleanWorkingDirIsTrue() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        workingdir.mkdirs();
        createDummyFilesAndDirectories(workingdir);
        assertThat(workingdir.listFiles().length, is(2));

        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName, false, true);

        buildWork.doWork(agentIdentifier,
                buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        assertThat(artifactManipulator.consoleOut(), containsString("Cleaning working directory \"" + workingdir.getAbsolutePath()));
        assertThat(buildRepository.results.contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
        assertThat(workingdir.listFiles().length, is(1));
    }

    @Test
    public void shouldReportCurrentWorkingDirectory() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier,
                buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);
        assertThat(artifactManipulator.consoleOut(),
                containsString("[" + SystemUtil.currentWorkingDirectory() + "]"));
    }

    private void createDummyFilesAndDirectories(File workingdir) {
        for (int i = 0; i < 2; i++) {
            File directory = new File(workingdir.getPath() + "/dir" + i);
            directory.mkdir();
            for (int j = 0; j < 10; j++) {
                new File(directory.getPath() + "/file" + i);
            }
        }
    }

    public static Work getWork(String jobXml, String pipelineName) throws Exception {
        return getWork(jobXml, pipelineName, true, false);
    }

    private static Work getWork(String jobXml, String pipelineName, boolean fetchMaterials, boolean cleanWorkingDir) throws Exception {
        CruiseConfig cruiseConfig = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(ConfigFileFixture.withJob(jobXml, pipelineName)))).config;
        JobConfig jobConfig = cruiseConfig.jobConfigByName(pipelineName, STAGE_NAME, JOB_PLAN_NAME, true);

        JobPlan jobPlan = JobInstanceMother.createJobPlan(jobConfig, new JobIdentifier(pipelineName, -2, PIPELINE_LABEL, STAGE_NAME, String.valueOf(STAGE_COUNTER), JOB_PLAN_NAME, 0L),
                new DefaultSchedulingContext());
        jobPlan.setFetchMaterials(fetchMaterials);
        jobPlan.setCleanWorkingDir(cleanWorkingDir);
        final Stage stage = StageMother.custom(STAGE_NAME, new JobInstance(JOB_PLAN_NAME));
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        final Pipeline pipeline = new Pipeline(pipelineName, buildCause, stage);
        pipeline.setLabel(PIPELINE_LABEL);
        List<Builder> builder = builderFactory.buildersForTasks(pipeline, jobConfig.getTasks(), resolver);

        BuildAssignment buildAssignment = BuildAssignment.create(jobPlan,
                BuildCause.createWithEmptyModifications(),
                builder, pipeline.defaultWorkingFolder()
        );
        return new BuildWork(buildAssignment);
    }

    private void createBuildWorkWithJobPlan(JobPlan jobPlan) throws Exception {
        CruiseConfig cruiseConfig = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(ConfigFileFixture.withJob(CMD_NOT_EXIST)))).config;
        JobConfig jobConfig = cruiseConfig.jobConfigByName(PIPELINE_NAME, STAGE_NAME, JOB_PLAN_NAME, true);

        final Stage stage = StageMother.custom(STAGE_NAME, new JobInstance(JOB_PLAN_NAME));
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        final Pipeline pipeline = new Pipeline(PIPELINE_NAME, buildCause, stage);
        List<Builder> builder = builderFactory.buildersForTasks(pipeline, jobConfig.getTasks(), resolver);

        BuildAssignment buildAssignment = BuildAssignment.create(jobPlan,
                BuildCause.createWithEmptyModifications(),
                builder, pipeline.defaultWorkingFolder()
        );
        buildWork = new BuildWork(buildAssignment);
    }

    @Test
    public void shouldReportEnvironmentVariables() throws Exception {
        buildWork = (BuildWork) getWork(WITH_ENV_VAR, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String consoleOut = artifactManipulator.consoleOut();


        assertThat(consoleOut, matches("'GO_SERVER_URL' (to|with) value '" + SERVER_URL));
        assertThat(consoleOut, matches("'GO_PIPELINE_LABEL' (to|with) value '" + PIPELINE_LABEL));
        assertThat(consoleOut, matches("'GO_PIPELINE_NAME' (to|with) value '" + PIPELINE_NAME));
        assertThat(consoleOut, matches("'GO_STAGE_NAME' (to|with) value '" + STAGE_NAME));
        assertThat(consoleOut, matches("'GO_STAGE_COUNTER' (to|with) value '" + STAGE_COUNTER));
        assertThat(consoleOut, matches("'GO_JOB_NAME' (to|with) value '" + JOB_PLAN_NAME));

        assertThat(consoleOut, containsString("[go] setting environment variable 'JOB_ENV' to value 'foobar'"));
        if (isWindows()) {
            assertThat(consoleOut, containsString("[go] overriding environment variable 'Path' with value '/tmp'"));
        } else {
            assertThat(consoleOut, containsString("[go] overriding environment variable 'PATH' with value '/tmp'"));
        }
    }

    @Test
    public void shouldMaskSecretInEnvironmentVarialbeReport() throws Exception {
        buildWork = (BuildWork) getWork(WITH_SECRET_ENV_VAR, PIPELINE_NAME);

        buildWork.doWork(agentIdentifier, buildRepository, artifactManipulator, environmentVariableContext, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), packageAsRepositoryExtension, scmExtension, taskExtension);

        String consoleOut = artifactManipulator.consoleOut();
        assertThat(consoleOut, containsString("[go] setting environment variable 'foo' to value 'foo(******)'"));
        assertThat(consoleOut, containsString("[go] setting environment variable 'bar' to value '********'"));
        assertThat(consoleOut, not(containsString("i am a secret")));
    }

    @Test
    public void encodeAndDecodeBuildWorkAsMessageData() throws Exception {
        Work original = getWork(WILL_FAIL, PIPELINE_NAME);
        Work clone = MessageEncoding.decodeWork(MessageEncoding.encodeWork(original));
        assertThat(clone, is(original));
    }
}
