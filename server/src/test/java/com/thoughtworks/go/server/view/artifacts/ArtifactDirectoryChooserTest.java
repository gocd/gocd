/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.view.artifacts;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.JobIdentifierMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.FileSystems;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactDirectoryChooserTest {
    JobIdentifier jobId = new JobIdentifier("pipeline-name", -2, "label-111", "stage-name", "1", "job-name", 666L);

    @TempDir
    File root1;
    @TempDir
    File root2;
    private File root1ArtifactLocation;
    private File root2ArtifactLocation;

    private ArtifactDirectoryChooser chooser;

    @BeforeEach
    public void setUp() {
        root1ArtifactLocation = new File(root1, "pipelines/pipeline-name/label-111/stage-name/1/job-name");
        root2ArtifactLocation = new File(root2, "pipelines/pipeline-name/label-111/stage-name/1/job-name");

        chooser = new ArtifactDirectoryChooser();
        chooser.add(new PathBasedArtifactsLocator(root1));
        chooser.add(new PathBasedArtifactsLocator(root2));
    }

    @Test
    public void shouldChooseFirstLocationWhereFolderExists() {
        root2ArtifactLocation.mkdirs();

        assertThat(chooser.chooseExistingRoot(jobId)).isEqualTo(root2ArtifactLocation);
    }

    @Test
    public void shouldChooseFirstLocatorForPreferredArtifactLocation() {
        assertThat(chooser.preferredRoot(jobId)).isEqualTo(root1ArtifactLocation);
    }

    @Test
    public void shouldLocateArtifactIfItExists() throws IllegalArtifactLocationException {
        root2ArtifactLocation.mkdirs();
        File file = new File(root2ArtifactLocation, "foo.txt");
        assertThat(chooser.findArtifact(jobId, "foo.txt")).isEqualTo(file);
    }

    @Test
    public void shouldLocateCachedArtifactIfItExists() {
        StageIdentifier stageIdentifier = new StageIdentifier("P1", 1, "S1", "1");
        File cachedStageFolder = new File(root2, "cache/artifacts/pipelines/P1/1/S1/1");
        cachedStageFolder.mkdirs();
        assertThat(chooser.findCachedArtifact(stageIdentifier)).isEqualTo(cachedStageFolder);
    }

    @Test
    public void shouldGivePreferredLocationIfArtifactDoesNotExist() throws IllegalArtifactLocationException {
        assertThat(chooser.findArtifact(jobId, "foo.txt")).isEqualTo(new File(root1ArtifactLocation, "foo.txt"));
    }

    @Test
    public void shouldThrowExceptionIfRequestedLocationIsOutsideArtifactDirectory() {
        String path = "../../../../../..";
        try {
            chooser.findArtifact(jobId, path);
        } catch (IllegalArtifactLocationException e) {
            assertThat(e.getMessage()).contains("Artifact path [" + path + "] is illegal.");
        }
    }

    @Test
    public void shouldReturnAUniqueLocationForConsoleFilesWithDifferentJobIdentifiers() {
        JobIdentifier jobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "1", "right");
        JobIdentifier anotherJobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "2", "now");
        assertThat(chooser.temporaryConsoleFile(jobIdentifier).getPath()).isNotEqualToIgnoringCase(chooser.temporaryConsoleFile(anotherJobIdentifier).getPath());
    }

    @Test
    public void shouldReturnASameLocationForConsoleFilesWithSimilarJobIdentifiers() {
        JobIdentifier jobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "1", "right");
        JobIdentifier anotherJobIdentifier = JobIdentifierMother.jobIdentifier("come", 1, "together", "1", "right");
        assertThat(chooser.temporaryConsoleFile(jobIdentifier).getPath()).isEqualToIgnoringCase(chooser.temporaryConsoleFile(anotherJobIdentifier).getPath());
    }

    @Test
    public void shouldFetchATemporaryConsoleOutLocation() {
        File consoleFile = chooser.temporaryConsoleFile(new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox", 0));
        String filePathSeparator = FileSystems.getDefault().getSeparator();
        assertThat(consoleFile.getPath()).isEqualTo(String.format("data%sconsole%sd0132b209429f7dc5b9ffffe87b02a7c.log", filePathSeparator, filePathSeparator));
    }
}
