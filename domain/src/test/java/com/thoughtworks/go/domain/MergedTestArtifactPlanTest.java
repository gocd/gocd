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

import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MergedTestArtifactPlanTest {
    @TempDir
    Path testFolder;

    @Mock
    private GoPublisher mockArtifactPublisher;

    @Test
    public void shouldNotThrowExceptionIfFolderNotFound() {
        final ArtifactPlan compositeTestArtifact = new MergedTestArtifactPlan(
            new ArtifactPlan(ArtifactPlanType.unit, "some_random_path_that_does_not_exist", "testoutput")
        );
        compositeTestArtifact.publishBuiltInArtifacts(mockArtifactPublisher, testFolder.toFile());
        verify(mockArtifactPublisher).taggedConsumeLineWithPrefix(GoPublisher.PUBLISH_ERR,
            String.format("The directory %s specified as a test artifact was not found. Please check your configuration",
                FilenameUtils.separatorsToUnix(testFolder.resolve("some_random_path_that_does_not_exist").toString())));
    }

    @Test
    public void shouldNotThrowExceptionIfUserSpecifiesNonFolderFileThatExistsAsSrc() throws Exception {
        Path testFile = testFolder.resolve("tempFolder/nonFolderFileThatExists");
        FileUtils.writeStringToFile(testFile.toFile(), "", StandardCharsets.UTF_8);
        final ArtifactPlan compositeTestArtifact = new MergedTestArtifactPlan(
            new ArtifactPlan(ArtifactPlanType.unit, testFile.toString(), "testoutput")
        );

        compositeTestArtifact.publishBuiltInArtifacts(mockArtifactPublisher, testFolder.toFile());
    }

    @Test
    public void shouldSupportGlobPatternsInSourcePath() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.unit, "**/*/a.log", "logs");
        MergedTestArtifactPlan testArtifactPlan = new MergedTestArtifactPlan(artifactPlan);

        File first = testFolder.resolve("report/a.log").toFile();
        File second = testFolder.resolve("test/a/b/a.log").toFile();

        first.mkdirs();
        second.mkdirs();

        testArtifactPlan.publishBuiltInArtifacts(mockArtifactPublisher, testFolder.toFile());

        verify(mockArtifactPublisher).upload(first, "logs/report");
        verify(mockArtifactPublisher).upload(second, "logs/test/a/b");
        verify(mockArtifactPublisher, times(2)).upload(any(File.class), eq("testoutput"));
    }
}
