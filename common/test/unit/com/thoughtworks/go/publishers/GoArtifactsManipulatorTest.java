/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.publishers;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.ArtifactMd5Checksums;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub;
import com.thoughtworks.go.remote.work.GoArtifactsManipulatorStub;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class GoArtifactsManipulatorTest {

    private HttpService httpService;
    private File tempFile;
    private GoArtifactsManipulatorStub goArtifactsManipulatorStub;
    private JobIdentifier jobIdentifier;
    private DefaultGoPublisher goPublisher;
    private File artifactFolder;

    @Before
    public void setUp() throws Exception {
        httpService = mock(HttpService.class);
        artifactFolder = TestFileUtil.createTempFolder("artifact_folder");
        tempFile = TestFileUtil.createTestFile(artifactFolder, "file.txt");
        goArtifactsManipulatorStub = new GoArtifactsManipulatorStub(httpService);
        jobIdentifier = new JobIdentifier("pipeline1", 1, "label-1", "stage1", "1", "job1");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("h", "1", "u"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null, null, false);
        goPublisher = new DefaultGoPublisher(goArtifactsManipulatorStub, jobIdentifier, new BuildRepositoryRemoteStub(), agentRuntimeInfo);
    }

    @After
    public void tearDown() throws IOException {
        FileUtil.tryDeleting(artifactFolder);
    }

    @Test
    public void shouldBombWithErrorWhenStatusCodeReturnedIsRequestEntityTooLarge() throws IOException, InterruptedException {
        long size = anyLong();
        when(httpService.upload(any(String.class), size, any(File.class), any(Properties.class))).thenReturn(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

        CircularFifoBuffer buffer = (CircularFifoBuffer) ReflectionUtil.getField(ReflectionUtil.getField(goPublisher, "consoleOutputTransmitter"), "buffer");
        synchronized (buffer) {
            try {
                goArtifactsManipulatorStub.publish(goPublisher, "some_dest", tempFile, jobIdentifier);
                fail("should have thrown request entity too large error");
            } catch (RuntimeException e) {
                String expectedMessage = "Artifact upload for file " + tempFile.getAbsolutePath() + " (Size: "+size+") was denied by the server. This usually happens when server runs out of disk space.";
                assertThat(e.getMessage(), is("java.lang.RuntimeException: " + expectedMessage + ".  HTTP return code is 413"));
                assertThat(buffer.toString().contains(expectedMessage), is(true));
            }
        }
    }

    @Test
    public void uploadShouldBeGivenFileSize() throws IOException {

        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), any(Properties.class))).thenReturn(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        try {
            goArtifactsManipulatorStub.publish(goPublisher, "dest", tempFile, jobIdentifier);
            fail("should have thrown request entity too large error");
        } catch (RuntimeException e) {
            verify(httpService).upload(any(String.class), eq(tempFile.length()), any(File.class), any(Properties.class));
        }
    }

    @Test
    public void shouldUploadArtifactChecksumAlongWithArtifact() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        goArtifactsManipulatorStub.publish(goPublisher, "/dest/path", tempFile, jobIdentifier);
    }

    @Test
    public void shouldUploadArtifactChecksumWithRightPathWhenArtifactDestinationPathIsEmpty() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data);
        Properties properties = new Properties();
        properties.setProperty("file.txt", md5);

        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        goArtifactsManipulatorStub.publish(goPublisher, "", tempFile, jobIdentifier);
    }

    @Test
    public void shouldUploadArtifactChecksumForADirectory() throws IOException {
        String data = "Some text whose checksum can be asserted";
        String secondData = "some more";

        FileUtils.writeStringToFile(tempFile, data);

        File anotherFile = new File(artifactFolder, "bond/james_bond/another_file");
        FileUtils.writeStringToFile(anotherFile, secondData);


        when(httpService.upload(any(String.class), eq(FileUtils.sizeOfDirectory(artifactFolder)), any(File.class), eq(expectedProperties(data, secondData)))).thenReturn(HttpServletResponse.SC_OK);

        goArtifactsManipulatorStub.publish(goPublisher, "dest", artifactFolder, jobIdentifier);
    }

    private Properties expectedProperties(String data, String secondData) {
        Properties properties = new Properties();
        properties.setProperty("dest/artifact_folder/file.txt", CachedDigestUtils.md5Hex(data));
        properties.setProperty("dest/artifact_folder/bond/james_bond/another_file", CachedDigestUtils.md5Hex(secondData));
        return properties;
    }

    private BaseMatcher<File> matchesContent(final Properties expectedProperties) {
        return new BaseMatcher<File>() {
            public boolean matches(Object item) {
                File checksumFile = (File) item;
                return new ArtifactMd5Checksums(checksumFile).equals(new ArtifactMd5Checksums(expectedProperties));
            }

            public void describeTo(Description description) {
                description.appendText("Checksum file should also be uploaded along with the artifact");
            }
        };
    }


}
