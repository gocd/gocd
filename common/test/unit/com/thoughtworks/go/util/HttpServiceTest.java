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

package com.thoughtworks.go.util;

import com.thoughtworks.go.domain.FetchHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.thoughtworks.go.util.HttpService.GO_ARTIFACT_PAYLOAD_SIZE;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class HttpServiceTest {
    private static final String NOT_EXIST_URL = "http://bjcruiselablablab";

    private File folderToSaveDowloadFiles;
    private HttpService service;
    private HttpService.HttpClientFactory httpClientFactory;
    private HttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        folderToSaveDowloadFiles = TestFileUtil.createUniqueTempFolder("HttpServiceTest");
        httpClientFactory = mock(HttpService.HttpClientFactory.class);
        httpClient = mock(HttpClient.class);
        when(httpClientFactory.httpClient()).thenReturn(httpClient);

        service = new HttpService(httpClientFactory);
    }

    @After
    public void tearDown() throws Exception {
        folderToSaveDowloadFiles.delete();
    }


    @Test
    public void shouldPostArtifactsAlongWithMD5() throws IOException {
        File uploadingFile = mock(File.class);
        java.util.Properties checksums = new java.util.Properties();

        String uploadUrl = "url";

        PostMethod mockPostMethod = mock(PostMethod.class);
        when(uploadingFile.exists()).thenReturn(true);
        when(httpClientFactory.createPost(uploadUrl)).thenReturn(mockPostMethod);

        service.upload(uploadUrl, 100L, uploadingFile, checksums);

        verify(mockPostMethod).setRequestHeader(GO_ARTIFACT_PAYLOAD_SIZE, "100");
        verify(httpClientFactory).createMultipartRequestEntity(uploadingFile, checksums, null);
        verify(httpClient).executeMethod(mockPostMethod);
    }

    @Test
    public void shouldDownloadArtifact() throws IOException {
        String url = "http://blah";
        FetchHandler fetchHandler = mock(FetchHandler.class);

        GetMethod mockGetMethod = mock(GetMethod.class);
        when(mockGetMethod.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
        when(mockGetMethod.getResponseBodyAsStream()).thenReturn(null);
        when(httpClientFactory.createGet(url)).thenReturn(mockGetMethod);

        service.download(url, fetchHandler);
        verify(httpClient).executeMethod(mockGetMethod);
        verify(fetchHandler).handle(null);
    }
    
    @Test
    public void shouldNotFailIfChecksumFileIsNotPresent() throws IOException {
        HttpService.HttpClientFactory factory = new HttpService.HttpClientFactory(null);
        File artifact = new File(folderToSaveDowloadFiles, "artifact");
        artifact.createNewFile();
        try {
            factory.createMultipartRequestEntity(artifact,null,new HttpMethodParams());
        } catch (FileNotFoundException e) {
            fail("Nulitpart should be created even in the absence of checksum file");
        }
    }

    @Test
    public void shouldSetTheAcceptHeaderWhilePostingProperties() throws Exception {
        PostMethod post = mock(PostMethod.class);
        when(httpClientFactory.createPost("url")).thenReturn(post);

        service.postProperty("url", "value");

        verify(post).setRequestHeader("GO_API","true");
        verify(post).setRequestBody(new NameValuePair[]{new NameValuePair("value", "value")});
    }

    @Test
    public void shouldCreateMultipleRequestWithChecksumValues() throws IOException {
        HttpService.HttpClientFactory factory = new HttpService.HttpClientFactory(null);
        File artifact = new File(folderToSaveDowloadFiles, "artifact");
        artifact.createNewFile();
        try {
            java.util.Properties artifactChecksums = new java.util.Properties();
            artifactChecksums.setProperty("foo.txt","323233333");

            factory.createMultipartRequestEntity(artifact, artifactChecksums, new HttpMethodParams());

        } catch (FileNotFoundException e) {
            fail("Nulitpart should be created even in the absence of checksum file");
        }

    }
}
