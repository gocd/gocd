/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.ArtifactPropertiesConfig;
import com.thoughtworks.go.config.ArtifactPropertyConfig;
import com.thoughtworks.go.domain.exception.ArtifactPublishingException;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.ConsoleOutputTransmitter;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.work.DefaultGoPublisher;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class ArtifactPropertiesGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private List<String> sentContents;
    private ArtifactPropertiesGenerator generator;
    private GoPublisherImple goPublisherImple;
    private File workspace;
    private ArrayList<String> sentErrors;

    @Before
    public void setUp() throws Exception {
        this.workspace = temporaryFolder.newFolder("workspace");

        this.sentContents = new ArrayList<>();
        this.sentErrors = new ArrayList<>();
        goPublisherImple = new GoPublisherImple(sentContents, sentErrors);
    }

    @Test
    public void shouldReportFailureWhenArtifactFileDoesNotExist() {
        File workspaceNotExist = new File("dir-not-exist");
        File srcFile = new File(workspaceNotExist, "file.xml");
        generator = new ArtifactPropertiesGenerator("test_property", srcFile.getName(), null);

        generator.generate(goPublisherImple, workspaceNotExist);
        assertThat(sentContents.get(0), containsString("Failed to create property"));
        assertThat(sentContents.get(0), containsString(srcFile.getAbsolutePath()));
    }

    @Test
    public void shouldReportNotingMatchedWhenNoNodeCanMatch() throws IOException {
        File srcFile = createSrcFile();
        String noNodeCanMatch = "//HTML";

        generator = new ArtifactPropertiesGenerator("test_property", srcFile.getName(), noNodeCanMatch);
        generator.generate(goPublisherImple, workspace);

        assertThat(sentContents.get(0), containsString("Failed to create property"));
        assertThat(sentContents.get(0), containsString(srcFile.getAbsolutePath()));
    }

    @Test
    public void shouldReportNotingMatchedWhenXPATHisNotValid() throws IOException {
        String noNodeCanMatch = "////////HTML";
        generator = new ArtifactPropertiesGenerator("test_property", createSrcFile().getName(), noNodeCanMatch);

        generator.generate(goPublisherImple, workspace);

        assertThat(sentErrors.get(0), containsString("Failed to create property"));
    }

    @Test
    public void shouldReportPropertyIsCreated() throws Exception {
        String validXpath = "//buildplan/@name";
        generator = new ArtifactPropertiesGenerator("test_property", createSrcFile().getName(), validXpath);

        generator.generate(goPublisherImple, workspace);

        assertThat(sentContents.get(0), containsString("Property " + "test_property" + " = test created"));
    }

    @Test
    public void shouldReportFirstMatchedProperty() throws Exception {
        String multipleMatchXPATH = "//artifact/@src";
        generator = new ArtifactPropertiesGenerator("test_property", createSrcFile().getName(), multipleMatchXPATH);

        generator.generate(goPublisherImple, workspace);

        assertThat(sentContents.get(0),
                containsString("Property " + "test_property" + " = target\\connectfour.jar created"));
    }

    @Test
    public void shouldConvertArtifactPropertiesConfigToArtifactPropertiesGenerator() {
        final ArtifactPropertiesConfig artifactPropertyConfigs = new ArtifactPropertiesConfig(
                new ArtifactPropertyConfig("foo", "//foo", "xpath"),
                new ArtifactPropertyConfig("bar", "//bar", "xpath")
        );

        final List<ArtifactPropertiesGenerator> artifactPropertiesGenerators = ArtifactPropertiesGenerator.toArtifactProperties(artifactPropertyConfigs);

        assertThat(artifactPropertiesGenerators, containsInAnyOrder(
                new ArtifactPropertiesGenerator("foo", "//foo", "xpath"),
                new ArtifactPropertiesGenerator("bar", "//bar", "xpath")
        ));
    }

    class GoPublisherImple extends DefaultGoPublisher {
        private final List<String> sentContents;
        private final List<String> sentErrors;

        public GoPublisherImple(List<String> sentContents, List<String> sentErrors) {
            super(new GoArtifactsManipulator(null, null, null) {
                public void publish(GoPublisher goPublisher, File dest, File source,
                                    JobIdentifier jobIdentifier) {
                }

                public void setProperty(JobIdentifier jobIdentifier, Property property) throws
                        ArtifactPublishingException {
                }

                public ConsoleOutputTransmitter createConsoleOutputTransmitter(JobIdentifier jobIdentifier,
                                                                               AgentIdentifier agentIdentifier, String consoleLogCharset) {
                    return null;
                }
            }, new JobIdentifier(), null, AgentRuntimeInfo.initialState(NullAgent.createNullAgent()), "utf-8");
            this.sentContents = sentContents;
            this.sentErrors = sentErrors;
        }

        public void consumeLine(String line) {
            sentContents.add(line);
        }

        @Override
        public void reportErrorMessage(String message, Exception e) {
            sentErrors.add(message);
        }
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
