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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.CachedDigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class FileHandlerTest {

    private File artifact;
    private ArtifactMd5Checksums checksums;
    private FileHandler fileHandler;
    private StubGoPublisher goPublisher;

    @Before
    public void setup() {
        artifact = new File("foo");
        checksums = mock(ArtifactMd5Checksums.class);
        fileHandler = new FileHandler(artifact, "src/file/path");
        goPublisher = new StubGoPublisher();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(artifact);
    }

    @Test
    public void shouldCheckTheMD5OfTheFile() throws IOException {
        when(checksums.md5For("src/file/path")).thenReturn(CachedDigestUtils.md5Hex(new ByteArrayInputStream("Hello world".getBytes())));
        fileHandler.useArtifactMd5Checksums(checksums);

        fileHandler.handle(new ByteArrayInputStream("Hello world".getBytes()));
        fileHandler.handleResult(200, goPublisher);

        assertThat(FileUtils.readFileToString(artifact, UTF_8), is("Hello world"));
        assertThat(goPublisher.getMessage(), containsString("Saved artifact to [foo] after verifying the integrity of its contents."));
        verify(checksums).md5For("src/file/path");
        verifyNoMoreInteractions(checksums);
    }

    @Test
    public void shouldWarnWhenChecksumsFileIsNotPresent() throws IOException {
        fileHandler.handle(new ByteArrayInputStream("Hello world".getBytes()));

        fileHandler.handleResult(200, goPublisher);

        assertThat(goPublisher.getMessage(), containsString("Saved artifact to [foo] without verifying the integrity of its contents."));
        assertThat(goPublisher.getMessage(), not(containsString("[WARN] The md5checksum value of the artifact [src/file/path] was not found on the server. Hence, Go could not verify the integrity of its contents.")));
        assertThat(FileUtils.readFileToString(artifact, UTF_8), is("Hello world"));
    }

    @Test
    public void shouldWarnWhenChecksumsFileIsPresentButMD5DoesNotExist() throws IOException {
        when(checksums.md5For("src/file/path")).thenReturn(null);
        fileHandler.useArtifactMd5Checksums(checksums);

        fileHandler.handle(new ByteArrayInputStream("Hello world".getBytes()));

        fileHandler.handleResult(200, goPublisher);

        assertThat(goPublisher.getMessage(), containsString("[WARN] The md5checksum value of the artifact [src/file/path] was not found on the server. Hence, Go could not verify the integrity of its contents."));
        assertThat(goPublisher.getMessage(), containsString("Saved artifact to [foo] without verifying the integrity of its contents"));
        assertThat(FileUtils.readFileToString(artifact, UTF_8), is("Hello world"));
    }

    @Test
    public void shouldThrowExceptionWhenChecksumsDoNotMatch() throws IOException {
        when(checksums.md5For("src/file/path")).thenReturn("wrong_md5");
        fileHandler.useArtifactMd5Checksums(checksums);

        try {
            fileHandler.handle(new ByteArrayInputStream("Hello world".getBytes()));
            fileHandler.handleResult(200, goPublisher);
            fail("Should throw exception when checksums do not match.");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Artifact download failed for [src/file/path]"));
            assertThat(goPublisher.getMessage(),
                    containsString("[ERROR] Verification of the integrity of the artifact [src/file/path] failed. The artifact file on the server may have changed since its original upload."));
        }
    }

    @Test
    public void shouldNotDisplayArtifactMultipleTimesWhenRetriesCalled() throws IOException {
        when(checksums.md5For("src/file/path")).thenReturn("wrong_md5");
        fileHandler.useArtifactMd5Checksums(checksums);

        int retryCount = 0;

        while (retryCount < 2) {
            retryCount++;
            try {
                fileHandler.handle(new ByteArrayInputStream("Hello world".getBytes()));
                fileHandler.handleResult(200, goPublisher);
                fail("Should throw exception when checksums do not match.");
            } catch (RuntimeException e) {
                assertThat(e.getMessage(), is("Artifact download failed for [src/file/path]"));
                assertThat(goPublisher.getMessage(),
                        containsString("[ERROR] Verification of the integrity of the artifact [src/file/path] failed. The artifact file on the server may have changed since its original upload."));
            }
        }
    }

}
