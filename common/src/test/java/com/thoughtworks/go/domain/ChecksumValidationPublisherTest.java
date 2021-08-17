/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import java.io.File;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ChecksumValidationPublisherTest {

    private ChecksumValidationPublisher checksumValidationPublisher;
    private StubGoPublisher goPublisher;
    private File artifact;

    @BeforeEach
    public void setUp() {
        checksumValidationPublisher = new ChecksumValidationPublisher();
        goPublisher = new StubGoPublisher();
        artifact = new File("src/file/path");
    }

    @Test
    public void testMessagesPublished_WhenMD5PropertyFileIsNotFoundOnServer() throws Exception {
        checksumValidationPublisher.md5ChecksumFileNotFound();
        checksumValidationPublisher.publish(HttpServletResponse.SC_OK, artifact, goPublisher);
        assertThat(goPublisher.getMessage(),
                not(containsString(String.format("[WARN] The md5checksum value of the artifact [%s] was not found on the server. Hence, Go could not verify the integrity of its contents.", artifact))));
        assertThat(goPublisher.getMessage(),
                (containsString(String.format("Saved artifact to [%s] without verifying the integrity of its contents.", artifact))));

    }

    @Test
    public void shouldThrowExceptionWhenMd5ValuesMismatch() {
        checksumValidationPublisher.md5Mismatch(artifact.getPath());
        try {
            checksumValidationPublisher.publish(HttpServletResponse.SC_OK, artifact, goPublisher);
            fail("Should throw exception when checksums do not match.");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(String.format("Artifact download failed for [%s]",artifact)));
            assertThat(goPublisher.getMessage(),
                    containsString(
                            String.format("[ERROR] Verification of the integrity of the artifact [%s] failed. The artifact file on the server may have changed since its original upload.", artifact)));
        }
    }
}
