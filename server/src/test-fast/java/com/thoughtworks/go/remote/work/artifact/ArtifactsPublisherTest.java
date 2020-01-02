/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.remote.work.artifact;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
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
import java.util.*;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor.Request.CONSOLE_LOG;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
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
    private PluginRequestProcessorRegistry registry;
    private final EnvironmentVariableContext env = new EnvironmentVariableContext("foo", "bar");

    @Before
    public void setUp() throws IOException {
        workingFolder = temporaryFolder.newFolder("temporaryFolder");
        artifactExtension = mock(ArtifactExtension.class);
        registry = mock(PluginRequestProcessorRegistry.class);
        publisher = new StubGoPublisher();

        artifactsPublisher = new ArtifactsPublisher(publisher, artifactExtension, new ArtifactStores(), registry, workingFolder);

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
        new DefaultJobPlan(new Resources(), artifactPlans, -1, null, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.unit, "test1", "test"));
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.unit, "test2", "test"));

        final File firstTestFolder = prepareTestFolder(workingFolder, "test1");
        final File secondTestFolder = prepareTestFolder(workingFolder, "test2");

        artifactsPublisher.publishArtifacts(artifactPlans, env);

        publisher.assertPublished(firstTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished(secondTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished("result", "testoutput");
        publisher.assertPublished("result" + File.separator + "index.html", "testoutput");
    }

    @Test
    public void shouldReportErrorWithTestArtifactSrcWhenUploadFails() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        new DefaultJobPlan(new Resources(), artifactPlans, -1, null, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.unit, "test1", "test"));
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.unit, "test2", "test"));

        prepareTestFolder(workingFolder, "test1");
        prepareTestFolder(workingFolder, "test2");

        publisher.setShouldFail(true);
        try {
            artifactsPublisher.publishArtifacts(artifactPlans, env);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to upload [test1, test2]"));
        }
    }

    @Test
    public void shouldUploadFilesCorrectly() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        TestFileUtil.createTestFile(src1, "test.txt");
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, src1.getName(), "dest"));
        final File src2 = TestFileUtil.createTestFolder(workingFolder, "src2");
        TestFileUtil.createTestFile(src1, "test.txt");

        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, src2.getName(), "test"));
        StubGoPublisher publisher = new StubGoPublisher();

        new ArtifactsPublisher(publisher, artifactExtension, new ArtifactStores(), registry, workingFolder).publishArtifacts(artifactPlans, env);

        Map<File, String> expectedFiles = new HashMap<File, String>() {
            {
                put(src1, "dest");
                put(src2, "test");
            }
        };
        assertThat(publisher.publishedFiles(), is(expectedFiles));
    }

    @Test
    public void shouldUploadFilesWhichMatchedWildCard() throws Exception {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        final File testFile1 = TestFileUtil.createTestFile(src1, "test1.txt");
        final File testFile2 = TestFileUtil.createTestFile(src1, "test2.txt");
        final File testFile3 = TestFileUtil.createTestFile(src1, "readme.pdf");
        artifactPlans.add(new ArtifactPlan(ArtifactPlanType.file, src1.getName() + "/*", "dest"));

        artifactsPublisher.publishArtifacts(artifactPlans, env);

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
        final ArtifactPlan dockerArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("test-reports", "docker", create("junit", false, "junit.xml")));

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(s3ArtifactPlan), eq(s3ArtifactStore), anyString(), eq(env)))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("src", "s3://dist")));
        when(artifactExtension.publishArtifact(eq("cd.go.docker"), eq(dockerArtifactPlan), eq(dockerArtifactStore), anyString(), eq(env)))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("image", "alpine")));

        new ArtifactsPublisher(publisher, artifactExtension, artifactStores, registry, workingFolder)
                .publishArtifacts(Arrays.asList(s3ArtifactPlan, dockerArtifactPlan), env);

        assertThat(uploadedPluggableMetadataFiles(publisher.publishedFiles()), containsInAnyOrder("cd.go.s3.json", "cd.go.docker.json"));
    }

    @Test
    public void shouldNotUploadMetadataFileWhenPublishPluggableArtifactIsUnsuccessful() {
        final ArtifactStore artifactStore = new ArtifactStore("s3", "cd.go.s3", create("Foo", false, "Bar"));
        final ArtifactStores artifactStores = new ArtifactStores(artifactStore);
        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(artifactPlan), eq(artifactStore), anyString(), eq(env))).thenThrow(new RuntimeException("something"));

        try {
            new ArtifactsPublisher(publisher, artifactExtension, artifactStores, registry, workingFolder)
                    .publishArtifacts(Arrays.asList(artifactPlan), env);
            fail("Should throw error for pluggable artifact [installers].");
        } catch (Exception e) {
            assertThat(publisher.publishedFiles().size(), is(0));
            assertThat(e.getMessage(), containsString("[go] Uploading finished. Failed to upload [installers]."));
        }

    }

    @Test
    public void shouldErrorOutWhenFailedToCreateFolderToWritePluggableArtifactMetadata() {
        assumeFalse("Do not run on windows.", IS_WINDOWS);

        final ArtifactStore artifactStore = new ArtifactStore("s3", "cd.go.s3", create("Foo", false, "Bar"));
        final ArtifactStores artifactStores = new ArtifactStores(artifactStore);
        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(artifactPlan), eq(artifactStore), anyString(), eq(env)))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("Foo", "Bar")));

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("[go] Could not create pluggable artifact metadata folder");

        workingFolder.setWritable(false);

        new ArtifactsPublisher(publisher, artifactExtension, artifactStores, registry, workingFolder)
                .publishArtifacts(Arrays.asList(artifactPlan), env);
    }

    @Test
    public void shouldContinueWithOtherPluginWhenPublishArtifactCallFailsForOnePlugin() throws IOException {
        final ArtifactStore s3ArtifactStore = new ArtifactStore("s3", "cd.go.s3", create("access_key", false, "some-key"));
        final ArtifactStore dockerArtifactStore = new ArtifactStore("docker", "cd.go.docker", create("registry-url", false, "docker.io"));
        final ArtifactStores artifactStores = new ArtifactStores(s3ArtifactStore, dockerArtifactStore);
        final ArtifactPlan s3ArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));
        final ArtifactPlan dockerArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("test-reports", "docker", create("junit", false, "junit.xml")));

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(s3ArtifactPlan), eq(s3ArtifactStore), anyString(), eq(env)))
                .thenThrow(new RuntimeException("Interaction with plugin `cd.go.s3` failed."));
        when(artifactExtension.publishArtifact(eq("cd.go.docker"), eq(dockerArtifactPlan), eq(dockerArtifactStore), anyString(), eq(env)))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("tag", "10.12.0")));
        try {
            new ArtifactsPublisher(publisher, artifactExtension, artifactStores, registry, workingFolder)
                    .publishArtifacts(Arrays.asList(s3ArtifactPlan, dockerArtifactPlan), env);
            fail("Should throw error for pluggable artifact [installers].");
        } catch (Exception e) {
            assertThat(uploadedPluggableMetadataFiles(publisher.publishedFiles()), containsInAnyOrder("cd.go.docker.json"));
            assertThat(publisher.getMessage(), containsString("[go] Interaction with plugin `cd.go.s3` failed"));
            assertThat(e.getMessage(), containsString("[go] Uploading finished. Failed to upload [installers]."));
        }
    }

    private Set<String> uploadedPluggableMetadataFiles(Map<File, String> actual) {
        final HashSet<String> filesUploaded = new HashSet<>();
        for (Map.Entry<File, String> entry : actual.entrySet()) {
            if (entry.getValue().equals("pluggable-artifact-metadata")) {
                filesUploaded.add(entry.getKey().getName());
            }
        }
        return filesUploaded;
    }

    @Test
    public void shouldAddPluggableArtifactMetadataFileArtifactPlanAtTop() throws Exception {
        TestFileUtil.createTestFile(workingFolder, "installer.zip");
        TestFileUtil.createTestFile(workingFolder, "testreports.xml");

        final ArtifactStore artifactStore = new ArtifactStore("s3", "cd.go.s3", create("Foo", false, "Bar"));
        final ArtifactStores artifactStores = new ArtifactStores(artifactStore);

        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));
        List<ArtifactPlan> artifactPlans = Arrays.asList(
                new ArtifactPlan(ArtifactPlanType.file, "installer.zip", "dist"),
                new ArtifactPlan(ArtifactPlanType.unit, "testreports.xml", "testreports"),
                artifactPlan
        );

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(artifactPlan), eq(artifactStore), anyString(), eq(env)))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("Foo", "Bar")));

        final GoPublisher publisher = mock(GoPublisher.class);

        new ArtifactsPublisher(publisher, artifactExtension, artifactStores, registry, workingFolder)
                .publishArtifacts(artifactPlans, env);

        InOrder inOrder = inOrder(publisher);
        inOrder.verify(publisher).upload(any(), eq("pluggable-artifact-metadata"));
        inOrder.verify(publisher).upload(any(), eq("dist"));
        inOrder.verify(publisher).upload(any(), eq("testreports"));
    }

    @Test
    public void shouldDeletePluggableArtifactMetadataDirectory() throws Exception {
        TestFileUtil.createTestFile(workingFolder, "installer.zip");
        TestFileUtil.createTestFile(workingFolder, "testreports.xml");

        final ArtifactStore artifactStore = new ArtifactStore("s3", "cd.go.s3", create("Foo", false, "Bar"));
        final ArtifactStores artifactStores = new ArtifactStores(artifactStore);

        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));
        List<ArtifactPlan> artifactPlans = Arrays.asList(
                new ArtifactPlan(ArtifactPlanType.file, "installer.zip", "dist"),
                new ArtifactPlan(ArtifactPlanType.unit, "testreports.xml", "testreports"),
                artifactPlan
        );

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(artifactPlan), eq(artifactStore), anyString(), eq(env)))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("Foo", "Bar")));

        final GoPublisher publisher = mock(GoPublisher.class);

        assertThat(Arrays.asList(workingFolder.list()), containsInAnyOrder("testreports.xml", "installer.zip", "cruise-output"));
        new ArtifactsPublisher(publisher, artifactExtension, artifactStores, registry, workingFolder)
                .publishArtifacts(artifactPlans, env);
        assertThat(Arrays.asList(workingFolder.list()), containsInAnyOrder("testreports.xml", "installer.zip", "cruise-output"));
    }

    @Test
    public void shouldRegisterAndDeRegisterArtifactRequestProcessBeforeAndAfterPublishingPluggableArtifact() {
        final ArtifactStore s3ArtifactStore = new ArtifactStore("s3", "cd.go.s3", create("access_key", false, "some-key"));
        final ArtifactStores artifactStores = new ArtifactStores(s3ArtifactStore);
        final ArtifactPlan s3ArtifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3", create("Baz", true, "Car")));

        when(artifactExtension.publishArtifact(eq("cd.go.s3"), eq(s3ArtifactPlan), eq(s3ArtifactStore), anyString(), eq(env)))
                .thenReturn(new PublishArtifactResponse(Collections.singletonMap("src", "s3://dist")));

        new ArtifactsPublisher(publisher, artifactExtension, artifactStores, registry, workingFolder)
                .publishArtifacts(Arrays.asList(s3ArtifactPlan), env);

        InOrder inOrder = inOrder(registry, artifactExtension);
        inOrder.verify(registry, times(1)).registerProcessorFor(eq(CONSOLE_LOG.requestName()), any(ArtifactRequestProcessor.class));
        inOrder.verify(artifactExtension, times(1))
                .publishArtifact("cd.go.s3", s3ArtifactPlan, s3ArtifactStore, workingFolder.getAbsolutePath(), env);
        inOrder.verify(registry, times(1)).removeProcessorFor(CONSOLE_LOG.requestName());
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
