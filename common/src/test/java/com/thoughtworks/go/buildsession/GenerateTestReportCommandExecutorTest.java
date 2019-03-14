/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.TestReportGenerator;
import com.thoughtworks.go.domain.exception.ArtifactPublishingException;
import com.thoughtworks.go.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.util.Collections;

import static com.thoughtworks.go.util.TestUtils.copyAndClose;
import static org.assertj.core.api.Assertions.assertThat;

class GenerateTestReportCommandExecutorTest extends BuildSessionBasedTestCase {

    private File testFolder;

    @BeforeEach
    void setUp() {
        testFolder = new File(sandbox, "test-reports");
        testFolder.mkdir();
    }

    @Test
    void generateReportForNUnit() throws IOException, ArtifactPublishingException {
        copyAndClose(source("TestResult.xml"), target("test-result.xml"));
        runBuild(BuildCommand.generateTestReport(Collections.singletonList("test-reports/test-result.xml"), "test-out"), JobResult.Passed);
        assertThat(artifactsRepository.getFileUploaded().size()).isEqualTo(1);
        assertThat(artifactsRepository.getFileUploaded().get(0).destPath).isEqualTo("test-out");
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.TOTAL_TEST_COUNT)).isEqualTo("206");
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.FAILED_TEST_COUNT)).isEqualTo("0");
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.IGNORED_TEST_COUNT)).isEqualTo("0");
        assertThat(artifactsRepository.propertyValue(TestReportGenerator.TEST_TIME)).isEqualTo("NaN");
    }


    private OutputStream target(String targetFile) throws FileNotFoundException {
        return new FileOutputStream(testFolder.getAbsolutePath() + FileUtil.fileseparator() + targetFile);
    }

    private InputStream source(String filename) throws IOException {
        return new ClassPathResource(FileUtil.fileseparator() + "data" + FileUtil.fileseparator() + filename).getInputStream();
    }

}
