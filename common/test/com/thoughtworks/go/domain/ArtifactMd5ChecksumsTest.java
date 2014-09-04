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
import java.io.IOException;
import java.util.Properties;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ArtifactMd5ChecksumsTest {

    private File file;

    @Before
    public void setUp() {
        file = TestFileUtil.createUniqueTempFile("foo");
    }

    @After
    public void tearDown() {
        FileUtil.tryDeleting(file);
    }

    @Test
    public void shouldReturnTrueIfTheChecksumFileContainsAGivenPath() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("first/path", "md5");
        ArtifactMd5Checksums artifactMd5Checksums = new ArtifactMd5Checksums(properties);
        assertThat(artifactMd5Checksums.md5For("first/path"), is("md5"));
    }

    @Test
    public void shouldReturnNullIfTheChecksumFileDoesNotContainsAGivenPath() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("first/path", "md5");
        ArtifactMd5Checksums artifactMd5Checksums = new ArtifactMd5Checksums(properties);
        assertThat(artifactMd5Checksums.md5For("foo"), is(nullValue()));
    }

    @Test
    public void shouldLoadThePropertiesFromTheGivenFile() throws IOException {
        FileUtils.writeStringToFile(file, "first/path:md5=");
        ArtifactMd5Checksums artifactMd5Checksums = new ArtifactMd5Checksums(file);
        assertThat(artifactMd5Checksums.md5For("first/path"), is("md5="));
    }

    @Test
    public void shouldThrowAnExceptionIfTheLoadingFails() throws IOException {
        try {
            new ArtifactMd5Checksums(file);
            fail("Should have failed because of an invalid properites file");
        } catch (RuntimeException e) {
            assertThat(e.getCause(), instanceOf(IOException.class));
            assertThat(e.getMessage(), is(String.format("[Checksum Verification] Could not load the MD5 from the checksum file '%s'", file)));
        }
    }
}
