/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.validation;

import com.thoughtworks.go.agent.ChecksumValidationPublisher;
import com.thoughtworks.go.domain.ArtifactMd5Checksums;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Mockito.*;

public class ChecksumValidatorTest {

    private ArtifactMd5Checksums checksums;
    private ChecksumValidationPublisher checksumValidationPublisher;

    @BeforeEach
    public void setUp() {
        checksums = mock(ArtifactMd5Checksums.class);
        checksumValidationPublisher = mock(ChecksumValidationPublisher.class);
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(checksumValidationPublisher);
    }

    @Test
    public void shouldCallbackWhenMd5Match() throws IOException {
        when(checksums.md5For("path")).thenReturn(DigestUtils.md5Hex("foo"));

        final ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
        new ChecksumValidator(checksums).validate("path", DigestUtils.md5Hex(stream), checksumValidationPublisher);

        verify(checksumValidationPublisher).md5Match("path");
    }

    @Test
    public void shouldCallbackWhenMd5Mismatch() throws IOException {
        when(checksums.md5For("path")).thenReturn(DigestUtils.md5Hex("something"));

        final ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
        new ChecksumValidator(checksums).validate("path", DigestUtils.md5Hex(stream), checksumValidationPublisher);

        verify(checksumValidationPublisher).md5Mismatch("path");
    }

    @Test
    public void shouldCallbackWhenMd5IsNotFound() throws IOException {
        when(checksums.md5For("path")).thenReturn(null);

        final ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
        new ChecksumValidator(checksums).validate("path", DigestUtils.md5Hex(stream), checksumValidationPublisher);

        verify(checksumValidationPublisher).md5NotFoundFor("path");
    }

    @Test
    public void shouldNotifyPublisherWhenArtifactChecksumFileIsMissing() {
        new ChecksumValidator(null).validate(null,null,checksumValidationPublisher);

        verify(checksumValidationPublisher).md5ChecksumFileNotFound();
    }
}
