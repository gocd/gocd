/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.LocatableEntity;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.JobIdentifierMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.LogFile;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.server.service.ArtifactsService.LOG_XML_NAME;
import static com.thoughtworks.go.util.GoConstants.PUBLISH_MAX_RETRIES;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.*;

@RunWith(JunitExtRunner.class)
public class ArtifactsServiceTest {
    private SystemService systemService;
    private ArtifactsDirHolder artifactsDirHolder;
    private ZipUtil zipUtil;
    private List<File> resourcesToBeCleanedOnTeardown = new ArrayList<File>();
    private File fakeRoot;
    private JobResolverService resolverService;
    private StageDao stageService;
    private LogFixture logFixture;

    @Before
    public void setUp() {
        systemService = mock(SystemService.class);
        artifactsDirHolder = mock(ArtifactsDirHolder.class);
        zipUtil = mock(ZipUtil.class);
        resolverService = mock(JobResolverService.class);
        stageService = mock(StageDao.class);

        fakeRoot = TestFileUtil.createTempFolder("ArtifactsServiceTest");
        logFixture = LogFixture.startListening();
    }

    @After
    public void tearDown() {
        for (File resource : resourcesToBeCleanedOnTeardown) {
            FileUtils.deleteQuietly(resource);
        }
        logFixture.stopListening();
    }

    @Test
    public void shouldThrowArtifactsParseExceptionWhenCannotParse() throws IOException {
        final File tempFolder = TestFileUtil.createTempFolder("tempFolder");
        resourcesToBeCleanedOnTeardown.add(tempFolder);
        final LogFile logFile = new LogFile(tempFolder, "logFile");
        String invalidXml = "<xml></wrongClosingTag>";
        FileUtils.writeStringToFile(logFile.getFile(), invalidXml);

        assumeArtifactsRoot(new File("logs"));
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        try {
            artifactsService.parseLogFile(logFile, true);
            fail();
        } catch (ArtifactsParseException e) {
            assertThat(e.getMessage(), containsString("Error parsing log file:"));
            assertThat(e.getMessage(), containsString(logFile.getPath()));
        }
    }

    @Test
    public void shouldUnzipWhenFileIsZip() throws Exception {
        final File logsDir = new File("logs");
        final ByteArrayInputStream stream = new ByteArrayInputStream("".getBytes());
        String buildInstanceId = "1";
        final File destFile = new File(logsDir, buildInstanceId + File.separator + LOG_XML_NAME);

        assumeArtifactsRoot(logsDir);
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.saveFile(destFile.getParentFile(), stream, true, 1);

        Mockito.verify(zipUtil).unzip(any(ZipInputStream.class), eq(destFile.getParentFile()));
    }

    @Test
    public void shouldNotSaveArtifactWhenItsAZipContainingDirectoryTraversalPath() throws URISyntaxException, IOException {
        final File logsDir = new File("logs");

        final ByteArrayInputStream stream = new ByteArrayInputStream(FileUtils.readFileToByteArray(new File(getClass().getResource("/archive_traversal_attack.zip").toURI())));
        String buildInstanceId = "1";
        final File destFile = new File(logsDir, buildInstanceId + File.separator + LOG_XML_NAME);
        assumeArtifactsRoot(logsDir);
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, new ZipUtil(), systemService);
        boolean saved = artifactsService.saveFile(destFile, stream, true, 1);
        assertThat(saved, is(false));
    }

    @Test
    public void shouldSaveFileInSpecifiedDirInRootFolder() throws IOException {
        final File logsDir = new File("logs");
        final ByteArrayInputStream stream = new ByteArrayInputStream("".getBytes());
        String buildInstanceId = "1";
        final File destFile = new File(logsDir, buildInstanceId + File.separator + LOG_XML_NAME);
        assumeArtifactsRoot(logsDir);
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.saveFile(destFile, stream, false, 1);

        Mockito.verify(systemService).streamToFile(eq(stream), eq(destFile));
    }

    @Test
    public void shouldSaveFileInSpecifiedDirInSpecificDest() throws IOException {
        final File logsDir = new File("logs");
        final ByteArrayInputStream stream = new ByteArrayInputStream("".getBytes());
        String buildInstanceId = "1";
        final File destFile = new File(logsDir,
                buildInstanceId + File.separator + "generated" + File.separator + LOG_XML_NAME);
        assumeArtifactsRoot(logsDir);

        String fileName = "generated" + File.separator + LOG_XML_NAME;
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.saveFile(destFile, stream, false, 1);

        Mockito.verify(systemService).streamToFile(eq(stream), eq(destFile));
    }

    @Test
    public void shouldWarnIfFailedToSaveFileWhenAttemptIsBelowMaxAttempts() throws IOException {
        final File logsDir = new File("logs");
        final ByteArrayInputStream stream = new ByteArrayInputStream("".getBytes());
        String buildInstanceId = "1";
        final File destFile = new File(logsDir,
                buildInstanceId + File.separator + "generated" + File.separator + LOG_XML_NAME);
        final IOException ioException = new IOException();

        assumeArtifactsRoot(logsDir);
        doThrow(ioException).when(zipUtil).unzip(Mockito.any(ZipInputStream.class), Mockito.any(File.class));

        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);

        artifactsService.saveFile(destFile, stream, true, 1);

        assertThat(logFixture.allLogs(), containsString("Failed to save the file to:"));
    }

    @Test
    public void shouldLogErrorIfFailedToSaveFileWhenAttemptHitsMaxAttempts() throws IOException {
        final File logsDir = new File("logs");
        final ByteArrayInputStream stream = new ByteArrayInputStream("".getBytes());
        String buildInstanceId = "1";
        final File destFile = new File(logsDir,
                buildInstanceId + File.separator + "generated" + File.separator + LOG_XML_NAME);
        final IOException ioException = new IOException();

        Mockito.doThrow(ioException).when(zipUtil).unzip(any(ZipInputStream.class), any(File.class));

        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);

        artifactsService.saveFile(destFile, stream, true, PUBLISH_MAX_RETRIES);

        assertThat(logFixture.allLogs(), containsString("Failed to save the file to:"));
    }

    @Test
    public void shouldConvertArtifactPathToFileSystemLocation() throws Exception {
        assumeArtifactsRoot(new File("artifact-root"));
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        File location = artifactsService.getArtifactLocation("foo/bar/baz");
        assertThat(location, is(new File("artifact-root/foo/bar/baz")));
    }

    @Test
    public void shouldConvertArtifactPathToUrl() throws Exception {
        assumeArtifactsRoot(new File("artifact-root"));
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        JobIdentifier identifier = JobIdentifierMother.jobIdentifier("p", 1, "s", "2", "j");
        when(resolverService.actualJobIdentifier(identifier)).thenReturn(identifier);

        String url = artifactsService.findArtifactUrl(identifier);
        assertThat(url, is("/files/p/1/s/2/j"));
    }

    @Test
    public void shouldConvertArtifactPathWithLocationToUrl() throws Exception {
        assumeArtifactsRoot(new File("artifact-root"));
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        JobIdentifier identifier = JobIdentifierMother.jobIdentifier("p", 1, "s", "2", "j");
        when(resolverService.actualJobIdentifier(identifier)).thenReturn(identifier);

        String url = artifactsService.findArtifactUrl(identifier, "console.log");
        assertThat(url, is("/files/p/1/s/2/j/console.log"));
    }

    @Test
    public void shouldUsePipelineCounterAsFolderName() throws IllegalArtifactLocationException, IOException {
        assumeArtifactsRoot(new File("artifact-root"));
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.initialize();
        File artifact = artifactsService.findArtifact(
                new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox", null), "pkg.zip");
        assertThat(artifact, is(new File("artifact-root/pipelines/cruise/1/dev/2/linux-firefox/pkg.zip")));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldProvideArtifactRootForAJobOnLinux() throws Exception {
        assumeArtifactsRoot(fakeRoot);
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.initialize();
        JobIdentifier oldId = new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox", null);
        when(resolverService.actualJobIdentifier(oldId)).thenReturn(new JobIdentifier("cruise", 2, "2.2", "functional", "3", "mac-safari"));
        String artifactRoot = artifactsService.findArtifactRoot(oldId);
        assertThat(artifactRoot, is("pipelines/cruise/2/functional/3/mac-safari"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldProvideArtifactRootForAJobOnWindows() throws Exception {
        assumeArtifactsRoot(fakeRoot);
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.initialize();
        JobIdentifier oldId = new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox", null);
        when(resolverService.actualJobIdentifier(oldId)).thenReturn(new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox", null));
        String artifactRoot = artifactsService.findArtifactRoot(oldId);
        assertThat(artifactRoot, is("pipelines\\cruise\\1\\dev\\2\\linux-firefox"));
    }

    @Test
    public void shouldProvideArtifactUrlForAJob() throws Exception {
        assumeArtifactsRoot(fakeRoot);
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        JobIdentifier oldId = new JobIdentifier("cruise", 1, "1.1", "dev", "2", "linux-firefox");
        when(resolverService.actualJobIdentifier(oldId)).thenReturn(new JobIdentifier("cruise", 2, "2.2", "functional", "3", "windows-ie"));
        String artifactUrl = artifactsService.findArtifactUrl(oldId);
        assertThat(artifactUrl, is("/files/cruise/2/functional/3/windows-ie"));
    }

    @Test
    public void shouldUsePipelineLabelAsFolderNameIfNoCounter() throws IllegalArtifactLocationException, IOException {
        File artifactsRoot = new File("artifact-root");
        assumeArtifactsRoot(artifactsRoot);
        willCleanUp(artifactsRoot);
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.initialize();
        File artifact = artifactsService.findArtifact(new JobIdentifier("cruise", -2, "1.1", "dev", "2", "linux-firefox", null), "pkg.zip");
        assertThat(artifact, is(new File("artifact-root/pipelines/cruise/1.1/dev/2/linux-firefox/pkg.zip")));
    }

    @Test
    public void shouldPurgeArtifactsExceptCruiseOutputForGivenStageAndMarkItCleaned() throws IOException {
        File artifactsRoot = new File("artifact-root");
        assumeArtifactsRoot(artifactsRoot);
        willCleanUp(artifactsRoot);
        File jobDir = new File("artifact-root/pipelines/pipeline/10/stage/20/job");
        jobDir.mkdirs();
        File aFile = new File(jobDir, "foo");
        FileUtil.writeContentToFile("hello world", aFile);
        File aDirectory = new File(jobDir, "bar");
        aDirectory.mkdir();
        File anotherFile = new File(aDirectory, "baz");
        FileUtil.writeContentToFile("quux", anotherFile);

        File cruiseOutputDir = new File(jobDir, "cruise-output");
        cruiseOutputDir.mkdir();
        File consoleLog = new File(cruiseOutputDir, "console.log");
        FileUtil.writeContentToFile("Build Logs", consoleLog);
        File checksumFile = new File(cruiseOutputDir, "md5.checksum");
        FileUtil.writeContentToFile("foo:25463254625346", checksumFile);


        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.initialize();
        Stage stage = StageMother.createPassedStage("pipeline", 10, "stage", 20, "job", new Date());
        artifactsService.purgeArtifactsForStage(stage);

        assertThat(jobDir.exists(), is(true));
        assertThat(aFile.exists(), is(false));
        assertThat(anotherFile.exists(), is(false));
        assertThat(aDirectory.exists(), is(false));

        assertThat(new File("artifact-root/pipelines/pipeline/10/stage/20/job/cruise-output/console.log").exists(), is(true));
        assertThat(new File("artifact-root/pipelines/pipeline/10/stage/20/job/cruise-output/md5.checksum").exists(), is(true));

        verify(stageService).markArtifactsDeletedFor(stage);
    }

    @Test
    public void shouldPurgeCachedArtifactsForGivenStageWhilePurgingArtifactsForAStage() throws IOException {
        File artifactsRoot = new File("artifact-root");
        assumeArtifactsRoot(artifactsRoot);
        willCleanUp(artifactsRoot);

        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        artifactsService.initialize();
        Stage stage = StageMother.createPassedStage("pipeline", 10, "stage", 20, "job1", new Date());
        File job1Dir = createJobArtifactFolder("artifact-root/pipelines/pipeline/10/stage/20/job1");
        File job2Dir = createJobArtifactFolder("artifact-root/pipelines/pipeline/10/stage/20/job2");
        File job1DirFromADifferentStageRun = createJobArtifactFolder("artifact-root/pipelines/pipeline/10/stage/25/job2");
        File job1CacheDir = createJobArtifactFolder("artifact-root/cache/artifacts/pipelines/pipeline/10/stage/20/job1");
        File job2CacheDir = createJobArtifactFolder("artifact-root/cache/artifacts/pipelines/pipeline/10/stage/20/job2");
        File job1CacheDirFromADifferentStageRun = createJobArtifactFolder("artifact-root/cache/artifacts/pipelines/pipeline/10/stage/25/job2");

        artifactsService.purgeArtifactsForStage(stage);

        assertThat(job1Dir.exists(), is(true));
        assertThat(job1Dir.listFiles().length, is(0));
        assertThat(job2Dir.exists(), is(true));
        assertThat(job2Dir.listFiles().length, is(0));
        assertThat(job1DirFromADifferentStageRun.exists(), is(true));
        assertThat(job1DirFromADifferentStageRun.listFiles().length, is(1));
        assertThat(job1CacheDir.exists(), is(false));
        assertThat(job2CacheDir.exists(), is(false));
        assertThat(job1CacheDirFromADifferentStageRun.exists(), is(true));
    }

    private File createJobArtifactFolder(final String path) throws IOException {
        File jobDir = new File(path);
        jobDir.mkdirs();
        File aFile = new File(jobDir, "foo");
        FileUtil.writeContentToFile("hello world", aFile);
        return jobDir;
    }

    @Test
    public void shouldLogAndIgnoreExceptionsWhenDeletingStageArtifacts() throws IllegalArtifactLocationException {
        ArtifactsService artifactsService = new ArtifactsService(resolverService, stageService, artifactsDirHolder, zipUtil, systemService);
        Stage stage = StageMother.createPassedStage("pipeline", 10, "stage", 20, "job", new Date());

        ArtifactDirectoryChooser chooser = mock(ArtifactDirectoryChooser.class);
        ReflectionUtil.setField(artifactsService, "chooser", chooser);

        when(chooser.findArtifact(any(LocatableEntity.class), eq(""))).thenThrow(new IllegalArtifactLocationException("holy cow!"));

        artifactsService.purgeArtifactsForStage(stage);

        assertThat(logFixture.contains(Level.ERROR, "Error occurred while clearing artifacts for 'pipeline/10/stage/20'. Error: 'holy cow!'"), is(true));

        verify(stageService).markArtifactsDeletedFor(stage);
    }

    private void assumeArtifactsRoot(final File artifactsRoot) {
        Mockito.when(artifactsDirHolder.getArtifactsDir()).thenReturn(artifactsRoot);
    }

    public void willCleanUp(File file) {
        resourcesToBeCleanedOnTeardown.add(file);
    }
}

