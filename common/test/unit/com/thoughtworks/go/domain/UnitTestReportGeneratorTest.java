/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import java.io.*;
import java.util.UUID;

import com.thoughtworks.go.domain.exception.ArtifactPublishingException;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;

import static com.thoughtworks.go.domain.UnitTestReportGenerator.FAILED_TEST_COUNT;
import static com.thoughtworks.go.domain.UnitTestReportGenerator.IGNORED_TEST_COUNT;
import static com.thoughtworks.go.domain.UnitTestReportGenerator.TEST_TIME;
import static com.thoughtworks.go.domain.UnitTestReportGenerator.TOTAL_TEST_COUNT;
import static com.thoughtworks.go.util.TestUtils.copyAndClose;
import static com.thoughtworks.go.util.TestUtils.restoreConsoleOutput;
import static com.thoughtworks.go.util.TestUtils.suppressConsoleOutput;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


@RunWith(JMock.class)
public class UnitTestReportGeneratorTest {
    private final Mockery context = new ClassMockery();

    private File testFolder;
    private UnitTestReportGenerator generator;
    private DefaultGoPublisher publisher;

    @Before
    public void setUp() {
        testFolder = TestFileUtil.createTempFolder(UUID.randomUUID().toString());
        publisher = context.mock(DefaultGoPublisher.class);
        generator = new UnitTestReportGenerator(publisher, testFolder);
    }

    @After
    public void tearDown() {
        FileUtil.deleteFolder(testFolder);
    }

    @Test
    public void shouldGenerateReportForNUnit() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "206"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "NaN"));
            }
        });


        copyAndClose(source("TestResult.xml"), target("test-result.xml"));
        final Properties properties = generator.generate(testFolder.listFiles(), "testoutput");
        assertThat(testFolder.listFiles().length, is(2));
    }

    @Test
    public void shouldGenerateReportForNUnitXmlWithByteOrderMark() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "18"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "1.570"));
            }
        });

        copyAndClose(source("NunitTestResultWithByteOrderMark.xml"), target("test-result.xml"));
        generator.generate(testFolder.listFiles(), "testoutput");
        assertThat(testFolder.listFiles().length, is(2));
    }

    @Test
    public void shouldNotGenerateAnyReportIfNoTestResultsWereFound() throws IOException, ArtifactPublishingException {
        expectZeroedProperties();

        suppressConsoleOutput();
        generator.generate(testFolder.listFiles(), "testoutput");
        restoreConsoleOutput();
    }

    @Test
    public void shouldNotGenerateAnyReportIfTestResultIsEmpty() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).consumeLine("Ignoring file empty.xml - it is not a recognised test file.");
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "0.000"));
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
            }
        });

        copyAndClose(source("empty.xml"), target("empty.xml"));

        suppressConsoleOutput();
        generator.generate(testFolder.listFiles(), "testoutput");
        restoreConsoleOutput();
    }

    private void expectZeroedProperties() throws ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "0.000"));
            }
        });
    }

    @Test
    public void shouldNotGenerateAnyReportIfTestReportIsInvalid() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).consumeLine("Ignoring file Invalid.xml - it is not a recognised test file.");
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "0.000"));
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
            }
        });

        copyAndClose(source("InvalidTestResult.xml"), target("Invalid.xml"));

        suppressConsoleOutput();
        generator.generate(testFolder.listFiles(), "testoutput");
        restoreConsoleOutput();
    }

    //This is bug #2319
    @Test
    public void shouldStillUploadResultsIfReportIsIllegalBug2319() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).consumeLine("Ignoring file Coverage.xml - it is not a recognised test file.");
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "0.000"));
            }
        });

        copyAndClose(source("xml_samples/Coverage.xml"), target("Coverage.xml"));

        suppressConsoleOutput();
        generator.generate(testFolder.listFiles(), "testoutput");
        restoreConsoleOutput();
    }

    @Test
    public void shouldGenerateReportForJUnitAlso() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "1"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "0.456"));
            }
        });

        copyAndClose(source("SerializableProjectConfigUtilTest.xml"), target("AgentTest.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");
    }

    @Test
    public void shouldGenerateReportForJUnitWithMultipleFiles() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "5"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "3"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(TEST_TIME, "1.286"));
            }
        });


        copyAndClose(source("UnitTestReportGeneratorTest.xml"), target("UnitTestReportGeneratorTest.xml"));
        copyAndClose(source("SerializableProjectConfigUtilTest.xml"),
                target("SerializableProjectConfigUtilTest.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");
    }

    @Test
    public void shouldGenerateReportForNUnitGivenMutipleInputFiles() throws IOException, ArtifactPublishingException {
        context.checking(new Expectations() {
            {
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "2762"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "120"));
                one(publisher).setProperty(new Property(TEST_TIME, "221.766"));
            }
        });

        copyAndClose(source("TestReport-Integration.xml"), target("test-result1.xml"));
        copyAndClose(source("TestReport-Unit.xml"), target("test-result2.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");
    }

    @Test
    public void shouldGenerateReportForXmlFilesRecursivelyInAFolder() throws ArtifactPublishingException, IOException {
         context.checking(new Expectations() {
            {
                one(publisher).consumeLine("Ignoring file Coverage.xml - it is not a recognised test file.");
                one(publisher).upload(with(any(File.class)), with(any(String.class)));
                one(publisher).setProperty(new Property(TOTAL_TEST_COUNT, "204"));
                one(publisher).setProperty(new Property(FAILED_TEST_COUNT, "0"));
                one(publisher).setProperty(new Property(IGNORED_TEST_COUNT, "6"));
                one(publisher).setProperty(new Property(TEST_TIME, "80.231"));
            }
        });

        File reports = new File(testFolder.getAbsoluteFile(), "reports");
        reports.mkdir();
        File module = new File(reports, "module");
        module.mkdir();
        copyAndClose(source("xml_samples/Coverage.xml"), target("reports/module/Coverage.xml"));
        copyAndClose(source("xml_samples/TestResult.xml"), target("reports/TestResult.xml"));

        suppressConsoleOutput();
        generator.generate(testFolder.listFiles(), "testoutput");
        restoreConsoleOutput();
    }

    private OutputStream target(String targetFile) throws FileNotFoundException {
        return new FileOutputStream(testFolder.getAbsolutePath() + FileUtil.fileseparator() + targetFile);
    }

    private InputStream source(String filename) throws IOException {
        return new ClassPathResource(FileUtil.fileseparator() + "data" + FileUtil.fileseparator() + filename).getInputStream();
    }


}
