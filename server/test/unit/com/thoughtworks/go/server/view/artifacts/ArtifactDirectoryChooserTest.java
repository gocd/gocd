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

package com.thoughtworks.go.server.view.artifacts;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.JobIdentifierMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class ArtifactDirectoryChooserTest {
    JobIdentifier jobId = new JobIdentifier("pipeline-name", -2, "label-111", "stage-name", "1", "job-name", 666L);

    private File root1;
    private File root2;
    private File root1ArtifactLocation;
    private File root2ArtifactLocation;

    private ArtifactDirectoryChooser chooser;

    @Before
    public void setUp() {
        root1 = TestFileUtil.createTempFolder("root1");
        root2 = TestFileUtil.createTempFolder("root2");

        root1ArtifactLocation = new File(root1, "pipelines/pipeline-name/label-111/stage-name/1/job-name");
        root2ArtifactLocation = new File(root2, "pipelines/pipeline-name/label-111/stage-name/1/job-name");

        chooser = new ArtifactDirectoryChooser();
        chooser.add(new PathBasedArtifactsLocator(root1));
        chooser.add(new PathBasedArtifactsLocator(root2));
    }

    @After
    public void removeTestDirectories() {
        if (root1.exists()) {
            FileUtil.deleteFolder(root1);
        }
        if (root2.exists()) {
            FileUtil.deleteFolder(root2);
        }
    }

    @Test
    public void shouldChooseFirstLocationWhereFolderExists() {
        root2ArtifactLocation.mkdirs();

        assertThat(chooser.chooseExistingRoot(jobId), is(root2ArtifactLocation));
    }

    @Test
    public void shouldChooseFirstLocatorForPreferredAtifactLocation() {
        assertThat(chooser.preferredRoot(jobId), is(root1ArtifactLocation));
    }

    @Test
    public void shouldLocateArtifactIfItExists() throws IllegalArtifactLocationException {
        root2ArtifactLocation.mkdirs();
        File file = new File(root2ArtifactLocation, "foo.txt");
        assertThat(chooser.findArtifact(jobId, "foo.txt"), is(file));
    }

    @Test
    public void shouldLocateCachedArtifactIfItExists() throws IllegalArtifactLocationException {
        StageIdentifier stageIdentifier = new StageIdentifier("P1", 1, "S1", "1");
        File cachedStageFolder = new File(root2, "cache/artifacts/pipelines/P1/1/S1/1");
        cachedStageFolder.mkdirs();
        assertThat(chooser.findCachedArtifact(stageIdentifier), is(cachedStageFolder));
    }

    @Test
    public void shouldGivePreferredLocationIfArtifactDoesNotExist() throws IllegalArtifactLocationException {
        assertThat(chooser.findArtifact(jobId, "foo.txt"), is(new File(root1ArtifactLocation, "foo.txt")));
    }

    @Test
    public void shouldThrowExceptionIfRequestedLocationIsOutsideArtifactDirectory() {
        String path = "../../../../../..";
        try {
            chooser.findArtifact(jobId, path);
        } catch (IllegalArtifactLocationException e) {
            assertThat(e.getMessage(), containsString("Artifact path [" + path + "] is illegal."));
        }
    }

    @Test
    public void shouldReturnAUniqueLocationForConsoleFilesWithDifferentJobIdentifiers() throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "1", "right");
        JobIdentifier anotherJobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "2", "now");
        assertThat(chooser.temporaryConsoleFile(jobIdentifier).getPath(),
                not(equalToIgnoringCase(chooser.temporaryConsoleFile(anotherJobIdentifier).getPath())));
    }

    @Test
    public void shouldReturnASameLocationForConsoleFilesWithSimilarJobIdentifiers() throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "1", "right");
        JobIdentifier anotherJobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "1", "right");
        assertThat(chooser.temporaryConsoleFile(jobIdentifier).getPath(),
                equalToIgnoringCase(chooser.temporaryConsoleFile(anotherJobIdentifier).getPath()));
    }

    @Test
    public void shouldFetchATemporaryConsoleOutLocation() throws Exception {
        File consoleFile = chooser.temporaryConsoleFile(new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox", null));
        String filePathSeparator = System.getProperty("file.separator");
        assertThat(consoleFile.getPath(), is(String.format("data%sconsole%sd0132b209429f7dc5b9ffffe87b02a7c.log", filePathSeparator, filePathSeparator)));
    }
}
