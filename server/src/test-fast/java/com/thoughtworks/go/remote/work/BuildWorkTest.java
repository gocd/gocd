/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.domain.JobState.*;
import static com.thoughtworks.go.helper.ConfigFileFixture.withJob;
import static com.thoughtworks.go.matchers.ConsoleOutMatcherJunit5.assertConsoleOut;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildWorkTest {

    static final String PIPELINE_NAME = "pipeline1";
    private static final String PIPELINE_LABEL = "100";
    private static final String STAGE_NAME = "mingle";
    private static final int STAGE_COUNTER = 100;
    private static final String JOB_PLAN_NAME = "run-ant";

    private static final String SERVER_URL = "somewhere-does-not-matter";
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier(PIPELINE_NAME, -3, PIPELINE_LABEL, STAGE_NAME, String.valueOf(STAGE_COUNTER), JOB_PLAN_NAME, 1L);

    private static final String NANT = ("""
            <job name="%s">
              <tasks>
                <nant target="-help"/>
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String NANT_WITH_WORKING_DIR = ("""
            <job name="%s">
              <tasks>
                <nant target="-help" workingdir="not-exists" />
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String RAKE = ("""
            <job name="%s">
              <tasks>
                <rake target="--help"/>
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String WILL_FAIL = ("""
            <job name="%s">
              <tasks>
                <ant target="something-not-really-exist" />
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String WILL_PASS = ("""
            <job name="%s">
              <tasks>
                <ant target="-help" />
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String WITH_ENV_VAR = ("""
            <job name="%s">
              <environmentvariables>
                <variable name="JOB_ENV">
                  <value>foobar</value>
                </variable>
                <variable name="%s">
                  <value>/tmp</value>
                </variable>
              </environmentvariables>
              <tasks>
                <ant target="-help" />
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME, SystemUtils.IS_OS_WINDOWS ? "Path" : "PATH");

    private static final String WITH_SECRET_ENV_VAR = ("""
            <job name="%s">
              <environmentvariables>
                <variable name="foo">
                  <value>foo(i am a secret)</value>
                </variable>
                <variable name="bar" secure="true">
                  <value>i am a secret</value>
                </variable>
              </environmentvariables>
              <tasks>
                <ant target="-help" />
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String SOMETHING_NOT_EXIST = "something-not-exist";

    private static final String CMD_NOT_EXIST = ("""
            <job name="%s">
              <tasks>
                <exec command="%s" />
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME, SOMETHING_NOT_EXIST);

    private static final String WILL_NOT_RUN = ("""
            <job name="%s">
              <tasks>
                <exec command="echo" args="run when status is failed">
                  <runif status="failed" />
                </exec>
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String MULTIPLE_TASKS = ("""
            <job name="%s">
              <tasks>
                <exec command="command-not-found" >
                </exec>
                <exec command="echo" args="run when status is failed">
                  <runif status="failed" />
                </exec>
                <exec command="echo" args="run when status is passed">
                  <runif status="passed" />
                </exec>
                <exec command="echo" args="run when status is any">
                  <runif status="any" />
                </exec>
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String MULTIPLE_RUN_IFS = ("""
            <job name="%s">
              <tasks>
                <exec command="echo" args="run when status is failed, passed or cancelled">
                  <runif status="failed" />
                  <runif status="passed" />
                </exec>
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final String WITH_GO_SERVER_URL_ENV_VAR = ("""
            <job name="%s">
              <environmentvariables>
                <variable name="GO_SERVER_URL">
                  <value>go_server_url_from_job</value>
                </variable>
              </environmentvariables>
              <tasks>
                <ant target="-help" />
              </tasks>
            </job>""").formatted(JOB_PLAN_NAME);

    private static final BuilderFactory builderFactory = new BuilderFactory(new AntTaskBuilder(), new ExecTaskBuilder(), new NantTaskBuilder(),
        new RakeTaskBuilder(), new PluggableTaskBuilderCreator(), new KillAllChildProcessTaskBuilder(),
        new FetchTaskBuilder(mock(GoConfigService.class)), new NullTaskBuilder());
    private static String willUpload(String file) {
        return ("""
                <job name="%s">
                   <artifacts>
                      <artifact type="build" src="something-not-there.txt" dest="dist" />
                      <artifact type="build" src="%s" dest="dist\\test" />
                   </artifacts>
                  <tasks>
                    <ant target="-help" />
                  </tasks>
                </job>""").formatted(JOB_PLAN_NAME, file);

    }

    private static String pluggableArtifact() {
        return ("""
                <job name="%s">
                   <artifacts>
                      <artifact type="external" id="installers" storeId="s3">
                      <configuration>
                       <property>
                         <key>FileName</key>
                         <value>build/lib/foo.jar</value>
                         </property>
                      </configuration>
                      </artifact>
                   </artifacts>
                  <tasks>
                    <ant target="-help" />
                  </tasks>
                </job>""").formatted(JOB_PLAN_NAME);

    }

    private static String willUploadToDest(String file, String dest) {
        return ("""
                <job name="%s">
                   <artifacts>
                      <artifact type= "build" src="%s" dest="%s" />
                   </artifacts>
                  <tasks>
                    <ant target="-help" />
                  </tasks>
                </job>""").formatted(JOB_PLAN_NAME, file, dest);

    }

    private BuildWork buildWork;
    private AgentIdentifier agentIdentifier;
    private EnvironmentVariableContext environmentVariableContext;
    private com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub buildRepository;
    private GoArtifactsManipulatorStub artifactManipulator;

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


    @BeforeEach
    void setUp() {
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
        environmentVariableContext = new EnvironmentVariableContext();
        artifactManipulator = new GoArtifactsManipulatorStub();
        new SystemEnvironment().setProperty(SystemEnvironment.SERVICE_URL, SERVER_URL);
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub();
    }

    @AfterEach
    void tearDown() {
        new SystemEnvironment().clearProperty("serviceUrl");
        verifyNoMoreInteractions(resolver);
    }

    @Test
    void shouldReportStatus() throws Exception {
        buildWork = getWork(WILL_FAIL, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.states).contains(Preparing, Building, Completing);
    }

    @Test
    void shouldNotRunTaskWhichConditionDoesNotMatch() throws Exception {
        buildWork = getWork(WILL_NOT_RUN, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();
        assertThat(actual).doesNotContain("run when status is failed");
    }

    @Test
    void shouldRunTaskWhenConditionMatches() throws Exception {
        buildWork = getWork(MULTIPLE_RUN_IFS, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();
        assertThat(actual).contains("[go] Task: echo run when status is failed, passed or cancelled");
        assertConsoleOut(actual).matchUsingRegex("\\[go] Task status: passed \\(\\d+ ms\\)");
        assertThat(actual).contains("[go] Current job status: passed");
        assertThat(actual).contains("run when status is failed, passed or cancelled");
    }

    @Test
    void shouldRunTasksBasedOnConditions() throws Exception {
        buildWork = getWork(MULTIPLE_TASKS, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String actual = artifactManipulator.consoleOut();

        assertConsoleOut(actual).contains("run when status is failed")
                .printedExcRunIfInfo("command-not-found", "failed")
                .contains("run when status is any")
                .printedExcRunIfInfo("echo", "run when status is any", "failed")
                .doesNotContain("run when status is passed")
                .doesNotContain("run when status is cancelled")
                .doesNotContainExcRunIfInfo("echo", "run when status is passed")
                .doesNotContainExcRunIfInfo("echo", "run when status is cancelled");
    }

    @Test
    void shouldReportBuildIsFailedWhenAntBuildFailed() throws Exception {
        buildWork = getWork(WILL_FAIL, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results).contains(Failed);
    }

    @Test
    void shouldReportDirectoryNotExists() throws Exception {
        buildWork = getWork(NANT_WITH_WORKING_DIR, PIPELINE_NAME);

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

        FileUtils.write(new File(basedir, artifactFile), "foo", UTF_8);

        buildWork = getWork(willUploadToDest(artifactFile, destFolder), PIPELINE_NAME);
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
        buildWork = getWork(willUpload(artifactFile), PIPELINE_NAME);
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
        buildWork = getWork(pluggableArtifact(), PIPELINE_NAME);
        artifactManipulator = new GoArtifactsManipulatorStub(new HttpServiceStub(SC_NOT_ACCEPTABLE));

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, artifactExtension, pluginRequestProcessorRegistry));

        verify(artifactExtension).publishArtifact(anyString(), any(ArtifactPlan.class), any(ArtifactStore.class), anyString(), any(EnvironmentVariableContext.class));
    }

    @Test
    void shouldReportBuildIsFailedWhenAntBuildPassed() throws Exception {
        buildWork = getWork(WILL_PASS, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results).contains(Passed);
    }

    @Test
    void shouldSendAResultStatusToServerWhenAThrowableErrorIsThrown() {
        BuildAssignment buildAssignment = mock(BuildAssignment.class);
        when(buildAssignment.shouldFetchMaterials()).thenThrow(new AssertionError());
        when(buildAssignment.getWorkingDirectory()).thenReturn(new File("current"));
        when(buildAssignment.getJobIdentifier()).thenReturn(JOB_IDENTIFIER);

        buildWork = new BuildWork(buildAssignment, UTF_8);

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
    void shouldSendAResultStatusToServerWhenAnExceptionIsThrown() {
        BuildAssignment buildAssignment = mock(BuildAssignment.class);
        when(buildAssignment.shouldFetchMaterials()).thenThrow(new RuntimeException());
        when(buildAssignment.getWorkingDirectory()).thenReturn(new File("current"));
        when(buildAssignment.getJobIdentifier()).thenReturn(JOB_IDENTIFIER);

        buildWork = new BuildWork(buildAssignment, UTF_8);

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
        buildWork = getWork(WILL_PASS, "pipeline1");
        buildRepository = new com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub(true);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results.isEmpty()).isTrue();
        assertThat(buildRepository.states).contains(JobState.Completed);
    }

    @Test
    void shouldUpdateBothStatusAndResultWhenBuildHasPassed() throws Exception {
        buildWork = getWork(WILL_PASS, "pipeline1");

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(buildRepository.results).contains(JobResult.Passed);
        assertThat(buildRepository.states).contains(JobState.Completed);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldReportErrorWhenCommandDoesNotExistOnLinux() throws Exception {
        buildWork = getWork(CMD_NOT_EXIST, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator,
                new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertConsoleOut(artifactManipulator.consoleOut()).printedAppsMissingInfoOnUnix(SOMETHING_NOT_EXIST);
        assertThat(buildRepository.results).contains(Failed);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldReportErrorWhenCommandDoesNotExistOnWindows() throws Exception {
        buildWork = getWork(CMD_NOT_EXIST, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertConsoleOut(artifactManipulator.consoleOut()).printedAppsMissingInfoOnWindows(SOMETHING_NOT_EXIST);
        assertThat(buildRepository.results).contains(Failed);
    }

    @Test
    void shouldReportConsoleOut() throws Exception {
        buildWork = getWork(WILL_FAIL, PIPELINE_NAME);
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
        buildWork = getWork(NANT, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(artifactManipulator.consoleOut()).contains("Usage : NAnt [options] <target> <target> ...");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void rakeTest() throws Exception {
        buildWork = getWork(RAKE, PIPELINE_NAME);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        assertThat(artifactManipulator.consoleOut()).contains("rake [-f rakefile] {options} targets...");
    }

    @Test
    void doWork_shouldSkipMaterialUpdateWhenFetchMaterialsIsSetToFalse() throws Exception {
        buildWork = getWork(WILL_PASS, PIPELINE_NAME, false, false);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).contains("Start to prepare");
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Start updating");
        assertThat(artifactManipulator.consoleOut()).contains("Skipping material update since stage is configured not to fetch materials");
        assertThat(buildRepository.states.contains(JobState.Preparing)).isTrue();
    }

    @Test
    void doWork_shouldUpdateMaterialsWhenFetchMaterialsIsTrue() throws Exception {
        buildWork = getWork(WILL_PASS, PIPELINE_NAME, true, false);
        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).contains("Start to prepare");
        assertThat(buildRepository.states.contains(JobState.Preparing)).isTrue();
        assertThat(artifactManipulator.consoleOut()).contains("Start to update materials");
    }

    @Test
    void shouldCreateAgentWorkingDirectoryIfNotExist() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingDir = new File("pipelines/" + pipelineName);
        if (workingDir.exists()) {
            FileUtils.deleteDirectory(workingDir);
        }
        assertThat(workingDir).doesNotExist();
        buildWork = getWork(WILL_PASS, pipelineName);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Working directory \"" + workingDir.getAbsolutePath() + "\" is not a directory");

        assertThat(buildRepository.results.contains(Passed)).isTrue();
        assertThat(workingDir).exists();
        FileUtils.deleteDirectory(workingDir);
    }

    @Test
    void shouldNotBombWhenCreatingWorkingDirectoryIfCleanWorkingDirectoryFlagIsTrue() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingDir = new File("pipelines/" + pipelineName);
        if (workingDir.exists()) {
            FileUtils.deleteDirectory(workingDir);
        }
        assertThat(workingDir).doesNotExist();
        buildWork = getWork(WILL_PASS, pipelineName, true, true);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Working directory \"" + workingDir.getAbsolutePath() + "\" is not a directory");

        assertThat(buildRepository.results.contains(Passed)).isTrue();
        assertThat(workingDir).exists();
    }

    @Test
    void shouldCreateAgentWorkingDirectoryIfNotExistWhenFetchMaterialsIsFalse() throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingDir = new File("pipelines/" + pipelineName);
        if (workingDir.exists()) {
            FileUtils.deleteDirectory(workingDir);
        }
        assertThat(workingDir).doesNotExist();
        buildWork = getWork(WILL_PASS, pipelineName, false, false);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).doesNotContain("Working directory \"" + workingDir.getAbsolutePath() + "\" is noa directory");
        assertThat(buildRepository.results.contains(Passed)).isTrue();
        assertThat(workingDir).exists();
    }

    @Test
    void shouldCleanAgentWorkingDirectoryIfExistsWhenCleanWorkingDirIsTrue() throws Exception {
        doCleanWorkingDirTest(workingDir -> {
            createDummyFilesAndDirectories(workingDir);
            assertThat(workingDir.toFile().listFiles()).hasSize(2);
        });
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldCleanAgentWorkingDirectoryWithSymlinksIfExistsWhenCleanWorkingDirIsTrue() throws Exception {
        doCleanWorkingDirTest(workingDir -> {
            createDummyFilesAndDirectories(workingDir);
            createSymlinkedDirs(workingDir);
            assertThat(workingDir.toFile().listFiles()).hasSize(4);
        });
    }

    private void doCleanWorkingDirTest(Consumer<Path> workingDirContentCreator) throws Exception {
        String pipelineName = "pipeline" + UUID.randomUUID();
        File workingDir = new File("pipelines/" + pipelineName);
        if (workingDir.exists()) {
            FileUtils.deleteDirectory(workingDir);
        }
        workingDir.mkdirs();
        workingDirContentCreator.accept(workingDir.toPath());

        buildWork = getWork(WILL_PASS, pipelineName, false, true);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertSoftly(softly -> {
            softly.assertThat(artifactManipulator.consoleOut()).contains("Cleaning working directory \"" + workingDir.getAbsolutePath());
            softly.assertThat(buildRepository.results).containsOnly(Passed);
            softly.assertThat(workingDir).exists();
            softly.assertThat(workingDir.listFiles()).isEmpty();
        });
    }

    private void createSymlinkedDirs(Path workingDir) {
        // |-- link-to-secondlevel -> firstlevel/link-to-secondlevel
        // `-- firstlevel
        //     |-- secondlevel
        //     |   `-- foo.txt
        //     `-- link-to-secondlevel -> secondlevel

        Path firstLevel = workingDir.resolve("firstlevel");
        Path secondLevel = firstLevel.resolve("secondlevel");
        Path targetFile = secondLevel.resolve("foo.txt");

        Path nestedLinkToSecond = firstLevel.resolve("link-to-secondlevel");
        Path linkToSecond = workingDir.resolve("link-to-secondlevel");
        try {
            Files.createDirectories(secondLevel);
            Files.createFile(targetFile);
            Files.createSymbolicLink(nestedLinkToSecond, Paths.get("secondLevel"));
            Files.createSymbolicLink(linkToSecond, Paths.get("firstlevel", "link-to-secondLevel"));
            assertThat(firstLevel.toFile().listFiles()).hasSize(2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReportCurrentWorkingDirectory() throws Exception {
        buildWork = getWork(WILL_PASS, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier,
                buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));
        assertThat(artifactManipulator.consoleOut()).contains("[" + SystemUtil.currentWorkingDirectory() + "]");
    }

    private void createDummyFilesAndDirectories(Path workingDir) {
        for (int i = 0; i < 2; i++) {
            File directory = new File(workingDir.toString() + "/dir" + i);
            directory.mkdir();
            for (int j = 0; j < 10; j++) {
                new File(directory.getPath() + "/file" + i);
            }
        }
    }

    static BuildWork getWork(String jobXml, String pipelineName) throws Exception {
        return getWork(jobXml, pipelineName, true, false);
    }

    private static BuildWork getWork(String jobXml, String pipelineName, boolean fetchMaterials, boolean cleanWorkingDir) throws Exception {
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

        return new BuildWork(buildAssignment, UTF_8);
    }

    @Test
    void shouldReportEnvironmentVariables() throws Exception {
        buildWork = getWork(WITH_ENV_VAR, PIPELINE_NAME);

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
        buildWork = getWork(WITH_GO_SERVER_URL_ENV_VAR, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String consoleOut = artifactManipulator.consoleOut();

        assertConsoleOut(consoleOut).matchUsingRegex("'GO_SERVER_URL' (to|with) value '" + SERVER_URL);
        assertThat(consoleOut).contains("[go] overriding environment variable 'GO_SERVER_URL' with value 'go_server_url_from_job'");
    }

    @Test
    void shouldMaskSecretInEnvironmentVariableReport() throws Exception {
        buildWork = getWork(WITH_SECRET_ENV_VAR, PIPELINE_NAME);

        buildWork.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"), packageRepositoryExtension, scmExtension, taskExtension, null, pluginRequestProcessorRegistry));

        String consoleOut = artifactManipulator.consoleOut();
        assertThat(consoleOut).contains("[go] setting environment variable 'foo' to value 'foo(******)'");
        assertThat(consoleOut).contains("[go] setting environment variable 'bar' to value '********'");
        assertThat(consoleOut).doesNotContain("i am a secret");
    }

}
