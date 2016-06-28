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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.JobResult;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GeneratePropertyCommandExecutorTest extends BuildSessionBasedTestCase {
    private static final String TEST_PROPERTY = "test_property";

    @Test
    public void shouldReportFailureWhenArtifactFileDoesNotExist() throws IOException {
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "not-exists.xml", "//src"), JobResult.Passed);
        assertThat(console.output(), containsString("Failed to create property"));
        assertThat(console.output(), containsString(new File(sandbox, "not-exists.xml").getAbsolutePath()));
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY), nullValue());
    }

    @Test
    public void shouldReportNotingMatchedWhenNoNodeCanMatch() throws IOException {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "//HTML"), JobResult.Passed);
        assertThat(console.output(), containsString("Failed to create property"));
        assertThat(console.output(), containsString("Nothing matched xpath \"//HTML\""));
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY), nullValue());
    }

    @Test
    public void shouldReportNotingMatchedWhenXPATHisNotValid() throws IOException {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "////////HTML"), JobResult.Passed);
        assertThat(console.output(), containsString("Failed to create property"));
        assertThat(console.output(), containsString("Illegal xpath: \"////////HTML\""));
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY), nullValue());
    }

    @Test
    public void shouldReportPropertyIsCreated() throws Exception {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "//buildplan/@name"), JobResult.Passed);
        assertThat(console.output(), containsString("Property " + TEST_PROPERTY + " = test created"));
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY), is("test"));
    }

    @Test
    public void shouldReportFirstMatchedProperty() throws Exception {
        createSrcFile("xmlfile");
        runBuild(BuildCommand.generateProperty(TEST_PROPERTY, "xmlfile", "//artifact/@src"), JobResult.Passed);
        assertThat(console.output(), containsString("Property " + TEST_PROPERTY + " = target\\connectfour.jar created"));
        assertThat(artifactsRepository.propertyValue(TEST_PROPERTY), is("target\\connectfour.jar"));
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