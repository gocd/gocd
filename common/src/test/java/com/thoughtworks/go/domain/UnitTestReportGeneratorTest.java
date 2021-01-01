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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.domain.exception.ArtifactPublishingException;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

import java.io.*;

import static com.thoughtworks.go.util.TestUtils.copyAndClose;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class UnitTestReportGeneratorTest {

    private File testFolder;
    private UnitTestReportGenerator generator;
    private DefaultGoPublisher publisher;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        temporaryFolder.create();
        testFolder = temporaryFolder.newFolder();
        publisher = mock(DefaultGoPublisher.class);
        generator = new UnitTestReportGenerator(publisher, testFolder);
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(testFolder);
    }

    @Test
    public void shouldGenerateReportForNUnit() throws IOException, ArtifactPublishingException {
        copyAndClose(source("TestResult.xml"), target("test-result.xml"));
        generator.generate(testFolder.listFiles(), "testoutput");
        assertThat(testFolder.listFiles().length, is(2));
        verify(publisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldNotGenerateAnyReportIfNoTestResultsWereFound() throws IOException, ArtifactPublishingException {
        generator.generate(testFolder.listFiles(), "testoutput");
        expectZeroedProperties();
    }

    @Test
    public void shouldNotGenerateAnyReportIfTestResultIsEmpty() throws IOException, ArtifactPublishingException {
        copyAndClose(source("empty.xml"), target("empty.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).consumeLine("Ignoring file empty.xml - it is not a recognised test file.");
        verify(publisher).upload(any(File.class), any(String.class));
    }

    private void expectZeroedProperties() throws ArtifactPublishingException {
        verify(publisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldNotGenerateAnyReportIfTestReportIsInvalid() throws IOException, ArtifactPublishingException {
        copyAndClose(source("InvalidTestResult.xml"), target("Invalid.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).consumeLine("Ignoring file Invalid.xml - it is not a recognised test file.");
        verify(publisher).upload(any(File.class), any(String.class));
    }

    //This is bug #2319
    @Test
    public void shouldStillUploadResultsIfReportIsIllegalBug2319() throws IOException, ArtifactPublishingException {
        copyAndClose(source("xml_samples/Coverage.xml"), target("Coverage.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).consumeLine("Ignoring file Coverage.xml - it is not a recognised test file.");
        verify(publisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldGenerateReportForJUnitAlso() throws IOException, ArtifactPublishingException {
        copyAndClose(source("SerializableProjectConfigUtilTest.xml"), target("AgentTest.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldGenerateReportForJUnitWithMultipleFiles() throws IOException, ArtifactPublishingException {
        copyAndClose(source("UnitTestReportGeneratorTest.xml"), target("UnitTestReportGeneratorTest.xml"));
        copyAndClose(source("SerializableProjectConfigUtilTest.xml"),
                target("SerializableProjectConfigUtilTest.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldGenerateReportForNUnitGivenMutipleInputFiles() throws IOException, ArtifactPublishingException {
        copyAndClose(source("TestReport-Integration.xml"), target("test-result1.xml"));
        copyAndClose(source("TestReport-Unit.xml"), target("test-result2.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldGenerateReportForXmlFilesRecursivelyInAFolder() throws ArtifactPublishingException, IOException {
        File reports = new File(testFolder.getAbsoluteFile(), "reports");
        reports.mkdir();
        File module = new File(reports, "module");
        module.mkdir();
        copyAndClose(source("xml_samples/Coverage.xml"), target("reports/module/Coverage.xml"));
        copyAndClose(source("xml_samples/TestResult.xml"), target("reports/TestResult.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).consumeLine("Ignoring file Coverage.xml - it is not a recognised test file.");
        verify(publisher).upload(any(File.class), any(String.class));
    }

    private OutputStream target(String targetFile) throws FileNotFoundException {
        return new FileOutputStream(testFolder.getAbsolutePath() + FileUtil.fileseparator() + targetFile);
    }

    private InputStream source(String filename) throws IOException {
        return new ClassPathResource(FileUtil.fileseparator() + "data" + FileUtil.fileseparator() + filename).getInputStream();
    }


}
