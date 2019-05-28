/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.buildsession.ArtifactsRepository;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.helper.TestStreamConsumer;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static com.thoughtworks.go.matchers.ConsoleOutMatcherJunit5.assertConsoleOut;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
class UrlBasedArtifactsRepositoryTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HttpService httpService;
    private File tempFile;
    private File artifactFolder;
    private ArtifactsRepository artifactsRepository;
    private TestStreamConsumer console;

    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);
        artifactFolder = temporaryFolder.newFolder("artifact_folder");
        tempFile = temporaryFolder.newFile("artifact_folder/file.txt");
        FileUtils.writeStringToFile(tempFile, "some-random-junk", UTF_8);
        console = new TestStreamConsumer();
        artifactsRepository = new UrlBasedArtifactsRepository(httpService, "http://baseurl/artifacts/", "http://baseurl/properties/", new ZipUtil());
    }

    @Test
    void shouldBombWithErrorWhenStatusCodeReturnedIsRequestEntityTooLarge() throws IOException, InterruptedException {
        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), any(Properties.class))).thenReturn(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

        try {
            artifactsRepository.upload(console, tempFile, "some_dest", "build42");
            fail("should have thrown request entity too large error");
        } catch (RuntimeException e) {
            String expectedMessage = "Artifact upload for file " + tempFile.getAbsolutePath() + " (Size: " + tempFile.length() + ") was denied by the server. This usually happens when server runs out of disk space.";
            assertThat(e.getMessage()).isEqualTo("java.lang.RuntimeException: " + expectedMessage + ".  HTTP return code is 413");
            assertThat(console.output().contains(expectedMessage)).isTrue();
        }
    }

    @Test
    void uploadShouldBeGivenFileSize() throws IOException {
        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), any(Properties.class))).thenReturn(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        try {
            artifactsRepository.upload(console, tempFile, "dest", "build42");
            fail("should have thrown request entity too large error");
        } catch (RuntimeException e) {
            verify(httpService).upload(eq("http://baseurl/artifacts/dest?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), any(Properties.class));
        }
    }

    @Test
    void shouldRetryUponUploadFailure() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data, UTF_8);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=2&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=3&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);
        artifactsRepository.upload(console, tempFile, "dest/path", "build42");
    }

    @Test
    void shouldPrintFailureMessageToConsoleWhenUploadFailed() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data, UTF_8);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=2&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=3&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);

        try {
            artifactsRepository.upload(console, tempFile, "dest/path", "build42");
            fail("should have thrown request entity too large error");
        } catch (RuntimeException e) {
            assertConsoleOut(console.output()).printedUploadingFailure(tempFile);
        }
    }


    @Test
    void shouldUploadArtifactChecksumAlongWithArtifact() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data, UTF_8);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        artifactsRepository.upload(console, tempFile, "dest/path", "build42");
    }

    @Test
    void shouldUploadArtifactChecksumWithRightPathWhenArtifactDestinationPathIsEmpty() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data, UTF_8);
        Properties properties = new Properties();
        properties.setProperty("file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        artifactsRepository.upload(console, tempFile, "", "build42");
    }

    @Test
    void shouldUploadArtifactChecksumForADirectory() throws IOException {
        String data = "Some text whose checksum can be asserted";
        String secondData = "some more";

        FileUtils.writeStringToFile(tempFile, data, UTF_8);

        File anotherFile = new File(artifactFolder, "bond/james_bond/another_file");
        FileUtils.writeStringToFile(anotherFile, secondData, UTF_8);


        when(httpService.upload(eq("http://baseurl/artifacts/dest?attempt=1&buildId=build42"), eq(FileUtils.sizeOfDirectory(artifactFolder)), any(File.class), eq(expectedProperties(data, secondData)))).thenReturn(HttpServletResponse.SC_OK);
        artifactsRepository.upload(console, artifactFolder, "dest", "build42");
    }

    @Test
    void setRemoteBuildPropertyShouldEncodePropertyName() throws IOException {
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);

        artifactsRepository.setProperty(new Property("fo,o", "bar"));
        verify(httpService).postProperty(url.capture(), value.capture());
        assertThat(value.getValue()).isEqualTo("bar");
        assertThat(url.getValue()).isEqualTo("http://baseurl/properties/fo%2Co");
    }

    private Properties expectedProperties(String data, String secondData) {
        Properties properties = new Properties();
        properties.setProperty("dest/artifact_folder/file.txt", CachedDigestUtils.md5Hex(data));
        properties.setProperty("dest/artifact_folder/bond/james_bond/another_file", CachedDigestUtils.md5Hex(secondData));
        return properties;
    }

}
