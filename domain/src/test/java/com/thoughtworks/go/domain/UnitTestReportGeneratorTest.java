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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.XpathUtils;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Path;
import java.util.Objects;

import static com.thoughtworks.go.util.TestUtils.copyAndClose;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@MockitoSettings
public class UnitTestReportGeneratorTest {
    private static final String CSS_TOTAL_TEST_COUNT = "tests_total_count";
    private static final String CSS_FAILED_TEST_COUNT = "tests_failed_count";
    private static final String CSS_IGNORED_TEST_COUNT = "tests_ignored_count";
    private static final String CSS_TEST_TIME = "tests_total_duration";

    private File testFolder;
    private UnitTestReportGenerator generator;

    @Mock
    private GoPublisher publisher;

    @Captor
    private ArgumentCaptor<File> uploadedFile;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException {
        testFolder = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
        generator = new UnitTestReportGenerator(publisher, testFolder);
    }

    @AfterEach
    public void tearDown() {
        FileUtils.deleteQuietly(testFolder);
    }

    @Test
    public void shouldNotGenerateAnyReportIfNoTestResultsWereFound() throws Exception {
        generator.generate(testFolder.listFiles(), "testoutput");
        expectZeroedProperties();
    }

    @Test
    public void shouldNotGenerateAnyReportIfTestResultIsEmpty() throws Exception {
        copyAndClose(source("empty.xml"), target("empty.xml"));

        generator.generate(testFolder.listFiles(), "testoutput");

        verify(publisher).consumeLine("Ignoring file empty.xml - it is not a recognised test file.");
        expectZeroedProperties();
    }

    @Nested
    class Nunit {
        @Test
        public void shouldGenerateReportForNUnit() throws Exception {
            copyAndClose(source("nunit-result-206.xml"), target("test-result.xml"));
            generator.generate(testFolder.listFiles(), "testoutput");
            assertThat(testFolder.listFiles().length).isEqualTo(2);
            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("206");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("NaN");
        }


        @Test
        public void shouldNotGenerateAnyReportIfTestReportIsInvalid() throws Exception {
            copyAndClose(source("invalid-nunit.xml"), target("Invalid.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).consumeLine("Ignoring file Invalid.xml - it is not a recognised test file.");
            expectZeroedProperties();
        }

        //This is bug #2319
        @Test
        public void shouldStillUploadResultsIfReportIsIllegalBug2319() throws Exception {
            copyAndClose(source("ncover-report.xml"), target("ncover-report.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).consumeLine("Ignoring file ncover-report.xml - it is not a recognised test file.");
            expectZeroedProperties();
        }

        @Test
        public void shouldGenerateReportForNUnitGivenMultipleInputFiles() throws Exception {
            copyAndClose(source("nunit-result-integration.xml"), target("test-result1.xml"));
            copyAndClose(source("nunit-result-unit.xml"), target("test-result2.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("2762");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("120");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("221.766");
        }
        @Test
        public void shouldGenerateReportForXmlFilesRecursivelyInAFolder() throws Exception {
            File reports = new File(testFolder.getAbsoluteFile(), "reports");
            reports.mkdir();
            File module = new File(reports, "module");
            module.mkdir();
            copyAndClose(source("ncover-report.xml"), target("reports/module/ncover-report.xml"));
            copyAndClose(source("nunit-result-204.xml"), target("reports/nunit-result-204.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).consumeLine("Ignoring file ncover-report.xml - it is not a recognised test file.");
            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("204");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("6");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("80.231");
        }
    }

    @Nested
    class Junit {

        @Test
        public void shouldGenerateReportForJUnit() throws Exception {
            copyAndClose(source("junit-result-single-test.xml"), target("AgentTest.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("1");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo(".456");
        }

        @Test
        public void shouldGenerateReportForJUnitWithoutDeclaration() throws Exception {
            copyAndClose(source("junit-result-no-decl.xml"), target("AgentTest.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("1");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("15.839");
        }

        @Test
        public void shouldGenerateReportForMinifiedJunit() throws Exception {
            copyAndClose(source("junit-minified-from-pytest.xml"), target("junit-minified-from-pytest.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("1");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("15.839");
        }

        @Test
        public void shouldGenerateReportForMinifiedJunitWithoutDeclaration() throws Exception {
            copyAndClose(source("junit-minified-from-pytest-no-decl.xml"), target("junit-minified-from-pytest.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("1");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("15.839");
        }

        @Test
        public void shouldGenerateReportForJUnitWithMultipleFiles() throws Exception {
            copyAndClose(source("junit-result-four-tests.xml"), target("junit-result-four-tests.xml"));
            copyAndClose(source("junit-result-single-test.xml"), target("junit-result-single-test.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("5");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("3");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("1.286");
        }

        @Test
        public void shouldGenerateReportForMinifiedMixedWithOthersJunit() throws Exception {
            copyAndClose(source("junit-minified-from-pytest.xml"), target("junit-minified-from-pytest.xml"));
            copyAndClose(source("junit-result-four-tests.xml"), target("junit-result-single-test.xml"));

            generator.generate(testFolder.listFiles(), "testoutput");

            verify(publisher).upload(uploadedFile.capture(), any(String.class));
            assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("5");
            assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("3");
            assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
            assertTestReportCssFor(CSS_TEST_TIME).isEqualTo("16.669");
        }
    }

    private void expectZeroedProperties() throws Exception {
        verify(publisher).upload(uploadedFile.capture(), any(String.class));
        assertTestReportCssFor(CSS_TOTAL_TEST_COUNT).isEqualTo("0");
        assertTestReportCssFor(CSS_FAILED_TEST_COUNT).isEqualTo("0");
        assertTestReportCssFor(CSS_IGNORED_TEST_COUNT).isEqualTo("0");
        assertTestReportCssFor(CSS_TEST_TIME).isEqualTo(".000");
    }

    private AbstractStringAssert<?> assertTestReportCssFor(String cssClass) throws XPathExpressionException, IOException {
        return assertThat(XpathUtils.evaluate(uploadedFile.getValue(), xpathFor(cssClass)));
    }

    private static String xpathFor(String cssClass) {
        return "//div/p/span[@class='" + cssClass + "']";
    }

    private OutputStream target(String targetFile) throws FileNotFoundException {
        return new FileOutputStream(testFolder.getAbsolutePath() + File.separator + targetFile);
    }

    private InputStream source(String filename) {
        return Objects.requireNonNull(getClass().getResourceAsStream("/data/test-results/" +  filename));
    }
}
