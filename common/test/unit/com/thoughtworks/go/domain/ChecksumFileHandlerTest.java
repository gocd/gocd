/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ChecksumFileHandlerTest {

    private File file;
    private ChecksumFileHandler checksumFileHandler;

    @Before public void setUp() throws Exception {
        file = TestFileUtil.createUniqueTempFile("foo");
        checksumFileHandler = new ChecksumFileHandler(file);
    }

    @After
    public void tearDown() {
        FileUtil.tryDeleting(file);
    }

    @Test
    public void shouldGenerateChecksumFileUrl() throws IOException {
        String url = checksumFileHandler.url("http://foo/go", "cruise/1/stage/1/job");
        assertThat(url, is("http://foo/go/remoting/files/cruise/1/stage/1/job/cruise-output/md5.checksum"));
    }
    
    @Test
    public void shouldStoreTheMd5ChecksumOnTheAgent() throws IOException {
        checksumFileHandler.handle(new ByteArrayInputStream("Hello World".getBytes()));
        assertThat(FileUtils.readFileToString(file), is("Hello World"));
    }

    @Test
    public void shouldDeleteOldMd5ChecksumFileIfItWasNotFoundOnTheServer() throws IOException {
        StubGoPublisher goPublisher = new StubGoPublisher();
        file.createNewFile();

        boolean isSuccessful = checksumFileHandler.handleResult(HttpServletResponse.SC_NOT_FOUND, goPublisher);
        assertThat(isSuccessful, is(true));
        assertThat(file.exists(), is(false));
    }

    @Test
    public void shouldRetainMd5ChecksumFileIfItIsDownloadedSuccessfully() throws IOException {
        StubGoPublisher goPublisher = new StubGoPublisher();
        file.createNewFile();

        boolean isSuccessful = checksumFileHandler.handleResult(HttpServletResponse.SC_OK, goPublisher);
        assertThat(isSuccessful, is(true));
        assertThat(file.exists(), is(true));

    }

    @Test
    public void shouldHandleResultIfHttpCodeSaysFileNotFound() {
        StubGoPublisher goPublisher = new StubGoPublisher();
        assertThat(checksumFileHandler.handleResult(HttpServletResponse.SC_NOT_FOUND, goPublisher), is(true));
        assertThat(goPublisher.getMessage(), containsString("[WARN] The md5checksum property file was not found on the server. Hence, Go can not verify the integrity of the artifacts."));
    }

    @Test
    public void shouldHandleResultIfHttpCodeIsSuccessful() {
        StubGoPublisher goPublisher = new StubGoPublisher();
        assertThat(checksumFileHandler.handleResult(HttpServletResponse.SC_OK, goPublisher), is(true));
    }

    @Test
    public void shouldHandleResultIfHttpCodeSaysFileNotModified() {
        StubGoPublisher goPublisher = new StubGoPublisher();
        assertThat(checksumFileHandler.handleResult(HttpServletResponse.SC_NOT_MODIFIED, goPublisher), is(true));
    }

    @Test
    public void shouldHandleResultIfHttpCodeSaysFilePermissionDenied() {
        StubGoPublisher goPublisher = new StubGoPublisher();
        assertThat(checksumFileHandler.handleResult(HttpServletResponse.SC_FORBIDDEN, goPublisher), is(false));
    }
    
    @Test
    public void shouldGetArtifactMd5Checksum() throws IOException {
        checksumFileHandler.handle(new ByteArrayInputStream("Hello!!!1".getBytes()));
        ArtifactMd5Checksums artifactMd5Checksums = checksumFileHandler.getArtifactMd5Checksums();
        assertThat(artifactMd5Checksums, is(new ArtifactMd5Checksums(file)));
    }

    @Test
    public void shouldReturnNullArtifactMd5ChecksumIfFileDoesNotExist() {
        assertThat(checksumFileHandler.getArtifactMd5Checksums(), is(nullValue()));
    }


}
