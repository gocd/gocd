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

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.TestArtifactPlan;
import com.thoughtworks.go.domain.JobInstanceLog;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(org.jmock.integration.junit4.JMock.class)
public class JobInstanceLogTest {
    private JobInstanceLog jobInstanceLog;
    private LogFile defaultLogFile;
    private Mockery context;
    private File rootFolder;

    @Before
    public void setUp() {
        context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        jobInstanceLog = new JobInstanceLog(null, new HashMap());
        defaultLogFile = new LogFile(new File("log20051209122103.xml"));
        rootFolder = new File("root");
        rootFolder.mkdirs();
    }

    @After
    public void teardown() {
        FileUtil.deleteFolder(rootFolder);
    }

    @Test
    public void canGetNumberOfTestsFromBuild() {
        BuildTestSuite suiteWithFiveTests = new BuildTestSuite("", 0.0f);
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        BuildTestSuite suiteWithFourTests = new BuildTestSuite("", 0.0f);
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        List nineTests = new ArrayList();
        nineTests.add(suiteWithFiveTests);
        nineTests.add(suiteWithFourTests);

        Map props = new HashMap();
        props.put("testsuites", nineTests);
        JobInstanceLog laterJob = new JobInstanceLog(defaultLogFile, props);
        assertEquals(9, laterJob.getNumberOfTests());
    }

    @Test
    public void canGetNumberOfFailedTests() {
        BuildTestSuite suiteWithTwoFailures = new BuildTestSuite("", 0.0f);
        suiteWithTwoFailures.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.FAILED));
        suiteWithTwoFailures.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.FAILED));
        BuildTestSuite suiteWithOneFailure = new BuildTestSuite("", 0.0f);
        suiteWithOneFailure.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.FAILED));
        List threeFailures = new ArrayList();
        threeFailures.add(suiteWithTwoFailures);
        threeFailures.add(suiteWithOneFailure);

        Map props = new HashMap();
        props.put("testsuites", threeFailures);
        JobInstanceLog laterJob = new JobInstanceLog(defaultLogFile, props);

        assertEquals(3, laterJob.getNumberOfFailures());
    }

    @Test
    public void canGetNumberOfTestErrors() {
        BuildTestSuite suiteWithTwoErrors = new BuildTestSuite("", 0.0f);
        suiteWithTwoErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        suiteWithTwoErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        BuildTestSuite suiteWithThreeErrors = new BuildTestSuite("", 0.0f);
        suiteWithThreeErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        suiteWithThreeErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        suiteWithThreeErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        List fiveErrors = new ArrayList();
        fiveErrors.add(suiteWithTwoErrors);
        fiveErrors.add(suiteWithThreeErrors);

        Map props = new HashMap();
        props.put("testsuites", fiveErrors);
        JobInstanceLog laterJob = new JobInstanceLog(defaultLogFile, props);

        assertEquals(5, laterJob.getNumberOfErrors());
    }

    @Test
    public void shouldFindIndexPageFromTestOutput() throws Exception {
        rootFolder = new File("root");
        final File testOutput = new File(rootFolder, "testoutput");
        testOutput.mkdirs();
        File indexHtml = new File(testOutput, "index.html");
        FileOutputStream fileOutputStream = new FileOutputStream(indexHtml);
        IOUtils.write("Test", fileOutputStream);
        IOUtils.closeQuietly(fileOutputStream);
        HashMap map = new HashMap();
        map.put("artifactfolder", rootFolder);
        jobInstanceLog = new JobInstanceLog(null, map);
        assertThat(jobInstanceLog.getTestIndexPage(), is(indexHtml));
    }

    @Test
    public void shouldNotCauseExceptionIfFilesAreNotAvailable() throws Exception {
        rootFolder = new File("root");
        final File testOutput = new File(rootFolder, "testoutputxxxxxxx");

        HashMap map = new HashMap();
        map.put("artifactfolder", testOutput);
        jobInstanceLog = new JobInstanceLog(null, map);
        assertThat(jobInstanceLog.getTestIndexPage(), is(nullValue()));
    }

    @Test
    public void shouldFindIndexPageFromTestOutputRecursively() throws Exception {

        final File testOutput = new File(rootFolder, "testoutput");
        final File junitReportFolder = new File(testOutput, "junitreport");
        junitReportFolder.mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(new File(junitReportFolder, "index.html"));
        IOUtils.write("Test", fileOutputStream);
        IOUtils.closeQuietly(fileOutputStream);
        HashMap map = new HashMap();
        map.put("artifactfolder", rootFolder);
        jobInstanceLog = new JobInstanceLog(null, map);
        assertThat(jobInstanceLog.getTestIndexPage().getName(), is("index.html"));

    }

    @Test
    public void shouldFindIndexPageFromTestOutputRecursivelyWithMultipleFolders() throws Exception {

        final File logFolder = new File(rootFolder, "logs");
        final File testOutput = new File(rootFolder, TestArtifactPlan.TEST_OUTPUT_FOLDER);
        final File junitReportFolder = new File(testOutput, "junitreport");
        junitReportFolder.mkdirs();
        logFolder.mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(new File(junitReportFolder, "index.html"));
        IOUtils.write("Test", fileOutputStream);
        IOUtils.closeQuietly(fileOutputStream);
        HashMap map = new HashMap();
        map.put("artifactfolder", rootFolder);
        jobInstanceLog = new JobInstanceLog(null, map);
        assertThat(jobInstanceLog.getTestIndexPage().getName(), is("index.html"));

    }

    @Test
    public void shouldFindIndexPageFromTestOutputGivenNunitTestOutput() throws Exception {

        final File testOutput = new File(rootFolder, TestArtifactPlan.TEST_OUTPUT_FOLDER);
        testOutput.mkdirs();
        IOUtils.write("Test", new FileOutputStream(new File(testOutput, "index.html")));
        HashMap map = new HashMap();
        map.put("artifactfolder", rootFolder);
        jobInstanceLog = new JobInstanceLog(null, map);
        assertThat(jobInstanceLog.getTestIndexPage().getName(), is("index.html"));
        FileUtil.deleteFolder(rootFolder);
    }
}