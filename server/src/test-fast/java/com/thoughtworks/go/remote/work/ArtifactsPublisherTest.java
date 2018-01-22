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

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ArtifactsPublisherTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    private File workingFolder;
    private ArtifactsPublisher artifactsPublisher;
    private ArtifactExtension artifactExtension;
    private StubGoPublisher publisher;

    @Before
    public void setUp() throws IOException {
        artifactExtension = mock(ArtifactExtension.class);
        publisher = new StubGoPublisher();

        artifactsPublisher = new ArtifactsPublisher(artifactExtension, new ArtifactStores(), null);

        workingFolder = temporaryFolder.newFolder("temporaryFolder");
        File file = new File(workingFolder, "cruise-output/log.xml");
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    @After
    public void tearDown() throws Exception {
        workingFolder.setWritable(true);
        workingFolder.delete();
    }

    @Test
    public void shouldMergeTestReportFilesAndUploadResult() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        new DefaultJobPlan(new Resources(), artifactPlans, new ArrayList<>(), -1, null, null, new EnvironmentVariables(), new EnvironmentVariables(), null);
        artifactPlans.add(new ArtifactPlan(ArtifactType.unit, "test1", "test"));
        artifactPlans.add(new ArtifactPlan(ArtifactType.unit, "test2", "test"));

        final File firstTestFolder = prepareTestFolder(workingFolder, "test1");
        final File secondTestFolder = prepareTestFolder(workingFolder, "test2");

        artifactsPublisher.publishArtifacts(publisher, workingFolder, artifactPlans);

        publisher.assertPublished(firstTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished(secondTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished("result", "testoutput");
        publisher.assertPublished("result" + File.separator + "index.html", "testoutput");
    }

    @Test
    public void shouldReportErrorWithTestArtifactSrcWhenUploadFails() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        new DefaultJobPlan(new Resources(), artifactPlans, new ArrayList<>(), -1, null, null, new EnvironmentVariables(), new EnvironmentVariables(), null);
        artifactPlans.add(new ArtifactPlan(ArtifactType.unit, "test1", "test"));
        artifactPlans.add(new ArtifactPlan(ArtifactType.unit, "test2", "test"));

        prepareTestFolder(workingFolder, "test1");
        prepareTestFolder(workingFolder, "test2");

        publisher.setShouldFail(true);
        try {
            artifactsPublisher.publishArtifacts(publisher, workingFolder, artifactPlans);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to upload [test1, test2]"));
        }
    }

    @Test
    public void shouldUploadFilesCorrectly() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        TestFileUtil.createTestFile(src1, "test.txt");
        artifactPlans.add(new ArtifactPlan(ArtifactType.file, src1.getName(), "dest"));
        final File src2 = TestFileUtil.createTestFolder(workingFolder, "src2");
        TestFileUtil.createTestFile(src1, "test.txt");

        artifactPlans.add(new ArtifactPlan(ArtifactType.file, src2.getName(), "test"));
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
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        final File testFile1 = TestFileUtil.createTestFile(src1, "test1.txt");
        final File testFile2 = TestFileUtil.createTestFile(src1, "test2.txt");
        final File testFile3 = TestFileUtil.createTestFile(src1, "readme.pdf");
        artifactPlans.add(new ArtifactPlan(ArtifactType.file, src1.getName() + "/*", "dest"));

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

    @Test
    public void shouldPublishPluggableArtifactsAndUploadMetadataFileToServer() throws IOException {
        final ArtifactStore s3ArtifactStore = new ArtifactStore("s3", "cd.go.s3", create("access_key", false, "some-key"));
        final ArtifactStore dockerArtifactStore = new ArtifactStore("docker", "cd.go.docker", create("registry-url", false, "docker.io"));
        final ArtifactStores artifactStores = new ArtifactStores(s3ArtifactStore, dockerArtifactStore);
        final ArtifactPlan s3ArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));
        final ArtifactPlan dockerArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("test-reports", "s3", create("junit", false, "junit.xml")));

        final ArtifactsPublisher artifactsPublisher = new ArtifactsPublisher(artifactExtension, artifactStores, null);

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(s3ArtifactPlan), eq(s3ArtifactStore), anyString()))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("src", "s3://dist"), new ArrayList<>()));
        when(artifactExtension.publishArtifact(eq("cd.go.docker"), eq(dockerArtifactPlan), eq(dockerArtifactStore), anyString()))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("image", "alpine"), new ArrayList<>()));

        artifactsPublisher.publishArtifacts(publisher, workingFolder, Arrays.asList(s3ArtifactPlan, dockerArtifactPlan));


        final File fileUploaded = getFileUploaded("cd.go.s3.json");

        assertThat(publisher.publishedFiles(), is(Collections.singletonMap(fileUploaded, "pluggable-artifact-metadata")));
        assertThat(FileUtils.readFileToString(fileUploaded, StandardCharsets.UTF_8), is("{\"installers\":{\"src\":\"s3://dist\"}}"));
    }

    @Test
    public void shouldNotUploadMetadataFileWhenPublishPluggableArtifactIsUnsuccessful() {
        final ArtifactStore artifactStore = new ArtifactStore("s3", "cd.go.s3", create("Foo", false, "Bar"));
        final ArtifactStores artifactStores = new ArtifactStores(artifactStore);
        final ArtifactsPublisher artifactsPublisher = new ArtifactsPublisher(artifactExtension, artifactStores, null);
        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(artifactPlan), eq(artifactStore), anyString())).thenThrow(new RuntimeException("something"));

        artifactsPublisher.publishArtifacts(publisher, workingFolder, Arrays.asList(artifactPlan));

        assertThat(publisher.publishedFiles().size(), is(0));
    }

    @Test
    public void shouldErrorOutWhenFailedToCreateFolderToWritePluggableArtifactMetadata() {
        assumeFalse("Do not run on windows.", IS_WINDOWS);

        final ArtifactStore artifactStore = new ArtifactStore("s3", "cd.go.s3", create("Foo", false, "Bar"));
        final ArtifactStores artifactStores = new ArtifactStores(artifactStore);
        final ArtifactsPublisher artifactsPublisher = new ArtifactsPublisher(artifactExtension, artifactStores, null);
        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(artifactPlan), eq(artifactStore), anyString())).thenReturn(new PublishArtifactResponse(Collections.singletonMap("Foo", "Bar"), Arrays.asList("some-error")));

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("[go] Could not create pluggable artifact metadata folder");

        workingFolder.setWritable(false);

        artifactsPublisher.publishArtifacts(publisher, workingFolder, Arrays.asList(artifactPlan));
    }

    @Test
    public void shouldContinueWithOtherPluginWhenPublishArtifactCallFailsForOnePlugin() throws IOException {
        final ArtifactStore s3ArtifactStore = new ArtifactStore("s3", "cd.go.s3", create("access_key", false, "some-key"));
        final ArtifactStore dockerArtifactStore = new ArtifactStore("docker", "cd.go.docker", create("registry-url", false, "docker.io"));
        final ArtifactStores artifactStores = new ArtifactStores(s3ArtifactStore, dockerArtifactStore);
        final ArtifactPlan s3ArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));
        final ArtifactPlan dockerArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("test-reports", "docker", create("junit", false, "junit.xml")));

        final ArtifactsPublisher artifactsPublisher = new ArtifactsPublisher(artifactExtension, artifactStores, null);

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(s3ArtifactPlan), eq(s3ArtifactStore), anyString())).thenThrow(new RuntimeException("Interaction with plugin `cd.go.s3` failed."));
        when(artifactExtension.publishArtifact(eq("cd.go.docker"), eq(dockerArtifactPlan), eq(dockerArtifactStore), anyString())).thenReturn(new PublishArtifactResponse(Collections.singletonMap("tag", "10.12.0"), new ArrayList<>()));

        artifactsPublisher.publishArtifacts(publisher, workingFolder, Arrays.asList(s3ArtifactPlan, dockerArtifactPlan));

        final File fileUploaded = getFileUploaded("cd.go.docker.json");

        assertThat(publisher.publishedFiles(), is(Collections.singletonMap(fileUploaded, "pluggable-artifact-metadata")));
        assertThat(FileUtils.readFileToString(fileUploaded, StandardCharsets.UTF_8), is("{\"test-reports\":{\"tag\":\"10.12.0\"}}"));

        assertThat(publisher.getMessage(), containsString("[go] Interaction with plugin `cd.go.s3` failed"));
    }

    private File getFileUploaded(String s) throws IOException {
        return Files
                .walk(Paths.get(workingFolder.toURI()))
                .filter(new Predicate<Path>() {
                    @Override
                    public boolean test(Path file) {
                        return file.toFile().getName().equals(s);
                    }
                })
                .findFirst().get().toFile();
    }

    @Test
    public void shouldAddPluggableArtifactMetadataFileArtifactPlanAtTop() throws Exception {
        final ArtifactStore artifactStore = new ArtifactStore("s3", "cd.go.s3", create("Foo", false, "Bar"));
        final ArtifactStores artifactStores = new ArtifactStores(artifactStore);
        final ArtifactsPublisher artifactsPublisher = new ArtifactsPublisher(artifactExtension, artifactStores, null);
        TestFileUtil.createTestFile(workingFolder, "installer.zip");
        TestFileUtil.createTestFile(workingFolder, "testreports.xml");

        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));
        List<ArtifactPlan> artifactPlans = Arrays.asList(
                new ArtifactPlan(ArtifactType.file, "installer.zip", "dist"),
                new ArtifactPlan(ArtifactType.unit, "testreports.xml", "testreports"),
                artifactPlan
        );

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(artifactPlan), eq(artifactStore), anyString())).thenReturn(new PublishArtifactResponse(Collections.singletonMap("Foo", "Bar"), new ArrayList<>()));

        final GoPublisher publisher = mock(GoPublisher.class);

        artifactsPublisher.publishArtifacts(publisher, workingFolder, artifactPlans);

        InOrder inOrder = inOrder(publisher);
        inOrder.verify(publisher).upload(any(), eq("pluggable-artifact-metadata"));
        inOrder.verify(publisher).upload(any(), eq("dist"));
        inOrder.verify(publisher).upload(any(), eq("testreports"));
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
