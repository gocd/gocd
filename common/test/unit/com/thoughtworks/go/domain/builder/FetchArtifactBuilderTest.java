/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FetchArtifactBuilderTest {
    private File zip;
    private List<File> toClean = new ArrayList<File>();
    private static final String URL = "http://10.18.7.51:8153/go/remoting/files/cruise/1.0.2341/dev/1/windows-3/cruise-output/console.log";

    private File dest;
    private TestingClock clock;
    private StubGoPublisher publisher;
    private ChecksumFileHandler checksumFileHandler;
    private URLService urlService;
    private DownloadAction downloadAction;

    @Before
    public void setUp() throws Exception {
        File folder = TestFileUtil.createTempFolder("log");
        File consolelog = new File(folder, "console.log");
        folder.mkdirs();
        consolelog.createNewFile();

        zip = new ZipUtil().zip(folder, TestFileUtil.createUniqueTempFile(folder.getName()), Deflater.NO_COMPRESSION);
        toClean.add(folder);
        toClean.add(zip);
        dest = new File("dest");
        dest.mkdirs();
        toClean.add(dest);
        clock = new TestingClock();
        publisher = new StubGoPublisher();
        checksumFileHandler = mock(ChecksumFileHandler.class);
        urlService = mock(URLService.class);
        downloadAction = mock(DownloadAction.class);
    }

    @After
    public void tearDown() throws Exception {
        for (File fileToClean : toClean) {
            FileUtils.deleteQuietly(fileToClean);
        }
    }

    @Test
    public void shouldUnzipWhenFetchingFolder() throws Exception {
        ChecksumFileHandler checksumFileHandler = mock(ChecksumFileHandler.class);
        when(checksumFileHandler.handleResult(SC_OK, publisher)).thenReturn(true);

        File destOnAgent = new File("pipelines/cruise/", dest.getPath());
        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -10, "1", "dev", "1", "windows", 1L), "log", dest.getPath(), new DirHandler("log",destOnAgent), checksumFileHandler);

        builder.fetch(new DownloadAction(new StubFetchZipHttpService(), publisher, clock), new StubURLService());

        assertDownloaded(destOnAgent);
    }

    @Test
    public void shouldSaveFileWhenFetchingFile() throws Exception {
        ChecksumFileHandler checksumFileHandler = mock(ChecksumFileHandler.class);
        when(checksumFileHandler.handleResult(SC_OK, publisher)).thenReturn(true);

        File artifactOnAgent = new File("pipelines/cruise/a.jar");
        toClean.add(artifactOnAgent);

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", -1, "1", "dev", "1", "windows", 1L), "log", "some where do download", new FileHandler(artifactOnAgent, getSrc()), checksumFileHandler);

        builder.fetch(new DownloadAction(new StubFetchZipHttpService(), publisher, clock), new StubURLService());

        assertThat(artifactOnAgent.isFile(), is(true));
    }

    private String getSrc() {
        return "";
    }

    @Test
    public void shouldReturnURLWithoutSHA1WhenFileDoesNotExist() throws Exception {
        String src = "cruise-output/console.log";
        File destOnAgent = new File("pipelines" + '/' + "cruise" + '/' + dest);
        File consolelog = new File(destOnAgent, "console.log");
        consolelog.delete();

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("foo", -1, "label-1", "dev", "1", "linux", 1L),
                src, "lib/a.jar",
                new FileHandler(consolelog, getSrc()), checksumFileHandler);

        when(urlService.baseRemoteURL()).thenReturn("http://foo.bar:8153/go");

        when(checksumFileHandler.url("http://foo.bar:8153/go", "foo/label-1/dev/1/linux")).thenReturn("http://foo.bar:8153/go/files/foo/label-1/dev/1/linux/cruise-output/md5.checksum");

        java.util.Properties properties = new java.util.Properties();
        when(checksumFileHandler.getArtifactMd5Checksums()).thenReturn(new ArtifactMd5Checksums(properties));

        builder.fetch(downloadAction, urlService);

        verify(downloadAction).perform(Mockito.eq("http://foo.bar:8153/go/files/foo/label-1/dev/1/linux/cruise-output/md5.checksum"), any(FetchHandler.class));
        verify(downloadAction).perform(Mockito.eq("http://foo.bar:8153/go/remoting/files/foo/label-1/dev/1/linux/cruise-output/console.log"), any(ChecksumFileHandler.class));
        verifyNoMoreInteractions(downloadAction);
    }

    @Test
    public void shouldReturnURLWithSHA1WhenFileExists() throws Exception {
        String src = "cruise-output/console.log";
        File destOnAgent = new File("pipelines" + '/' + "cruise" + '/' + dest);
        File consolelog = new File(destOnAgent, "console.log");
        consolelog.getParentFile().mkdirs();
        consolelog.createNewFile();
        toClean.add(destOnAgent);

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("foo", -1, "label-1", "dev", "1", "linux", 1L),
                src, "lib/a.jar",
                new FileHandler(consolelog, getSrc()), checksumFileHandler);

        when(urlService.baseRemoteURL()).thenReturn("http://foo.bar:8153/go");

        when(checksumFileHandler.url("http://foo.bar:8153/go", "foo/label-1/dev/1/linux")).thenReturn("http://foo.bar:8153/go/files/foo/label-1/dev/1/linux/cruise-output/md5.checksum");

        java.util.Properties properties = new java.util.Properties();
        when(checksumFileHandler.getArtifactMd5Checksums()).thenReturn(new ArtifactMd5Checksums(properties));

        builder.fetch(downloadAction, urlService);

        verify(downloadAction).perform(Mockito.eq("http://foo.bar:8153/go/files/foo/label-1/dev/1/linux/cruise-output/md5.checksum"), any(FetchHandler.class));
        verify(downloadAction).perform(Mockito.eq("http://foo.bar:8153/go/remoting/files/foo/label-1/dev/1/linux/cruise-output/console.log?sha1=2jmj7l5rSw0yVb%2FvlWAYkK%2FYBwk%3D"), any(ChecksumFileHandler.class));
        verifyNoMoreInteractions(downloadAction);
    }

    @Test
    public void shouldReturnURLEndsWithDotZipWhenRequestingFolder() throws Exception {
        String src = "cruise-output";
        File destOnAgent = new File("pipelines/cruise/", dest.getPath());

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("foo", -1, "label-1", "dev", "1", "linux", 1L),
                src, "lib/a.jar",
                new DirHandler(src, destOnAgent), checksumFileHandler);

        when(urlService.baseRemoteURL()).thenReturn("http://foo.bar:8153/go");

        when(checksumFileHandler.url("http://foo.bar:8153/go", "foo/label-1/dev/1/linux")).thenReturn("http://foo.bar:8153/go/files/foo/label-1/dev/1/linux/cruise-output/md5.checksum");

        java.util.Properties properties = new java.util.Properties();
        when(checksumFileHandler.getArtifactMd5Checksums()).thenReturn(new ArtifactMd5Checksums(properties));

        builder.fetch(downloadAction, urlService);

        verify(downloadAction).perform(Mockito.eq("http://foo.bar:8153/go/files/foo/label-1/dev/1/linux/cruise-output/md5.checksum"), any(FetchHandler.class));
        verify(downloadAction).perform(Mockito.eq("http://foo.bar:8153/go/remoting/files/foo/label-1/dev/1/linux/cruise-output.zip"), any(ChecksumFileHandler.class));
        verifyNoMoreInteractions(downloadAction);
    }


    @Test
    public void shouldValidateChecksumOnArtifact() throws Exception {
        when(urlService.baseRemoteURL()).thenReturn("http://10.10.1.1/go/files");
        when(checksumFileHandler.url("http://10.10.1.1/go/files", "cruise/10/dev/1/windows")).thenReturn("http://10.10.1.1/go/files/cruise/10/dev/1/windows/cruise-output/md5.checksum");

        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", 10, "1", "dev", "1", "windows", 1L), "log", dest.getPath(), mock(FetchHandler.class), checksumFileHandler);
        builder.fetch(downloadAction, urlService);

        verify(downloadAction).perform("http://10.10.1.1/go/files/cruise/10/dev/1/windows/cruise-output/md5.checksum", checksumFileHandler);
    }

    @Test
    public void shouldMakeTheFetchHandlerUseTheArtifactMd5Checksum() throws Exception {
        ArtifactMd5Checksums artifactMd5Checksums = mock(ArtifactMd5Checksums.class);

        when(urlService.baseRemoteURL()).thenReturn("http://10.10.1.1/go/files");
        when(checksumFileHandler.url("http://10.10.1.1/go/files", "cruise/10/dev/1/windows")).thenReturn("http://10.10.1.1/go/files/cruise/10/dev/1/windows/cruise-output/md5.checksum");
        when(checksumFileHandler.getArtifactMd5Checksums()).thenReturn(artifactMd5Checksums);

        FetchHandler fetchHandler = mock(FetchHandler.class);
        FetchArtifactBuilder builder = getBuilder(new JobIdentifier("cruise", 10, "1", "dev", "1", "windows", 1L), "log", dest.getPath(), fetchHandler, checksumFileHandler);
        builder.fetch(downloadAction, urlService);

        verify(fetchHandler).useArtifactMd5Checksums(artifactMd5Checksums);
    }

    private FetchArtifactBuilder getBuilder(JobIdentifier jobLocator, String srcdir, String dest, FetchHandler handler, final ChecksumFileHandler checksumFileHandler) {
        return new FetchArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "", jobLocator, srcdir, dest, handler, checksumFileHandler);
    }

    private class StubFetchZipHttpService extends HttpService {
        public int download(String url, FetchHandler handler) throws IOException {
            handler.handle(new FileInputStream(zip));
            return SC_OK;
        }
    }

    private static class StubURLService extends URLService {
        public String baseRemoteURL() {
            return "";
        }
    }

    private void assertDownloaded(File destOnAgent) {
        File logFolder = new File(destOnAgent, "log");
        assertThat(logFolder.exists(), is(true));
        assertThat(logFolder.isDirectory(), is(true));
        assertThat(new File(logFolder, "console.log").exists(), is(true));
        assertThat(destOnAgent.listFiles(), is(new File[]{logFolder}));
    }
}
