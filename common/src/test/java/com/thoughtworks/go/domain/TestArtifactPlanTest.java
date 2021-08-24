/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.internal.verification.Times;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.mockito.Mockito.*;

public class TestArtifactPlanTest {
    @TempDir
    Path temporaryFolder;

    private DefaultGoPublisher mockArtifactPublisher;
    private Path rootPath;

    @BeforeEach
    public void setup() throws IOException {
        mockArtifactPublisher = mock(DefaultGoPublisher.class);
        rootPath = TempDirUtils.createTempDirectoryIn(temporaryFolder, "test");
    }

    @Test
    public void shouldNotThrowExceptionIfFolderNotFound() throws Exception {
        final MergedTestArtifactPlan compositeTestArtifact = new MergedTestArtifactPlan(
                new ArtifactPlan(ArtifactPlanType.unit, "some_random_path_that_does_not_exist", "testoutput")
        );
        compositeTestArtifact.publishBuiltInArtifacts(mockArtifactPublisher, rootPath.toFile());
        verify(mockArtifactPublisher).taggedConsumeLineWithPrefix(DefaultGoPublisher.PUBLISH_ERR,
                String.format("The Directory %s specified as a test artifact was not found. Please check your configuration",
                        FilenameUtils.separatorsToUnix(rootPath.resolve("some_random_path_that_does_not_exist").toString())));
    }

    @Test
    public void shouldNotThrowExceptionIfUserSpecifiesNonFolderFileThatExistsAsSrc() throws Exception {
        Path testFile = temporaryFolder.resolve("tempFolder/nonFolderFileThatExists");
        FileUtils.writeStringToFile(testFile.toFile(), "", StandardCharsets.UTF_8);
        final ArtifactPlan compositeTestArtifact = new ArtifactPlan(
                new ArtifactPlan(ArtifactPlanType.unit, testFile.toString(), "testoutput")
        );

        compositeTestArtifact.publishBuiltInArtifacts(mockArtifactPublisher, rootPath.toFile());
        doNothing().when(mockArtifactPublisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldSupportGlobPatternsInSourcePath() throws IOException {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.unit, "**/*/a.log", "logs");
        MergedTestArtifactPlan testArtifactPlan = new MergedTestArtifactPlan(artifactPlan);

        File first = rootPath.resolve("report/a.log").toFile();
        File second = rootPath.resolve("test/a/b/a.log").toFile();

        first.mkdirs();
        second.mkdirs();

        testArtifactPlan.publishBuiltInArtifacts(mockArtifactPublisher, rootPath.toFile());

        verify(mockArtifactPublisher).upload(first, "logs/report");
        verify(mockArtifactPublisher).upload(second, "logs/test/a/b");
        verify(mockArtifactPublisher, new Times(2)).upload(any(File.class), eq("testoutput"));
    }

}
