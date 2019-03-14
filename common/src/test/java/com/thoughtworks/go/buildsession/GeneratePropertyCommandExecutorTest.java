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
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratePropertyCommandExecutorTest extends BuildSessionBasedTestCase {
    private static final String TEST_PROPERTY = "test_property";

    @Test
    void shouldReportFailureWhenArtifactFileDoesNotExist() {
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "not-exists.xml", "//src"), JobResult.Passed);
        assertThat(console.output()).contains("Failed to create property");
        assertThat(console.output()).contains(new File(sandbox, "not-exists.xml").getAbsolutePath());
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY)).isNull();
    }

    @Test
    void shouldReportNotingMatchedWhenNoNodeCanMatch() throws IOException {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "//HTML"), JobResult.Passed);
        assertThat(console.output()).contains("Failed to create property");
        assertThat(console.output()).contains("Nothing matched xpath \"//HTML\"");
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY)).isNull();
    }

    @Test
    void shouldReportNotingMatchedWhenXPATHisNotValid() throws IOException {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "////////HTML"), JobResult.Passed);
        assertThat(console.output()).contains("Failed to create property");
        assertThat(console.output()).contains("Illegal xpath: \"////////HTML\"");
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY)).isNull();
    }

    @Test
    void shouldReportPropertyIsCreated() throws Exception {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "//buildplan/@name"), JobResult.Passed);
        assertThat(console.output()).contains("Property " + TEST_PROPERTY + " = test created");
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY)).isEqualTo("test");
    }

    @Test
    void shouldReportFirstMatchedProperty() throws Exception {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "//artifact/@src"), JobResult.Passed);
        assertThat(console.output()).contains("Property " + TEST_PROPERTY + " = target\\connectfour.jar created");
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY)).isEqualTo("target\\connectfour.jar");
    }

    private void createSrcFile(String filename) throws IOException {
        String content = "<buildplans>\n"
                + "          <buildplan name=\"test\">\n"
                + "            <artifacts>\n"
                + "              <artifact src=\"target\\connectfour.jar\" dest=\"dist\\jars\" />\n"
                + "              <artifact src=\"target\\test-results\" dest=\"testoutput\" type=\"junit\" />\n"
                + "              <artifact src=\"build.xml\" />\n"
                + "            </artifacts>\n"
                + "            <tasks>\n"
                + "              <ant workingdir=\"dev\" target=\"all\" />\n"
                + "            </tasks>\n"
                + "          </buildplan>\n"
                + "        </buildplans>";
        FileUtils.writeStringToFile(new File(sandbox, filename), content, "UTF-8");
    }
}
