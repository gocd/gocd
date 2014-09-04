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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DirHandlerTest {

    private File artifactDest;
    private File agentDest;
    private ArtifactMd5Checksums checksums;
    private StubGoPublisher goPublisher;
    private File zip;
    private DirHandler dirHandler;

    @Before public void setUp() throws Exception {
        artifactDest = TestFileUtil.createTempFolder("fetch_dest");
        checksums = mock(ArtifactMd5Checksums.class);
        goPublisher = new StubGoPublisher();
        zip = TestFileUtil.createTempFile("zip_location");
        agentDest = TestFileUtil.createTempFolder("agent_fetch_dest");
        dirHandler = new DirHandler("fetch_dest",agentDest);
    }

    @After
    public void tearDown() {
        FileUtil.tryDeleting(artifactDest);
    }

    @Test
    public void shouldComputeMd5ForEveryFileInADirectory() throws IOException {
        zip = createZip("under_dir");
        dirHandler.useArtifactMd5Checksums(checksums);

        when(checksums.md5For("fetch_dest/first")).thenReturn(DigestUtils.md5Hex("First File"));
        when(checksums.md5For("fetch_dest/under_dir/second")).thenReturn(DigestUtils.md5Hex("Second File"));

        dirHandler.handle(new FileInputStream(zip));
        dirHandler.handleResult(200, goPublisher);

        assertThat(goPublisher.getMessage(), containsString(String.format("Saved artifact to [%s] after verifying the integrity of its contents.", agentDest)));
        assertThat(goPublisher.getMessage(), not(containsString("[WARN]")));
        assertArtifactWasSaved("under_dir");
    }

    @Test
    public void shouldSuccessfullyProceedIfNoMd5IsPresentForTheFileUnderInspection() throws IOException {
        when(checksums.md5For("fetch_dest/first")).thenReturn(null);
        zip = createZip("under_dir");
        dirHandler.useArtifactMd5Checksums(checksums);

        try {
            dirHandler.handle(new FileInputStream(zip));
            dirHandler.handleResult(200, goPublisher);
        } catch (RuntimeException e) {
            fail("should not have failed");
        }
        verify(checksums).md5For("fetch_dest/first");
        verify(checksums).md5For("fetch_dest/under_dir/second");
        assertArtifactWasSaved("under_dir");
    }


    @Test
    public void shouldProceedSuccessfullyWhenNoChecksumFileIsPresent() throws IOException {
        zip = createZip("under_dir");
        dirHandler.useArtifactMd5Checksums(null);

        dirHandler.handle(new FileInputStream(zip));
        dirHandler.handleResult(200, goPublisher);

        assertThat(goPublisher.getMessage(), not(
                containsString(String.format("[WARN] The md5checksum value of the artifact [%s] was not found on the server. Hence, Go could not verify the integrity of its contents.", agentDest))));
        assertThat(goPublisher.getMessage(), containsString(String.format("Saved artifact to [%s] without verifying the integrity of its contents.", agentDest)));
        assertArtifactWasSaved("under_dir");
    }

    @Test
    public void shouldProceedSuccessfullyWhenNoChecksumFileIsPresentJustForASingleFile() throws IOException {
        zip = createZip("under_dir");

        dirHandler.useArtifactMd5Checksums(checksums);

        dirHandler.handle(new FileInputStream(zip));
        dirHandler.handleResult(200, goPublisher);

        assertThat(goPublisher.getMessage(), containsString(
                String.format("[WARN] The md5checksum value of the artifact [%s] was not found on the server. Hence, Go could not verify the integrity of its contents.",
                        "fetch_dest/under_dir/second")));
        assertThat(goPublisher.getMessage(), containsString(
                String.format("[WARN] The md5checksum value of the artifact [%s] was not found on the server. Hence, Go could not verify the integrity of its contents.", "fetch_dest/first")));
        assertThat(goPublisher.getMessage(), containsString(String.format("Saved artifact to [%s] without verifying the integrity of its contents.", agentDest)));
        assertArtifactWasSaved("under_dir");
    }

    @Test
    public void shouldThrowExceptionWhenChecksumsDoNotMatch() throws IOException {
        when(checksums.md5For("fetch_dest/first")).thenReturn("foo");
        dirHandler.useArtifactMd5Checksums(checksums);
        zip = createZip("under_dir");

        try {
            dirHandler.handle(new FileInputStream(zip));
            dirHandler.handleResult(200, goPublisher);
            fail("Should throw exception when check sums do not match.");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Artifact download failed for [fetch_dest/first]"));
            assertThat(goPublisher.getMessage(),
                    containsString("[ERROR] Verification of the integrity of the artifact [fetch_dest/first] failed. The artifact file on the server may have changed since its original upload."));
        }
    }

    @Test
    public void shouldUseCompletePathToLookUpMd5Checksum() throws IOException {
        DirHandler handler = new DirHandler("server/fetch_dest", agentDest);
        zip = createZip("under_dir");
        handler.useArtifactMd5Checksums(checksums);

        when(checksums.md5For("server/fetch_dest/first")).thenReturn(DigestUtils.md5Hex("First File"));
        when(checksums.md5For("server/fetch_dest/under_dir/second")).thenReturn(DigestUtils.md5Hex("Second File"));

        handler.handle(new FileInputStream(zip));
        handler.handleResult(200, goPublisher);

        assertThat(goPublisher.getMessage(), containsString(String.format("Saved artifact to [%s] after verifying the integrity of its contents.", agentDest)));
        assertThat(goPublisher.getMessage(), not(containsString("[WARN]")));
        assertArtifactWasSaved("under_dir");

    }

    @Test
    public void shouldUseCorrectPathOnServerToLookUpMd5Checksum() throws IOException {
        DirHandler handler = new DirHandler("fetch_dest", agentDest);
        zip = createZip("fetch_dest");
        handler.useArtifactMd5Checksums(checksums);

        when(checksums.md5For("fetch_dest/first")).thenReturn(DigestUtils.md5Hex("First File"));
        when(checksums.md5For("fetch_dest/fetch_dest/second")).thenReturn(DigestUtils.md5Hex("Second File"));

        handler.handle(new FileInputStream(zip));
        handler.handleResult(200, goPublisher);

        verify(checksums).md5For("fetch_dest/first");
        verify(checksums).md5For("fetch_dest/fetch_dest/second");
        assertThat(goPublisher.getMessage(), containsString(String.format("Saved artifact to [%s] after verifying the integrity of its contents.", agentDest)));
        assertThat(goPublisher.getMessage(), not(containsString("[WARN]")));
        assertArtifactWasSaved("fetch_dest");
    }

    private File createZip(String subDirectoryName) throws IOException {
        File first = new File(artifactDest, "first");
        FileUtils.writeStringToFile(first, "First File");
        File second = new File(artifactDest, subDirectoryName + "/second");
        FileUtils.writeStringToFile(second, "Second File");
        new ZipUtil().zip(artifactDest, zip, 0);
        return zip;
    }

    private void assertArtifactWasSaved(String subDirectoryName) throws IOException {
        File firstFile = new File(agentDest, "fetch_dest/first");
        assertThat(firstFile.exists(), is(true));
        assertThat(FileUtils.readFileToString(firstFile), is("First File"));
        File secondFile = new File(agentDest, "fetch_dest/" + subDirectoryName + "/second");
        assertThat(secondFile.exists(), is(true));
        assertThat(FileUtils.readFileToString(secondFile), is("Second File"));
    }
}
