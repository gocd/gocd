/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.TestArtifactPlan;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.Times;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JMock.class)
public class TestArtifactPlanTest {
    private DefaultGoPublisher mockArtifactPublisher;
    private final Mockery context = new ClassMockery();
    private File rootPath;

    @Before
    public void setup() throws IOException {
        mockArtifactPublisher = mock(DefaultGoPublisher.class);
        rootPath = new File("target/test");
        rootPath.mkdirs();
    }

    @After
    public void tearDown() {
        FileUtil.deleteFolder(rootPath);
    }

    @Test
    public void shouldNotThrowExceptionIfFolderNotFound() throws Exception {
        final TestArtifactPlan compositeTestArtifact = new TestArtifactPlan(
                new TestArtifactPlan("some_random_path_that_does_not_exist",
                        "testoutput"));
        compositeTestArtifact.publish(mockArtifactPublisher, rootPath);
        verify(mockArtifactPublisher).consumeLineWithPrefix("The Directory target/test/some_random_path_that_does_not_exist specified as a test artifact was not found. Please check your configuration");
    }

    @Test
    public void shouldNotThrowExceptionIfUserSpecifiesNonFolderFileThatExistsAsSrc() throws Exception {
        final File nonFolderFileThatExists = TestFileUtil.createTestFile(TestFileUtil.createTempFolder("tempFolder"),
                "nonFolderFileThatExists");
        final TestArtifactPlan compositeTestArtifact = new TestArtifactPlan(
                new TestArtifactPlan(nonFolderFileThatExists.getPath(),
                        "testoutput"));

        compositeTestArtifact.publish(mockArtifactPublisher, rootPath);
        doNothing().when(mockArtifactPublisher).upload(any(File.class), any(String.class));
    }

    @Test
    public void shouldSupportGlobPatternsInSourcePath() {
        ArtifactPlan artifactPlan = new ArtifactPlan( "**/*/a.log", "logs");
        TestArtifactPlan testArtifactPlan = new TestArtifactPlan(artifactPlan);

        File first = new File("target/test/report/a.log");
        File second = new File("target/test/test/a/b/a.log");

        first.mkdirs();
        second.mkdirs();

        testArtifactPlan.publish(mockArtifactPublisher, rootPath);

        verify(mockArtifactPublisher).upload(first, "logs/report");
        verify(mockArtifactPublisher).upload(second, "logs/test/a/b");
        verify(mockArtifactPublisher, new Times(2)).upload(any(File.class), eq("testoutput"));
    }

}
