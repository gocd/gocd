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

package com.thoughtworks.go.server.web;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * This class exists because I have no idea what the ArtifactFolder is supposed to do :-(
 */
@Ignore("CS/DY - we will fix these tests after we fix an artifact-not-showing-up bug")
public class ArtifactFolderTest {
    private File folder;
    private static final JobIdentifier JOB_IDENTIFIER
            = new JobIdentifier("pipeline-name", "label-111", "stage-name", 1, "job-name", 666L);
    private static final String PATH = "pathRelativeToBaseURL";
    private ArtifactFolder artifactFolder;

    @Before
    public void setUp() {
        folder = TestFileUtil.createTempFolder("ArtifactFolderTest" + System.currentTimeMillis());
        artifactFolder = new ArtifactFolder(JOB_IDENTIFIER, folder, PATH);
    }

    @After
    public void tearDown() {
        FileUtil.deleteFolder(folder);
    }

    @Test
    public void shouldRenderNoArtifactsFoundIfDirectoryDoesNotExist() {
        assertThat(artifactFolder.renderArtifactFiles("http://uri"), containsString("No artifacts found."));
    }

    @Test
    public void shouldRenderFileRelativeToFolderIfItExists() throws IOException {
        File file = new File(folder, "foo.xml");
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, "FOO");

        assertThat(
                artifactFolder.renderArtifactFiles("http://uri"),
                containsString(">\nfoo.xml\n</a>"));
        assertThat(
                artifactFolder.renderArtifactFiles("http://uri"),
                containsString(
                        "<a href=\"http://uri/files/pipeline-name/label-111/"
                                + "stage-name/1/job-name/pathRelativeToBaseURL/foo.xml\">"));
    }

    @Test
    public void shouldRenderDirectoryIfItExists() throws IOException {
        File file = new File(new File(folder, "dir1"), "foo.txt");
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, "FOO");
        assertThat(
                artifactFolder.renderArtifactFiles("http://uri"),
                containsString(">\nfoo.txt\n</a>"));
        assertThat(
                artifactFolder.renderArtifactFiles("http://uri"),
                containsString(
                        "<a href=\"http://uri/files/pipeline-name/label-111/"
                                + "stage-name/1/job-name/pathRelativeToBaseURL/dir1/foo.txt\">"));
    }

    @Test
    public void shouldRenderNoArtifactsFoundInJson() {
        assertThat(artifactFolder.toJson().toString(), containsString("[  ]"));
    }

    @Test
    public void shouldRenderFileRelativeToFolderInJsonIfItExists() throws IOException {
        File file = new File(folder, "foo.xml");
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, "FOO");

        assertThat(
                artifactFolder.toJson().toString(),
                containsString(
                        "{ \"name\" : \"foo.xml\", \"url\" : \"/files/pipeline-name/label-111/stage-name"
                                + "/1/job-name/pathRelativeToBaseURL/foo.xml\", \"type\" : \"file\" }"));
    }

    @Test
    public void shouldRenderDirectoryIfItExistsInJson() throws IOException {
        File file = new File(new File(folder, "dir1"), "foo.txt");
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, "FOO");

        assertThat(
                artifactFolder.toJson().toString(),
                containsString(
                        "{ \"name\" : \"foo.txt\", \"url\" : \"/files/pipeline-name/label-111/stage-name"
                                + "/1/job-name/pathRelativeToBaseURL/dir1/foo.txt\", \"type\" : \"file\" }"));
    }
}
