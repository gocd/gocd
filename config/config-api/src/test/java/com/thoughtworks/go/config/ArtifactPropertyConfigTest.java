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

package com.thoughtworks.go.config;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArtifactPropertyConfigTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File workspace;

    @Before
    public void setUp() throws Exception {
        this.workspace = temporaryFolder.newFolder("workspace");
    }

    @Test
    public void shouldAddErrorToErrorCollection() throws IOException {
        String multipleMatchXPATH = "//artifact/@src";
        ArtifactPropertyConfig generator = new ArtifactPropertyConfig("test_property", createSrcFile().getName(), multipleMatchXPATH);
        generator.addError("src", "Source invalid");
        assertThat(generator.errors().on("src"), is("Source invalid"));
    }

    @Test
    public void shouldValidateThatNameIsMandatory() {
        ArtifactPropertyConfig generator = new ArtifactPropertyConfig(null, "props.xml", "//some_xpath");
        generator.validateTree(null);
        assertThat(generator.errors().on(ArtifactPropertyConfig.NAME), containsString("Invalid property name 'null'."));
    }

    private File createSrcFile() throws IOException {
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
        File file = new File(workspace, "xmlfile");
        FileUtils.writeStringToFile(file, content, "UTF-8");
        return file;
    }
}
