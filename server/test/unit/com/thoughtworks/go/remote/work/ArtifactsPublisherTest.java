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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.StubGoPublisher;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ArtifactsPublisherTest {

    private File workingFolder;
    private File toClean;
    private ArtifactsPublisher artifactsPublisher;

    @Before
    public void setUp() throws IOException {
        artifactsPublisher = new ArtifactsPublisher();
        workingFolder = TestFileUtil.createTempFolder("workingFolder");
        File file = new File(workingFolder, "cruise-output/log.xml");
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    @After
    public void tearDown() {
        FileUtil.deleteFolder(workingFolder);
        FileUtils.deleteQuietly(toClean);
    }

    @Test
    public void shouldMergeTestReportFilesAndUploadResult() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), artifactPlans, new ArtifactPropertiesGenerators(), -1, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        artifactPlans.add(new TestArtifactPlan("test1", "test"));
        artifactPlans.add(new TestArtifactPlan("test2", "test"));

        final File firstTestFolder = prepareTestFolder(workingFolder, "test1");
        final File secondTestFolder = prepareTestFolder(workingFolder, "test2");

        StubGoPublisher publisher = new StubGoPublisher();
        artifactsPublisher.publishArtifacts(publisher, workingFolder, artifactPlans);

        publisher.assertPublished(firstTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished(secondTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished("result", "testoutput");
        publisher.assertPublished("result" + File.separator + "index.html", "testoutput");
    }

    @Test
    public void shouldReportErrorWithTestArtifactSrcWhenUploadFails() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), artifactPlans, new ArtifactPropertiesGenerators(), -1, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        artifactPlans.add(new TestArtifactPlan("test1", "test"));
        artifactPlans.add(new TestArtifactPlan("test2", "test"));

        prepareTestFolder(workingFolder, "test1");
        prepareTestFolder(workingFolder, "test2");

        StubGoPublisher publisherThatShouldFail = new StubGoPublisher(true);
        try {
            artifactsPublisher.publishArtifacts(publisherThatShouldFail, workingFolder, artifactPlans);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to upload [test1, test2]"));
        }
    }

    @Test
    public void shouldUploadFilesCorrectly() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        TestFileUtil.createTestFile(src1, "test.txt");
        artifactPlans.add(new ArtifactPlan(src1.getName(), "dest"));
        final File src2 = TestFileUtil.createTestFolder(workingFolder, "src2");
        TestFileUtil.createTestFile(src1, "test.txt");

        artifactPlans.add(new ArtifactPlan(src2.getName(), "test"));
        StubGoPublisher publisher = new StubGoPublisher();

        artifactsPublisher.publishArtifacts(publisher, workingFolder, artifactPlans);

        Map<File, String> expectedFiles = new HashMap<File, String>() {
            {
                put(src1, "dest");
                put(src2, "test");
            }
        };
        assertThat(publisher.publishedFiles(), is(expectedFiles));
    }

    @Test
    public void shouldUploadFilesWhichMathedWildCard() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        final File testFile1 = TestFileUtil.createTestFile(src1, "test1.txt");
        final File testFile2 = TestFileUtil.createTestFile(src1, "test2.txt");
        final File testFile3 = TestFileUtil.createTestFile(src1, "readme.pdf");
        artifactPlans.add(new ArtifactPlan(src1.getName() + "/*", "dest"));
        StubGoPublisher publisher = new StubGoPublisher();

        artifactsPublisher.publishArtifacts(publisher, workingFolder, artifactPlans);

        Map<File, String> expectedFiles = new HashMap<File, String>() {
            {
                put(testFile1, "dest");
                put(testFile2, "dest");
                put(testFile3, "dest");
            }
        };
        assertThat(publisher.publishedFiles(), is(expectedFiles));
    }

    private File prepareTestFolder(File workingFolder, String folderName) throws Exception {
        File testFolder = TestFileUtil.createTestFolder(workingFolder, folderName);
        File testFile = TestFileUtil.createTestFile(testFolder, "testFile.xml");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<testsuite errors=\"0\" failures=\"0\" tests=\"7\" time=\"0.429\" >\n"
                + "<testcase/>\n"
                + "</testsuite>\n";
        FileUtils.writeStringToFile(testFile, content, StandardCharsets.UTF_8);
        return testFolder;
    }
}
