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

package com.thoughtworks.go.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.thoughtworks.go.agent.ChecksumValidationPublisher;
import com.thoughtworks.go.domain.ArtifactMd5Checksums;
import com.thoughtworks.go.util.CachedDigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ChecksumValidatorTest {

    private ArtifactMd5Checksums checksums;
    private ChecksumValidationPublisher checksumValidationPublisher;

    @Before public void setUp() throws Exception {
        checksums = mock(ArtifactMd5Checksums.class);
        checksumValidationPublisher = mock(ChecksumValidationPublisher.class);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(checksumValidationPublisher);
    }

    @Test
    public void shouldCallbackWhenMd5Match() throws IOException {
        when(checksums.md5For("path")).thenReturn(CachedDigestUtils.md5Hex("foo"));

        final ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
        new ChecksumValidator(checksums).validate("path", CachedDigestUtils.md5Hex(stream), checksumValidationPublisher);

        verify(checksumValidationPublisher).md5Match("path");
    }

    @Test
    public void shouldCallbackWhenMd5Mismatch() throws IOException {
        when(checksums.md5For("path")).thenReturn(CachedDigestUtils.md5Hex("something"));

        final ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
        new ChecksumValidator(checksums).validate("path", CachedDigestUtils.md5Hex(stream), checksumValidationPublisher);

        verify(checksumValidationPublisher).md5Mismatch("path");
    }

    @Test
    public void shouldCallbackWhenMd5IsNotFound() throws IOException {
        when(checksums.md5For("path")).thenReturn(null);

        final ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
        new ChecksumValidator(checksums).validate("path", CachedDigestUtils.md5Hex(stream), checksumValidationPublisher);

        verify(checksumValidationPublisher).md5NotFoundFor("path");
    }

    @Test
    public void shouldNotifyPublisherWhenArtifactChecksumFileIsMissing() throws IOException {
        new ChecksumValidator(null).validate(null,null,checksumValidationPublisher);

        verify(checksumValidationPublisher).md5ChecksumFileNotFound();
    }
}
