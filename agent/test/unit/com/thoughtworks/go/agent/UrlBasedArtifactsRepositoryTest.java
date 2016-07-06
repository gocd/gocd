/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.agent;

import com.thoughtworks.go.buildsession.ArtifactsRepository;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.helper.TestStreamConsumer;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static com.thoughtworks.go.matchers.ConsoleOutMatcher.printedUploadingFailure;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UrlBasedArtifactsRepositoryTest {

    @Mock
    private HttpService httpService;
    private File tempFile;
    private File artifactFolder;
    private ArtifactsRepository artifactsRepository;
    private TestStreamConsumer console;

    @Before
    public void setUp() throws Exception {
        artifactFolder = TestFileUtil.createTempFolder("artifact_folder");
        tempFile = TestFileUtil.createTestFile(artifactFolder, "file.txt");
        console = new TestStreamConsumer();
        artifactsRepository = new UrlBasedArtifactsRepository(httpService, "http://baseurl/artifacts/", "http://baseurl/properties/", new ZipUtil());
    }

    @After
    public void tearDown() throws IOException {
        FileUtil.tryDeleting(artifactFolder);
    }

    @Test
    public void shouldBombWithErrorWhenStatusCodeReturnedIsRequestEntityTooLarge() throws IOException, InterruptedException {
        long size = anyLong();
        when(httpService.upload(any(String.class), size, any(File.class), any(Properties.class))).thenReturn(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

        try {
            artifactsRepository.upload(console, tempFile, "some_dest", "build42");
            fail("should have thrown request entity too large error");
        } catch (RuntimeException e) {
            String expectedMessage = "Artifact upload for file " + tempFile.getAbsolutePath() + " (Size: " + size + ") was denied by the server. This usually happens when server runs out of disk space.";
            assertThat(e.getMessage(), is("java.lang.RuntimeException: " + expectedMessage + ".  HTTP return code is 413"));
            assertThat(console.output().contains(expectedMessage), is(true));
        }
    }

    @Test
    public void uploadShouldBeGivenFileSize() throws IOException {
        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), any(Properties.class))).thenReturn(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        try {
            artifactsRepository.upload(console, tempFile, "dest", "build42");
            fail("should have thrown request entity too large error");
        } catch (RuntimeException e) {
            verify(httpService).upload(eq("http://baseurl/artifacts/dest?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), any(Properties.class));
        }
    }

    @Test
    public void shouldRetryUponUploadFailure() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=2&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=3&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);
        artifactsRepository.upload(console, tempFile, "dest/path", "build42");
    }

    @Test
    public void shouldPrintFailureMessageToConsoleWhenUploadFailed() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=2&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=3&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);

        try {
            artifactsRepository.upload(console, tempFile, "dest/path", "build42");
            fail("should have thrown request entity too large error");
        } catch (RuntimeException e) {
            assertThat(console.output(), printedUploadingFailure(tempFile));
        }
    }


    @Test
    public void shouldUploadArtifactChecksumAlongWithArtifact() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/dest/path?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        artifactsRepository.upload(console, tempFile, "dest/path", "build42");
    }

    @Test
    public void shouldUploadArtifactChecksumWithRightPathWhenArtifactDestinationPathIsEmpty() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data);
        Properties properties = new Properties();
        properties.setProperty("file.txt", md5);

        when(httpService.upload(eq("http://baseurl/artifacts/?attempt=1&buildId=build42"), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        artifactsRepository.upload(console, tempFile, "", "build42");
    }

    @Test
    public void shouldUploadArtifactChecksumForADirectory() throws IOException {
        String data = "Some text whose checksum can be asserted";
        String secondData = "some more";

        FileUtils.writeStringToFile(tempFile, data);

        File anotherFile = new File(artifactFolder, "bond/james_bond/another_file");
        FileUtils.writeStringToFile(anotherFile, secondData);


        when(httpService.upload(eq("http://baseurl/artifacts/dest?attempt=1&buildId=build42"), eq(FileUtils.sizeOfDirectory(artifactFolder)), any(File.class), eq(expectedProperties(data, secondData)))).thenReturn(HttpServletResponse.SC_OK);
        artifactsRepository.upload(console, artifactFolder, "dest", "build42");
    }

    @Test
    public void setRemoteBuildPropertyShouldEncodePropertyName() throws IOException {
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);

        artifactsRepository.setProperty(new Property("fo,o", "bar"));
        verify(httpService).postProperty(url.capture(), value.capture());
        assertThat(value.getValue(), is("bar"));
        assertThat(url.getValue(), is("http://baseurl/properties/fo%2Co"));
    }

    private Properties expectedProperties(String data, String secondData) {
        Properties properties = new Properties();
        properties.setProperty("dest/artifact_folder/file.txt", CachedDigestUtils.md5Hex(data));
        properties.setProperty("dest/artifact_folder/bond/james_bond/another_file", CachedDigestUtils.md5Hex(secondData));
        return properties;
    }

}
