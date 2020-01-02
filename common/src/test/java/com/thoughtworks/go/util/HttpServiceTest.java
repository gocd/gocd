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
package com.thoughtworks.go.util;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.domain.FetchHandler;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.thoughtworks.go.util.HttpService.GO_ARTIFACT_PAYLOAD_SIZE;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class HttpServiceTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File folderToSaveDowloadFiles;
    private HttpService service;
    private HttpService.HttpClientFactory httpClientFactory;
    private GoAgentServerHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        folderToSaveDowloadFiles = temporaryFolder.newFolder();
        httpClientFactory = mock(HttpService.HttpClientFactory.class);
        httpClient = mock(GoAgentServerHttpClient.class);
        when(httpClientFactory.httpClient()).thenReturn(httpClient);

        AgentRegistry agentRegistry = mock(AgentRegistry.class);
        when(agentRegistry.token()).thenReturn("some-token");
        when(agentRegistry.uuid()).thenReturn("some-guid");
        service = new HttpService(httpClientFactory, agentRegistry, false);
    }

    @Test
    public void shouldPostArtifactsAlongWithMD5() throws IOException, URISyntaxException {
        File uploadingFile = mock(File.class);
        java.util.Properties checksums = new java.util.Properties();

        String uploadUrl = "http://url";

        HttpPost mockPostMethod = mock(HttpPost.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(mockPostMethod)).thenReturn(response);

        when(uploadingFile.exists()).thenReturn(true);
        when(httpClientFactory.createPost(uploadUrl)).thenReturn(mockPostMethod);
        when(mockPostMethod.getURI()).thenReturn(new URI(uploadUrl));

        service.upload(uploadUrl, 100L, uploadingFile, checksums);

        verify(mockPostMethod).setHeader(GO_ARTIFACT_PAYLOAD_SIZE, "100");
        verify(mockPostMethod).setHeader("Confirm", "true");
        verify(mockPostMethod).setHeader("X-Agent-GUID", "some-guid");
        verify(mockPostMethod).setHeader("Authorization", "some-token");
        verify(httpClientFactory).createMultipartRequestEntity(uploadingFile, checksums);
        verify(httpClient).execute(mockPostMethod);
    }

    @Test
    public void shouldDownloadArtifact() throws IOException, URISyntaxException {
        String url = "http://blah";
        FetchHandler fetchHandler = mock(FetchHandler.class);

        HttpGet mockGetMethod = mock(HttpGet.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        ByteArrayInputStream instream = new ByteArrayInputStream(new byte[]{});
        basicHttpEntity.setContent(instream);
        when(response.getEntity()).thenReturn(basicHttpEntity);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(mockGetMethod)).thenReturn(response);
        when(httpClientFactory.createGet(url)).thenReturn(mockGetMethod);

        when(mockGetMethod.getURI()).thenReturn(new URI(url));

        service.download(url, fetchHandler);
        verify(httpClient).execute(mockGetMethod);
        verify(fetchHandler).handle(instream);
    }

    @Test
    public void shouldNotFailIfChecksumFileIsNotPresent() throws IOException {
        HttpService.HttpClientFactory factory = new HttpService.HttpClientFactory(null);
        File artifact = new File(folderToSaveDowloadFiles, "artifact");
        artifact.createNewFile();
        try {
            factory.createMultipartRequestEntity(artifact, null);
        } catch (FileNotFoundException e) {
            fail("Nulitpart should be created even in the absence of checksum file");
        }
    }

    @Test
    public void shouldCreateMultipleRequestWithChecksumValues() throws IOException {
        HttpService.HttpClientFactory factory = new HttpService.HttpClientFactory(null);
        File artifact = new File(folderToSaveDowloadFiles, "artifact");
        artifact.createNewFile();
        try {
            java.util.Properties artifactChecksums = new java.util.Properties();
            artifactChecksums.setProperty("foo.txt", "323233333");

            factory.createMultipartRequestEntity(artifact, artifactChecksums);

        } catch (FileNotFoundException e) {
            fail("Nulitpart should be created even in the absence of checksum file");
        }

    }
}
