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

import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.internal.verification.Times;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class TestArtifactPlanTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DefaultGoPublisher mockArtifactPublisher;
    private File rootPath;

    @BeforeEach
    public void setup() throws IOException {
        temporaryFolder.create();
        mockArtifactPublisher = mock(DefaultGoPublisher.class);
        rootPath = new File("target/test");
        rootPath.mkdirs();
    }

    @AfterEach
    public void tearDown() {
        temporaryFolder.delete();
        FileUtils.deleteQuietly(rootPath);
    }

    @Test
    public void shouldNotThrowExceptionIfFolderNotFound() throws Exception {
        final MergedTestArtifactPlan compositeTestArtifact = new MergedTestArtifactPlan(
                new ArtifactPlan(ArtifactPlanType.unit, "some_random_path_that_does_not_exist", "testoutput")
        );
        compositeTestArtifact.publishBuiltInArtifacts(mockArtifactPublisher, rootPath);
        verify(mockArtifactPublisher).taggedConsumeLineWithPrefix(DefaultGoPublisher.PUBLISH_ERR,
                "The Directory target/test/some_random_path_that_does_not_exist specified as a test artifact was not found. Please check your configuration");
    }

    @Test
    public void shouldNotThrowExceptionIfUserSpecifiesNonFolderFileThatExistsAsSrc() throws Exception {
        temporaryFolder.newFolder("tempFolder");
        File nonFolderFileThatExists = temporaryFolder.newFile("tempFolder/nonFolderFileThatExists");
        final ArtifactPlan compositeTestArtifact = new ArtifactPlan(
                new ArtifactPlan(ArtifactPlanType.unit, nonFolderFileThatExists.getPath(), "testoutput")
        );

        compositeTestArtifact.publishBuiltInArtifacts(mockArtifactPublisher, rootPath);
        doNothing().when(mockArtifactPublisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldSupportGlobPatternsInSourcePath() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.unit, "**/*/a.log", "logs");
        MergedTestArtifactPlan testArtifactPlan = new MergedTestArtifactPlan(artifactPlan);

        File first = new File("target/test/report/a.log");
        File second = new File("target/test/test/a/b/a.log");

        first.mkdirs();
        second.mkdirs();

        testArtifactPlan.publishBuiltInArtifacts(mockArtifactPublisher, rootPath);

        verify(mockArtifactPublisher).upload(first, "logs/report");
        verify(mockArtifactPublisher).upload(second, "logs/test/a/b");
        verify(mockArtifactPublisher, new Times(2)).upload(any(File.class), eq("testoutput"));
    }

}
