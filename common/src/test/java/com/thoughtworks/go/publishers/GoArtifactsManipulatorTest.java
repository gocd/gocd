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
package com.thoughtworks.go.publishers;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildRepositoryRemoteStub;
import com.thoughtworks.go.remote.work.GoArtifactsManipulatorStub;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class GoArtifactsManipulatorTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private HttpService httpService;
    private File tempFile;
    private GoArtifactsManipulatorStub goArtifactsManipulatorStub;
    private JobIdentifier jobIdentifier;
    private DefaultGoPublisher goPublisher;
    private File artifactFolder;

    @Before
    public void setUp() throws Exception {
        httpService = mock(HttpService.class);
        artifactFolder = temporaryFolder.newFolder("artifact_folder");
        tempFile = temporaryFolder.newFile("artifact_folder/file.txt");
        FileUtils.writeStringToFile(tempFile, "some-random-data", UTF_8);
        goArtifactsManipulatorStub = new GoArtifactsManipulatorStub(httpService);
        jobIdentifier = new JobIdentifier("pipeline1", 1, "label-1", "stage1", "1", "job1");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("h", "1", "u"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null);
        goPublisher = new DefaultGoPublisher(goArtifactsManipulatorStub, jobIdentifier, new BuildRepositoryRemoteStub(), agentRuntimeInfo, "utf-8");
    }

    @Test
    public void shouldBombWithErrorWhenStatusCodeReturnedIsRequestEntityTooLarge() throws IOException, InterruptedException {
        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), any(Properties.class))).thenReturn(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

        CircularFifoQueue buffer = (CircularFifoQueue) ReflectionUtil.getField(ReflectionUtil.getField(goPublisher, "consoleOutputTransmitter"), "buffer");
        synchronized (buffer) {
            try {
                goArtifactsManipulatorStub.publish(goPublisher, "some_dest", tempFile, jobIdentifier);
                fail("should have thrown request entity too large error");
            } catch (RuntimeException e) {
                String expectedMessage = "Artifact upload for file " + tempFile.getAbsolutePath() + " (Size: "+ tempFile.length() +") was denied by the server. This usually happens when server runs out of disk space.";
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
        FileUtils.writeStringToFile(tempFile, data, UTF_8);
        Properties properties = new Properties();
        properties.setProperty("dest/path/file.txt", md5);

        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        goArtifactsManipulatorStub.publish(goPublisher, "/dest/path", tempFile, jobIdentifier);
    }

    @Test
    public void shouldUploadArtifactChecksumWithRightPathWhenArtifactDestinationPathIsEmpty() throws IOException {
        String data = "Some text whose checksum can be asserted";
        final String md5 = CachedDigestUtils.md5Hex(data);
        FileUtils.writeStringToFile(tempFile, data, UTF_8);
        Properties properties = new Properties();
        properties.setProperty("file.txt", md5);

        when(httpService.upload(any(String.class), eq(tempFile.length()), any(File.class), eq(properties))).thenReturn(HttpServletResponse.SC_OK);

        goArtifactsManipulatorStub.publish(goPublisher, "", tempFile, jobIdentifier);
    }

    @Test
    public void shouldUploadArtifactChecksumForADirectory() throws IOException {
        String data = "Some text whose checksum can be asserted";
        String secondData = "some more";

        FileUtils.writeStringToFile(tempFile, data, UTF_8);

        File anotherFile = new File(artifactFolder, "bond/james_bond/another_file");
        FileUtils.writeStringToFile(anotherFile, secondData, UTF_8);


        when(httpService.upload(any(String.class), eq(FileUtils.sizeOfDirectory(artifactFolder)), any(File.class), eq(expectedProperties(data, secondData)))).thenReturn(HttpServletResponse.SC_OK);

        goArtifactsManipulatorStub.publish(goPublisher, "dest", artifactFolder, jobIdentifier);
    }

    private Properties expectedProperties(String data, String secondData) {
        Properties properties = new Properties();
        properties.setProperty("dest/artifact_folder/file.txt", CachedDigestUtils.md5Hex(data));
        properties.setProperty("dest/artifact_folder/bond/james_bond/another_file", CachedDigestUtils.md5Hex(secondData));
        return properties;
    }


}
