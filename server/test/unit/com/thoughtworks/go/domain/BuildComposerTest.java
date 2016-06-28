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
package com.thoughtworks.go.domain;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.domain.BuildComposer;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.server.service.builders.*;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.utils.Timeout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.getLast;
import static com.thoughtworks.go.domain.JobResult.*;
import static com.thoughtworks.go.domain.JobState.*;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.matchers.ConsoleOutMatcher.*;
import static com.thoughtworks.go.matchers.RegexMatcher.matches;
import static com.thoughtworks.go.util.SystemUtil.isWindows;
import static com.thoughtworks.go.util.TestUtils.copyAndClose;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(JunitExtRunner.class)
public class BuildComposerTest extends BuildSessionBasedTestCase {
    public static final String PIPELINE_NAME = "pipeline1";
    public static final String PIPELINE_LABEL = "100";
    public static final String STAGE_NAME = "mingle";
    public static final String JOB_PLAN_NAME = "run-ant";
    private static final int STAGE_COUNTER = 100;
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier(PIPELINE_NAME, -3, PIPELINE_LABEL, STAGE_NAME, String.valueOf(STAGE_COUNTER), JOB_PLAN_NAME, 1L);
    private static final String SERVER_URL = "somewhere-does-not-matter";

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
            + "    <exec command=\"echo\">\n"
            + "      <arg>hello world</arg>\n"
            + "    </exec>\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String WITH_ENV_VAR = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <environmentvariables>\n"
            + "    <variable name=\"JOB_ENV\">\n"
            + "      <value>foobar</value>\n"
            + "    </variable>\n"
            + "    <variable name=\"" + (isWindows() ? "Path" : "PATH") + "\">\n"
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

    private static final String SLEEP_TEN_SECONDS_ON_UNIX = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <exec command=\"/bin/sh\">\n"
            + "      <arg>-c</arg>\n"
            + "      <arg>echo before sleep; sleep 100</arg>\n"
            + "      <oncancel>\n"
            + "        <exec command=\"/bin/bash\">\n"
            + "           <arg>-c</arg>\n"
            + "           <arg>echo \"executing on cancel task\"; echo \"done\"</arg>\n"
            + "        </exec>\n"
            + "      </oncancel>\n"
            + "    </exec>\n"
            + "  </tasks>\n"
            + "</job>";

    private static final String SLEEP_TEN_SECONDS_ON_WINDOWS = "<job name=\"" + JOB_PLAN_NAME + "\">\n"
            + "  <tasks>\n"
            + "    <exec command=\"echo before sleep &amp; ping 1.1.1.1 -n 1 -w 100000 >NULL\">\n"
            + "      <oncancel>\n"
            + "        <exec command=\"echo\">\n"
            + "           <arg>executing on cancel task</arg>\n"
            + "        </exec>\n"
            + "      </oncancel>\n"
            + "    </exec>\n"
            + "  </tasks>\n"
            + "</job>";

    private static BuilderFactory builderFactory = new BuilderFactory(new AntTaskBuilder(), new ExecTaskBuilder(), new NantTaskBuilder(), new RakeTaskBuilder(),
            new PluggableTaskBuilderCreator(mock(TaskExtension.class)), new KillAllChildProcessTaskBuilder(), new FetchTaskBuilder(), new NullTaskBuilder());
    @Mock
    private static UpstreamPipelineResolver resolver;

    private volatile BuildSession buildSession;

    private static String willUpload(String file) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <artifact src=\"something-not-there.txt\" dest=\"dist\" />\n"
                + "      <artifact src=\"" + file + "\" dest=\"dist\\test\" />\n"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <exec command=\"echo\">\n"
                + "      <arg>hello world</arg>\n"
                + "    </exec>\n"
                + "  </tasks>\n"
                + "</job>";

    }

    private static String willUploadToDest(String file, String dest) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <artifact src=\"" + file + "\" dest=\"" + dest + "\" />\n"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <exec command=\"echo\">\n"
                + "      <arg>hello world</arg>\n"
                + "    </exec>\n"
                + "  </tasks>\n"
                + "</job>";
    }


    private static String willUploadTestArtifact(String path, String dest) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <artifacts>\n"
                + "      <test src=\"" + path + "\" dest=\"" + dest + "\" />\n"
                + "   </artifacts>"
                + "  <tasks>\n"
                + "    <exec command=\"echo\">\n"
                + "      <arg>hello world</arg>\n"
                + "    </exec>\n"
                + "  </tasks>\n"
                + "</job>";
    }

    private String willGenerateProperties(String src, String propertyName, String xpath) {
        return "<job name=\"" + JOB_PLAN_NAME + "\">\n"
                + "   <properties>\n"
                + "      <property name=\"" + propertyName + "\" src=\"" + src + "\" xpath=\"" +  xpath + "\" />\n"
                + "   </properties>"
                + "  <tasks>\n"
                + "    <exec command=\"echo\">\n"
                + "      <arg>hello world</arg>\n"
                + "    </exec>\n"
                + "  </tasks>\n"
                + "</job>";
    }



    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    private void build(String jobXml, String pipelineName, boolean fetchMaterials, boolean cleanWorkingDir) throws Exception {
        BuildAssignment assignment = getAssigment(jobXml, pipelineName, fetchMaterials, cleanWorkingDir);
        final BuildCommand buildCommand = new BuildComposer(assignment).compose();
        buildSession = newBuildSession();
        buildSession.setEnv("GO_SERVER_URL", SERVER_URL);
        buildSession.build(buildCommand);
    }


    @Test
    public void shouldUpdateBothStatusAndResultWhenBuildHasFailed() throws Exception {
        build(WILL_FAIL, PIPELINE_NAME, true, false);
        assertThat(statusReporter.status(), is(Arrays.asList(Preparing, Building, Completing, Completed)));
        assertThat(getLast(statusReporter.results()), is(Failed));
    }

    @Test
    public void shouldUpdateBothStatusAndResultWhenBuildHasPassed() throws Exception {
        build(WILL_PASS, PIPELINE_NAME, true, false);
        assertThat(statusReporter.status(), is(Arrays.asList(Preparing, Building, Completing, Completed)));
        assertThat(getLast(statusReporter.results()), is(Passed));
    }

    @Test
    public void shouldRunTaskWhenConditionMatches() throws Exception {
        build(MULTIPLE_RUN_IFS, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("[go] Current job status: passed."));
        assertThat(console.output(), containsString("[go] Start to execute task: <exec command=\"echo\" args=\"run when status is failed, passed or cancelled\" />."));
        assertThat(console.output(), containsString("run when status is failed, passed or cancelled"));
        assertThat(console.output(), not(containsString("Current job status: failed.")));
    }


    @Test
    public void shouldNotRunTaskWhichConditionDoesNotMatch() throws Exception {
        build(WILL_NOT_RUN, PIPELINE_NAME, true, false);
        assertThat(console.output(), not(containsString("run when status is failed")));
    }

    @Test
    public void shouldRunTasksBasedOnConditions() throws Exception {
        build(MULTIPLE_TASKS, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("run when status is failed"));
        assertThat(console.output(), printedExcRunIfInfo("command-not-found", "passed"));
        assertThat(console.output(), containsString("run when status is any"));
        assertThat(console.output(), printedExcRunIfInfo("echo", "run when status is any", "failed"));
        assertThat(console.output(), not(containsString("run when status is passed")));
        assertThat(console.output(), not(printedExcRunIfInfo("echo", "run when status is passed", "failed")));
        assertThat(console.output(), not(containsString("run when status is cancelled")));
        assertThat(console.output(), not(printedExcRunIfInfo("echo", "run when status is cancelled", "failed")));
    }

    @Test
    public void shouldReportDirectoryNotExists() throws Exception {
        build(NANT_WITH_WORKINGDIR, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("not-exists\" is not a directory!"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldReportErrorWhenComandIsNotExistOnLinux() throws Exception {
        build(CMD_NOT_EXIST, PIPELINE_NAME, true, false);
        assertThat(console.output(), printedAppsMissingInfoOnUnix(SOMETHING_NOT_EXIST));
        assertThat(statusReporter.results(), containsResult(Failed));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldReportErrorWhenComandIsNotExistOnWindows() throws Exception {
        build(CMD_NOT_EXIST, PIPELINE_NAME, true, false);
        assertThat(console.output(), printedAppsMissingInfoOnWindows(SOMETHING_NOT_EXIST));
        assertThat(statusReporter.results(), containsResult(Failed));
    }

    @Test
    public void shouldReportBuildStatusToConsoleout() throws Exception {
        build(WILL_FAIL, PIPELINE_NAME, true, false);

        String locator = JOB_IDENTIFIER.buildLocator();
        assertThat(console.output(), printedPreparingInfo(locator));
        assertThat(console.output(), printedBuildingInfo(locator));
        assertThat(console.output(), printedUploadingInfo(locator));
        assertThat(console.output(), printedBuildFailed());
        assertThat(console.output(), printedJobCompletedInfo(JOB_IDENTIFIER.buildLocatorForDisplay()));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void nantTest() throws Exception {
        build(NANT, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("Usage : NAnt [options] <target> <target> ..."));
    }

    @Test
    public void rakeTest() throws Exception {
        build(RAKE, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("rake [-f rakefile] {options} targets..."));
    }

    @Test
    public void shouldSkipMaterialUpdateWhenFetchMaterialsIsSetToFalse() throws Exception {
        build(WILL_PASS, PIPELINE_NAME, false, false);
        assertThat(console.output(), containsString("Start to prepare"));
        assertThat(console.output(), not(containsString("Start updating")));
        assertThat(console.output(), containsString("Skipping material update since stage is configured not to fetch materials"));
        assertThat(statusReporter.status().contains(JobState.Preparing), is(true));
    }

    @Test
    public void shouldUpdateMaterialsWhenFetchMaterialsIsTrue() throws Exception {
        build(WILL_PASS, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("Start to prepare"));
        assertThat(console.output(), containsString("Start to update materials"));
        assertThat(statusReporter.status().contains(JobState.Preparing), is(true));
    }

    @Test
    public void shouldCreateAgentWorkingDirectoryIfNotExist() throws Exception {
        String pipelineName = "pipeline1";
        File workingdir = new File(sandbox, "pipelines/" + pipelineName);
        assertThat(workingdir.exists(), is(false));
        build(WILL_PASS, pipelineName, true, false);

        assertThat(console.output(),
                not(containsString("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory")));

        assertThat(statusReporter.results().contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
    }

    @Test
    public void shouldNotBombWhenCreatingWorkingDirectoryIfCleanWorkingDirectoryFlagIsTrue() throws Exception {
        String pipelineName = "p1";
        File workingdir = new File(sandbox, "pipelines/" + pipelineName);
        assertThat(workingdir.exists(), is(false));
        build(WILL_PASS, pipelineName, true, true);

        assertThat(console.output(),
                not(containsString("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory")));

        assertThat(statusReporter.results().contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
    }

    @Test
    public void shouldCreateAgentWorkingDirectoryIfNotExistWhenFetchMaterialsIsFalse() throws Exception {
        String pipelineName = "p1";
        File workingdir = new File(sandbox, "pipelines/" + pipelineName);
        assertThat(workingdir.exists(), is(false));
        build(WILL_PASS, pipelineName, false, false);

        assertThat(console.output(), not(containsString("Working directory \"" + workingdir.getAbsolutePath() + "\" is not a directory")));
        assertThat(statusReporter.results().contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
    }

    @Test
    public void shouldCleanAgentWorkingDirectoryIfExistsWhenCleanWorkingDirIsTrue() throws Exception {
        String pipelineName = "p1";
        File workingdir = new File(sandbox, "pipelines/" + pipelineName);
        workingdir.mkdirs();
        new File(workingdir, "foo").createNewFile();
        new File(workingdir, "bar").mkdirs();
        assertThat(workingdir.listFiles().length, is(2));

        build(WILL_PASS, pipelineName, false, true);
        assertThat(statusReporter.results().contains(Passed), is(true));
        assertThat(workingdir.exists(), is(true));
        assertThat(workingdir.listFiles().length, is(0));
    }

    @Test
    public void shouldReportAgentLocation() throws Exception {
        buildVariables.put("agent.location", "far/far/away");
        build(WILL_PASS, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("[far/far/away]"));
    }

    @Test
    public void shouldReportEnvironmentVariables() throws Exception {
        build(WITH_ENV_VAR, PIPELINE_NAME, true, false);
        assertThat(console.output(), matches("'GO_SERVER_URL' (to|with) value '" + SERVER_URL));
        assertThat(console.output(), matches("'GO_PIPELINE_LABEL' (to|with) value '" + PIPELINE_LABEL));
        assertThat(console.output(), matches("'GO_PIPELINE_NAME' (to|with) value '" + PIPELINE_NAME));
        assertThat(console.output(), matches("'GO_STAGE_NAME' (to|with) value '" + STAGE_NAME));
        assertThat(console.output(), matches("'GO_STAGE_COUNTER' (to|with) value '" + STAGE_COUNTER));
        assertThat(console.output(), matches("'GO_JOB_NAME' (to|with) value '" + JOB_PLAN_NAME));
        assertThat(console.output(), containsString("[go] setting environment variable 'JOB_ENV' to value 'foobar'"));
        if (isWindows()) {
            assertThat(console.output(), containsString("[go] overriding environment variable 'Path' with value '/tmp'"));
        } else {
            assertThat(console.output(), containsString("[go] overriding environment variable 'PATH' with value '/tmp'"));
        }
    }

    @Test
    public void shouldMaskSecretInEnvironmentVarialbeReport() throws Exception {
        build(WITH_SECRET_ENV_VAR, PIPELINE_NAME, true, false);
        assertThat(console.output(), containsString("[go] setting environment variable 'foo' to value 'foo(******)'"));
        assertThat(console.output(), containsString("[go] setting environment variable 'bar' to value '********'"));
        assertThat(console.output(), not(containsString("i am a secret")));
    }

    @Test
    public void shouldRunCancelTaskWhenBuildIsCanceled() throws Exception {
        final Exception[] err = {null};
        Thread buildThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    build(SystemUtil.isWindows() ? SLEEP_TEN_SECONDS_ON_WINDOWS: SLEEP_TEN_SECONDS_ON_UNIX,
                            PIPELINE_NAME, true, false);
                } catch (Exception e) {
                    err[0] = e;
                }
            }
        });

        buildThread.start();
        console.waitForContain("before sleep", Timeout.FIVE_SECONDS);
        assertTrue(buildSession.cancel(30, TimeUnit.SECONDS));
        assertThat(statusReporter.status(), is(Arrays.asList(Preparing, Building, Completed)));
        assertThat(statusReporter.results(), is(Collections.singletonList(Cancelled)));
        assertThat(console.output(), printedJobCompletedInfo(JOB_IDENTIFIER.buildLocatorForDisplay()));
        assertThat(console.output(), printedJobCanceledInfo(JOB_IDENTIFIER.buildLocatorForDisplay()));
        assertThat(console.output(), containsString("executing on cancel task"));
        buildThread.join();
        if (err[0] != null) {
            throw err[0];
        }
    }

    @Test
    public void shouldReportUploadMessageWhenUpload() throws Exception {
        String destFolder = "dest\\test\\sub-folder";
        File basedir = new File(sandbox, "pipelines/pipeline1");
        basedir.mkdirs();
        File artifact = new File(basedir, "artifact");
        artifact.createNewFile();
        build(willUploadToDest("artifact", destFolder), PIPELINE_NAME, true, false);
        assertThat("build should pass, console output is" + console.output(), getLast(statusReporter.results()), is(Passed));
        assertThat(artifactsRepository.getFileUploaded().size(), is(1));
        assertThat(artifactsRepository.getFileUploaded().get(0).file, is(artifact));
    }

    @Test
    public void shouldFailTheJobWhenFailedToUploadArtifact() throws Exception {
        artifactsRepository.setUploadError(new RuntimeException("upload failed"));
        build(willUpload("cruise-output/log.xml"), PIPELINE_NAME, true, false);
        assertThat(statusReporter.results(), containsResult(Failed));
    }

    @Test
    public void generateReportForNUnit() throws Exception {
        File basedir = new File(sandbox, "pipelines/pipeline1/test-reports");
        basedir.mkdirs();

        InputStream nunitResult = new ClassPathResource(FileUtil.fileseparator() + "data" + FileUtil.fileseparator() + "TestResult.xml").getInputStream();
        FileOutputStream testArtifact = new FileOutputStream(new File(basedir, "test-result.xml"));
        copyAndClose(nunitResult, testArtifact);

        build(willUploadTestArtifact("test-reports", "test-report-dest"), PIPELINE_NAME, false, false);

        assertThat(artifactsRepository.getFileUploaded().size(), Is.is(2));
        assertThat(artifactsRepository.getFileUploaded().get(0).file, is(basedir));
        assertThat(artifactsRepository.getFileUploaded().get(0).destPath, is("test-report-dest"));
        assertThat(artifactsRepository.getFileUploaded().get(1).destPath, is("testoutput"));
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.TOTAL_TEST_COUNT), is("206"));
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.FAILED_TEST_COUNT), is("0"));
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.IGNORED_TEST_COUNT), is("0"));
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.TEST_TIME), is("NaN"));
    }

    @Test
    public void generateBuildPropertyUsingXPath() throws Exception {
        File basedir = new File(sandbox, "pipelines/pipeline1");
        String content = "<artifacts>\n"
                + "         <artifact src=\"target\\connectfour.jar\" dest=\"dist\\jars\" />\n"
                + "         <artifact src=\"target\\test-results\" dest=\"testoutput\" type=\"junit\" />\n"
                + "         <artifact src=\"build.xml\" />\n"
                + "       </artifacts>\n";
        File file = new File(basedir, "xmlfile");
        FileUtils.writeStringToFile(file, content, "UTF-8");
        build(willGenerateProperties("xmlfile", "artifactsrc", "//artifact/@src"), PIPELINE_NAME, false, false);
        assertThat(buildInfo(), getLast(statusReporter.results()), is(Passed));
        assertThat(artifactsRepository.propertyValue("artifactsrc"), is("target\\connectfour.jar"));
    }

    private static BuildAssignment getAssigment(String jobXml, String pipelineName, boolean fetchMaterials, boolean cleanWorkingDir) throws Exception {
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

        return BuildAssignment.create(jobPlan,
                BuildCause.createWithEmptyModifications(),
                builder, pipeline.defaultWorkingFolder()
        );
    }
}
