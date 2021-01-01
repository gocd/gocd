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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.server.service.builders.*;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.domain.JobState.*;
import static com.thoughtworks.go.helper.ConfigFileFixture.withJob;
import static com.thoughtworks.go.matchers.ConsoleOutMatcherJunit5.assertConsoleOut;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class BuildWorkTest {

    static final String PIPELINE_NAME = "pipeline1";
    private static final String PIPELINE_LABEL = "100";
    private static final String STAGE_NAME = "mingle";
    private static final String JOB_PLAN_NAME = "run-ant";
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
            + "    <variable name=\"" + (SystemUtils.IS_OS_WINDOWS ? "Path" : "PATH") + "\">\n"
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

    private static final String WITH_GO_SERVER_URL_ENV_VAR = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <environmentvariables>\n"
            + "    <variable name=\"GO_SERVER_URL\">\n"
            + "      <value>go_server_url_from_job</value>\n"
            + "    </variable>\n"
            + "  </environmentvariables>\n"
            + "  <tasks>\n"
            + "    <ant target=\"-help\" />\n"
            + "  </tasks>\n"
            + "</job>";

    private EnvironmentVariableContext environmentVariableContext;
    private com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub buildRepository;
    private GoArtifactsManipulatorStub artifactManipulator;
    private static BuilderFactory builderFactory = new BuilderFactory(new AntTaskBuilder(), new ExecTaskBuilder(), new NantTaskBuilder(),
            new RakeTaskBuilder(), new PluggableTaskBuilderCreator(), new KillAllChildProcessTaskBuilder(),
            new FetchTaskBuilder(mock(GoConfigService.class)), new NullTaskBuilder());
    @Mock
    private static UpstreamPipelineResolver resolver;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Mock
    private PluginRequestProcessorRegistry pluginRequestProcessorRegistry;

    private static String willUpload(String file) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <artifact type=\"build\" src=\"something-not-there.txt\" dest=\"dist\" />\n"
                + "      <artifact type=\"build\" src=\"" + file + "\" dest=\"dist\\test\" />\n"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <ant target=\"-help\" />\n"
                + "  </tasks>\n"
                + "</job>";

    }

    private static String pluggableArtifact() {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <artifact type=\"external\" id=\"installers\" storeId=\"s3\">\n"
                + "      <configuration>"
                + "       <property>\n"
                + "         <key>FileName</key>\n"
                + "         <value>build/lib/foo.jar</value>\n"
                + "         </property>\n"
                + "      </configuration>"
                + "      </artifact>"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <ant target=\"-help\" />\n"
                + "  </tasks>\n"
                + "</job>";

    }

    private static String willUploadToDest(String file, String dest) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <artifact type= \"build\" src=\"" + file + "\" dest=\"" + dest + "\" />\n"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <ant target=\"-help\" />\n"
                + "  </tasks>\n"
                + "</job>";

    }

    private static final int STAGE_COUNTER = 100;
    private static final String SERVER_URL = "somewhere-does-not-matter";
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier(PIPELINE_NAME, -3, PIPELINE_LABEL, STAGE_NAME, String.valueOf(STAGE_COUNTER), JOB_PLAN_NAME, 1L);

    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
        environmentVariableContext = new EnvironmentVariableContext();
        artifactManipulator = new GoArtifactsManipulatorStub();
        new SystemEnvironment().setProperty("serviceUrl", SERVER_URL);
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub();
    }

    @AfterEach
    void tearDown() {
        new SystemEnvironment().clearProperty("serviceUrl");
        verifyNoMoreInteractions(resolver);
    }

    @Test
    void shouldReportStatus() throws Exception {
        buildWork = (BuildWork) getWork(WILL_FAIL, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.states).contains(Preparing, Building, Completing);
    }

    @Test
    void shouldNotRunTaskWhichConditionDoesNotMatch() throws Exception {
        buildWork = (BuildWork) getWork(WILL_NOT_RUN, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();
        assertThat(actual).doesNotContain("run when status is failed");
    }

    @Test
    void shouldRunTaskWhenConditionMatches() throws Exception {
        buildWork = (BuildWork) getWork(MULTIPLE_RUN_IFS, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();
        assertThat(actual).contains("[go] Task: echo run when status is failed, passed or cancelled");
        assertConsoleOut(actual).matchUsingRegex("\\[go] Task status: passed \\(\\d+ ms\\)");
        assertThat(actual).contains("[go] Current job status: passed");
        assertThat(actual).contains("run when status is failed, passed or cancelled");
    }

    @Test
    void shouldRunTasksBasedOnConditions() throws Exception {
        buildWork = (BuildWork) getWork(MULTIPLE_TASKS, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();

        assertConsoleOut(actual).contains("run when status is failed")
                .printedExcRunIfInfo("command-not-found", "failed")
                .contains("run when status is any")
                .printedExcRunIfInfo("echo", "run when status is any", "failed")
                .doesNotContain("run when status is passed")
                .doesNotContain("run when status is cancelled")
                .doesNotContainExcRunIfInfo("echo", "run when status is passed", "failed")
                .doesNotContainExcRunIfInfo("echo", "run when status is cancelled", "failed");
    }

    @Test
    void shouldReportBuildIsFailedWhenAntBuildFailed() throws Exception {
        buildWork = (BuildWork) getWork(WILL_FAIL, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results).contains(Failed);
    }

    @Test
    void shouldReportDirectoryNotExists() throws Exception {
        buildWork = (BuildWork) getWork(NANT_WITH_WORKINGDIR, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(artifactManipulator.consoleOut()).contains("not-exists\" is not a directory!");
    }

    @Test
    void shouldReportUploadMessageWhenUpload() throws Exception {
        String destFolder = "dest\\test\\sub-folder";
        final String url = String.format("%s/remoting/files/%s/%s/%s/%s/%s/%s?attempt=1&buildId=0", SERVER_URL, PIPELINE_NAME, PIPELINE_LABEL, STAGE_NAME, STAGE_COUNTER, JOB_PLAN_NAME,
                destFolder.replaceAll("\\\\", "/"));

        String artifactFile = "example.txt";
        File basedir = new File("pipelines/pipeline1");

        FileUtils.write(new File(basedir, artifactFile), "foo", StandardCharsets.UTF_8);

        buildWork = (BuildWork) getWork(willUploadToDest(artifactFile, destFolder), PIPELINE_NAME);
        com.thoughtworks.go.remote.work.HttpServiceStub httpService = new com.thoughtworks.go.remote.work.HttpServiceStub(HttpServletResponse.SC_OK);
        artifactManipulator = new GoArtifactsManipulatorStub(httpService);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();

        assertThat(actual.toLowerCase()).contains(("Uploading artifacts from " + new File(basedir, artifactFile).getCanonicalPath()).toLowerCase());

        Map<String, File> uploadedFiles = httpService.getUploadedFiles();

        assertThat(uploadedFiles.size()).isEqualTo(1);
        assertThat(uploadedFiles.get(url).getAbsolutePath()).contains(artifactFile + ".zip");
    }

    @Test
    void shouldFailTheJobWhenFailedToUploadArtifact() throws Exception {
        String artifactFile = "some.txt";
        buildWork = (BuildWork) getWork(willUpload(artifactFile), PIPELINE_NAME);
        artifactManipulator = new GoArtifactsManipulatorStub(new HttpServiceStub(SC_NOT_ACCEPTABLE));

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();

        assertConsoleOut(actual).printedRuleDoesNotMatchFailure(new File("pipelines/pipeline1").getPath(), artifactFile);
        assertThat(buildRepository.results).contains(Failed);
    }

    @Test
    void shouldCallArtifactExtensionToPublishPluggableArtifact() throws Exception {
        final ArtifactExtension artifactExtension = mock(ArtifactExtension.class);
        buildWork = (BuildWork) getWork(pluggableArtifact(), PIPELINE_NAME);
        artifactManipulator = new GoArtifactsManipulatorStub(new HttpServiceStub(SC_NOT_ACCEPTABLE));

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, artifactExtension, pluginRequestProcessorRegistry));

        verify(artifactExtension).publishArtifact(anyString(), any(ArtifactPlan.class), any(ArtifactStore.class), anyString(), any(EnvironmentVariableContext.class));
    }

    @Test
    void shouldReportBuildIsFailedWhenAntBuildPassed() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results).contains(Passed);
    }

    @Test
    void shouldSendAResultStatusToServerWhenAThrowableErrorIsThrown() {
        BuildAssignment buildAssignment = mock(BuildAssignment.class);
        when(buildAssignment.shouldFetchMaterials()).thenThrow(new AssertionError());
        when(buildAssignment.initialEnvironmentVariableContext()).thenReturn(new EnvironmentVariableContext());
        when(buildAssignment.getWorkingDirectory()).thenReturn(new File("current"));
        when(buildAssignment.getJobIdentifier()).thenReturn(JOB_IDENTIFIER);

        buildWork = new BuildWork(buildAssignment, "utf-8");

        try {
            buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
            fail("Should have thrown an assertion error");
        } catch (AssertionError e) {
            assertThat(buildRepository.results.isEmpty()).isTrue();
            assertThat(buildRepository.states.size()).isEqualTo(1);
            assertThat(buildRepository.states.get(0)).isEqualTo(JobState.Preparing);
        }
    }

    @Test
    void shouldSendAResultStatusToServerWhenAnExceptionIsThrown() throws Exception {
        BuildAssignment buildAssignment = mock(BuildAssignment.class);
        when(buildAssignment.shouldFetchMaterials()).thenThrow(new RuntimeException());
        when(buildAssignment.initialEnvironmentVariableContext()).thenReturn(new EnvironmentVariableContext());
        when(buildAssignment.getWorkingDirectory()).thenReturn(new File("current"));
        when(buildAssignment.getJobIdentifier()).thenReturn(JOB_IDENTIFIER);

        buildWork = new BuildWork(buildAssignment, "utf-8");

        try {
            buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
            fail("Should have thrown an assertion error");
        } catch (AssertionError e) {
            assertThat(buildRepository.results.isEmpty()).isFalse();
            assertThat(buildRepository.results.get(0)).isEqualTo(JobResult.Failed);
        }
    }

    @Test
    void shouldUpdateOnlyStatusWhenBuildIsIgnored() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, "pipeline1");
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub(true);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results.isEmpty()).isTrue();
        assertThat(buildRepository.states).contains(JobState.Completed);
    }

    @Test
    void shouldUpdateBothStatusAndResultWhenBuildHasPassed() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, "pipeline1");

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results).contains(JobResult.Passed);
        assertThat(buildRepository.states).contains(JobState.Completed);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldReportErrorWhenComandIsNotExistOnLinux() throws Exception {
        buildWork = (BuildWork) getWork(CMD_NOT_EXIST, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertConsoleOut(artifactManipulator.consoleOut()).printedAppsMissingInfoOnUnix(SOMETHING_NOT_EXIST);
        assertThat(buildRepository.results).contains(Failed);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldReportErrorWhenComandIsNotExistOnWindows() throws Exception {
        buildWork = (BuildWork) getWork(CMD_NOT_EXIST, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertConsoleOut(artifactManipulator.consoleOut()).printedAppsMissingInfoOnWindows(SOMETHING_NOT_EXIST);
        assertThat(buildRepository.results).contains(Failed);
    }

    @Test
    void shouldReportConsoleOut() throws Exception {
        buildWork = (BuildWork) getWork(WILL_FAIL, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String consoleOutAsString = artifactManipulator.consoleOut();

        String locator = JOB_IDENTIFIER.buildLocator();
        assertConsoleOut(consoleOutAsString).printedPreparingInfo(locator)
                .printedBuildingInfo(locator)
                .printedUploadingInfo(locator)
                .printedBuildFailed();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void nantTest() throws Exception {
        buildWork = (BuildWork) getWork(NANT, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(artifactManipulator.consoleOut()).contains("Usage : NAnt [options] <target> <target> ...");
    }

    @Test
    void rakeTest() throws Exception {
        buildWork = (BuildWork) getWork(RAKE, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(artifactManipulator.consoleOut()).contains("rake [-f rakefile] {options} targets...");
    }

    @Test
    void doWork_shouldSkipMaterialUpdateWhenFetchMaterialsIsSetToFalse() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME, false, false);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).contains("Start to prepare");
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Start updating");
        assertThat(artifactManipulator.consoleOut()).contains("Skipping material update since stage is configured not to fetch materials");
        assertThat(buildRepository.states.contains(JobState.Preparing)).isTrue();
    }

    @Test
    void doWork_shouldUpdateMaterialsWhenFetchMaterialsIsTrue() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME, true, false);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).contains("Start to prepare");
        assertThat(buildRepository.states.contains(JobState.Preparing)).isTrue();
        assertThat(artifactManipulator.consoleOut()).contains("Start to update materials");
    }

    @Test
    void shouldCreateAgentWorkingDirectoryIfNotExist() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        assertThat(workingdir.exists()).isFalse();
        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory");

        assertThat(buildRepository.results.contains(Passed)).isTrue();
        assertThat(workingdir.exists()).isTrue();
        FileUtils.deleteDirectory(workingdir);
    }

    @Test
    void shouldNotBombWhenCreatingWorkingDirectoryIfCleanWorkingDirectoryFlagIsTrue() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        assertThat(workingdir.exists()).isFalse();
        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName, true, true);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory");

        assertThat(buildRepository.results.contains(Passed)).isTrue();
        assertThat(workingdir.exists()).isTrue();
    }

    @Test
    void shouldCreateAgentWorkingDirectoryIfNotExistWhenFetchMaterialsIsFalse() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        assertThat(workingdir.exists()).isFalse();
        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName, false, false);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Working directory \"" + workingdir.getAbsolutePath() + "\" is noa directory");
        assertThat(buildRepository.results.contains(Passed)).isTrue();
        assertThat(workingdir.exists()).isTrue();
    }

    @Test
    void shouldCleanAgentWorkingDirectoryIfExistsWhenCleanWorkingDirIsTrue() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingdir = new File("pipelines/" + pipelineName);
        if (workingdir.exists()) {
            FileUtils.deleteDirectory(workingdir);
        }
        workingdir.mkdirs();
        createDummyFilesAndDirectories(workingdir);
        assertThat(workingdir.listFiles().length).isEqualTo(2);

        buildWork = (BuildWork) getWork(WILL_PASS, pipelineName, false, true);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).contains("Cleaning working directory \"" + workingdir.getAbsolutePath());
        assertThat(buildRepository.results.contains(Passed)).isTrue();
        assertThat(workingdir.exists()).isTrue();
        assertThat(workingdir.listFiles().length).isEqualTo(0);
    }

    @Test
    void shouldReportCurrentWorkingDirectory() throws Exception {
        buildWork = (BuildWork) getWork(WILL_PASS, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).contains("[" + SystemUtil.currentWorkingDirectory() + "]");
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

    static Work getWork(String jobXml, String pipelineName) throws Exception {
        return getWork(jobXml, pipelineName, true, false);
    }

    private static Work getWork(String jobXml, String pipelineName, boolean fetchMaterials, boolean cleanWorkingDir) throws Exception {
        CruiseConfig cruiseConfig = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).loadConfigHolder(withJob(jobXml, pipelineName)).config;
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
                builder, pipeline.defaultWorkingFolder(),
                null, cruiseConfig.getArtifactStores());

        return new BuildWork(buildAssignment, "utf-8");
    }

    @Test
    void shouldReportEnvironmentVariables() throws Exception {
        buildWork = (BuildWork) getWork(WITH_ENV_VAR, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String consoleOut = artifactManipulator.consoleOut();


        assertConsoleOut(consoleOut).matchUsingRegex("'GO_SERVER_URL' (to|with) value '" + SERVER_URL);
        assertConsoleOut(consoleOut).matchUsingRegex("'GO_PIPELINE_LABEL' (to|with) value '" + PIPELINE_LABEL);
        assertConsoleOut(consoleOut).matchUsingRegex("'GO_PIPELINE_NAME' (to|with) value '" + PIPELINE_NAME);
        assertConsoleOut(consoleOut).matchUsingRegex("'GO_STAGE_NAME' (to|with) value '" + STAGE_NAME);
        assertConsoleOut(consoleOut).matchUsingRegex("'GO_STAGE_COUNTER' (to|with) value '" + STAGE_COUNTER);
        assertConsoleOut(consoleOut).matchUsingRegex("'GO_JOB_NAME' (to|with) value '" + JOB_PLAN_NAME);

        assertThat(consoleOut).contains("[go] setting environment variable 'JOB_ENV' to value 'foobar'");
        if (SystemUtils.IS_OS_WINDOWS) {
            assertThat(consoleOut).contains("[go] overriding environment variable 'Path' with value '/tmp'");
        } else {
            assertThat(consoleOut).contains("[go] overriding environment variable 'PATH' with value '/tmp'");
        }
    }

    @Test
    void shouldOverrideAgentGO_SERVER_URL_EnvironmentVariableIfDefinedInJob() throws Exception {
        buildWork = (BuildWork) getWork(WITH_GO_SERVER_URL_ENV_VAR, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String consoleOut = artifactManipulator.consoleOut();

        assertConsoleOut(consoleOut).matchUsingRegex("'GO_SERVER_URL' (to|with) value '" + SERVER_URL);
        assertThat(consoleOut).contains("[go] overriding environment variable 'GO_SERVER_URL' with value 'go_server_url_from_job'");
    }

    @Test
    void shouldMaskSecretInEnvironmentVarialbeReport() throws Exception {
        buildWork = (BuildWork) getWork(WITH_SECRET_ENV_VAR, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String consoleOut = artifactManipulator.consoleOut();
        assertThat(consoleOut).contains("[go] setting environment variable 'foo' to value 'foo(******)'");
        assertThat(consoleOut).contains("[go] setting environment variable 'bar' to value '********'");
        assertThat(consoleOut).doesNotContain("i am a secret");
    }

}
