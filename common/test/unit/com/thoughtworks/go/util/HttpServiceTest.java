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

package com.thoughtworks.go.util;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.domain.FetchHandler;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.thoughtworks.go.util.HttpService.GO_ARTIFACT_PAYLOAD_SIZE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class HttpServiceTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort(), true);

    private File folderToSaveDowloadFiles;
    private HttpService service;
    private GoAgentServerHttpClient httpClient;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        folderToSaveDowloadFiles = temporaryFolder.newFolder();
        httpClient = new GoAgentServerHttpClient(new GoAgentServerHttpClientBuilder(new SystemEnvironment()));
        httpClient.init();
        service = new HttpService(httpClient);
    }

    @After
    public void tearDown() throws Exception {
        httpClient.destroy();
    }

    @Test
    public void shouldPostArtifactsAlongWithMD5() throws Exception {
        File uploadingFile = temporaryFolder.newFile();
        String fileContents = randomFileContents();

        stubFor(post(urlPathEqualTo("/foo"))
                .willReturn(aResponse()
                        .withStatus(200))
        );

        FileUtils.write(uploadingFile, fileContents, UTF_8);
        java.util.Properties checksums = new java.util.Properties();
        checksums.put("foo", "bar");

        service.upload("http://localhost:" + wireMockRule.port() + "/foo", fileContents.length(), uploadingFile, checksums);

        LoggedRequest loggedRequest = wireMockRule.findAll(
                postRequestedFor(urlPathEqualTo("/foo"))
                        .withHeader(GO_ARTIFACT_PAYLOAD_SIZE, equalTo(Integer.toString(fileContents.length())))
                        .withHeader("Confirm", equalTo("true"))
        ).get(0);

        FileUpload fileUpload = new FileUpload(new DiskFileItemFactory(1024 * 1024, temporaryFolder.newFolder("uploads")));
        List<FileItem> fileItems = fileUpload.parseRequest(new LoggedRequestBasedContext(loggedRequest));

        assertThat(fileItems, hasSize(2));

        assertThat(fileItems.get(0).getString("utf-8"), is(fileContents));
        assertThat(fileItems.get(0).getFieldName(), is("zipfile"));
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(fileItems.get(1).get()));
        assertThat(properties, is(checksums));
        assertThat(fileItems.get(1).getFieldName(), is("file_checksum"));
    }

    @Test
    public void shouldDownloadArtifact() throws Exception {
        String url = "http://localhost:" + wireMockRule.port() + "/download";

        String fileContents = randomFileContents();
        stubFor(get(urlPathEqualTo("/download"))
                .willReturn(aResponse()
                        .withStatus(200).withBody(fileContents)
                ));
        FetchHandler fetchHandler = mock(FetchHandler.class);

        ArgumentCaptor<InputStream> argumentCaptor = ArgumentCaptor.forClass(InputStream.class);
        service.download(url, fetchHandler);
        Mockito.verify(fetchHandler).handle(argumentCaptor.capture());

        InputStream value = argumentCaptor.getValue();
        assertThat(IOUtils.toString(value, StandardCharsets.UTF_8), is(fileContents));
    }

    @Test
    public void shouldNotFailIfChecksumFileIsNotPresent() throws Exception {
        File uploadingFile = temporaryFolder.newFile();
        String fileContents = randomFileContents();

        stubFor(post(urlPathEqualTo("/foo"))
                .willReturn(aResponse()
                        .withStatus(200))
        );

        FileUtils.write(uploadingFile, fileContents, UTF_8);
        service.upload("http://localhost:" + wireMockRule.port() + "/foo", fileContents.length(), uploadingFile, null);

        LoggedRequest loggedRequest = wireMockRule.findAll(
                postRequestedFor(urlPathEqualTo("/foo"))
                        .withHeader(GO_ARTIFACT_PAYLOAD_SIZE, equalTo(Integer.toString(fileContents.length())))
                        .withHeader("Confirm", equalTo("true"))
        ).get(0);

        FileUpload fileUpload = new FileUpload(new DiskFileItemFactory(1024 * 1024, temporaryFolder.newFolder("uploads")));
        List<FileItem> fileItems = fileUpload.parseRequest(new LoggedRequestBasedContext(loggedRequest));

        assertThat(fileItems, hasSize(1));
        assertThat(fileItems.get(0).getString("utf-8"), is(fileContents));
        assertThat(fileItems.get(0).getFieldName(), is("zipfile"));
    }

    @Test
    public void shouldSetTheConfirmHeaderWhilePostingProperties() throws Exception {
        stubFor(post(urlPathEqualTo("/property"))
                .willReturn(aResponse()
                        .withStatus(200))
        );

        service.postProperty("http://localhost:" + wireMockRule.port() + "/property", "some-property-value");

        verify(postRequestedFor(urlPathEqualTo("/property"))
                .withHeader("Confirm", equalTo("true"))
                .withRequestBody(equalTo("value=some-property-value"))
        );
    }

    @Test
    public void shouldAppendConsoleLog() throws Exception {
        stubFor(put(urlPathEqualTo("/console-log"))
                .willReturn(aResponse()
                        .withStatus(200))
        );

        // testing with some multi byte unicode chars
        String content = "some console \u040A log contents \u20AC";

        service.appendConsoleLog("http://localhost:" + wireMockRule.port() + "/console-log", content);
        verify(putRequestedFor(urlPathEqualTo("/console-log"))
                .withHeader("Confirm", equalTo("true"))
                .withHeader("Content-Length", equalTo(Integer.toString(content.getBytes(UTF_8).length)))
                .withRequestBody(equalTo(content))
        );
    }

    private String randomFileContents() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    private static class LoggedRequestBasedContext implements RequestContext {
        private final LoggedRequest loggedRequest;

        public LoggedRequestBasedContext(LoggedRequest loggedRequest) {
            this.loggedRequest = loggedRequest;
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public String getContentType() {
            return loggedRequest.getHeader("Content-Type");
        }

        @Override
        public int getContentLength() {
            return -1;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(loggedRequest.getBody());
        }
    }

}
