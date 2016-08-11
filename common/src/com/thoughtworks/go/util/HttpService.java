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

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.domain.FetchHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

@Component
public class HttpService {
    private HttpClientFactory httpClientFactory;
    private static final Log LOGGER = LogFactory.getLog(HttpService.class);
    public static final String GO_ARTIFACT_PAYLOAD_SIZE = "X-GO-ARTIFACT-SIZE";

    public HttpService() {
        this(new GoAgentServerHttpClient(new SystemEnvironment()));
    }

    @Autowired(required = false)
    public HttpService(GoAgentServerHttpClient httpClient) {
        this(new HttpClientFactory(httpClient));
    }

    HttpService(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public int upload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException {
        String absolutePath = artifactFile.getAbsolutePath();
        if (!artifactFile.exists()) {
            String message = "Failed to find file [" + absolutePath + "]";
            LOGGER.error(message);
            throw new FileNotFoundException(message);
        }
        LOGGER.info(String.format("Uploading file [%s] to url [%s]", absolutePath, url));

        HttpPost filePost = createHttpPostForUpload(url, size, artifactFile, artifactChecksums);
        try (CloseableHttpResponse response = execute(filePost)) {
            return response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            LOGGER.error("Error while uploading file [" + artifactFile.getAbsolutePath() + "]", e);
            throw e;
        } finally {
            filePost.releaseConnection();
        }
    }

    private HttpPost createHttpPostForUpload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException {
        HttpPost filePost = httpClientFactory.createPost(url);
        setSizeHeader(filePost, size);
        filePost.setHeader("Confirm", "true");
        filePost.setEntity(httpClientFactory.createMultipartRequestEntity(artifactFile, artifactChecksums));
        return filePost;
    }

    public int download(String url, FetchHandler handler) throws IOException {
        HttpGet toGet = null;
        InputStream is = null;
        try {
            toGet = httpClientFactory.createGet(url);
            PerfTimer timer = PerfTimer.start(String.format("Downloading from url [%s]", url));
            try (CloseableHttpResponse response = execute(toGet)) {
                timer.stop();
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == HttpServletResponse.SC_OK) {
                    if (response.getEntity() != null) {
                        is = response.getEntity().getContent();
                    }
                    handler.handle(is);
                }
                return statusCode;
            }
        } catch (IOException e) {
            LOGGER.error("Error while downloading [" + url + "]", e);
            throw e;
        } finally {
            IOUtils.closeQuietly(is);
            if (toGet != null) {
                toGet.releaseConnection();
            }
        }
    }

    public void postProperty(String url, String value) throws IOException {
        LOGGER.info("Posting property to the URL " + url + "Property Value =" + value);
        HttpPost post = httpClientFactory.createPost(url);
        CloseableHttpResponse response = null;
        try {
            post.setHeader("Confirm", "true");
            post.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("value", value))));
            response = execute(post);
        } finally {
            IOUtils.closeQuietly(response);
            post.releaseConnection();
        }
    }

    public CloseableHttpResponse execute(HttpRequestBase httpMethod) throws IOException {
        GoAgentServerHttpClient client = httpClientFactory.httpClient();
        CloseableHttpResponse response = client.execute(httpMethod);
        LOGGER.info("Got back " + response.getStatusLine().getStatusCode() + " from server");
        return response;
    }

    public static void setSizeHeader(HttpRequestBase method, long size) {
        method.setHeader(GO_ARTIFACT_PAYLOAD_SIZE, String.valueOf(size));
    }

    /**
     * Used to wrap the constructors in order to mock them out.
     */
    static class HttpClientFactory {
        private final GoAgentServerHttpClient httpClient;

        public HttpClientFactory(GoAgentServerHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        public GoAgentServerHttpClient httpClient() {
            return httpClient;
        }

        public HttpPost createPost(String url) {
            return new HttpPost(url);
        }

        public HttpGet createGet(String url) {
            return new HttpGet(url);
        }

        public HttpEntity createMultipartRequestEntity(File artifact, Properties artifactChecksums) throws IOException {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.addPart(GoConstants.ZIP_MULTIPART_FILENAME, new FileBody(artifact));
            if (artifactChecksums != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                artifactChecksums.store(outputStream, "");
                entityBuilder.addPart(GoConstants.CHECKSUM_MULTIPART_FILENAME, new ByteArrayBody(outputStream.toByteArray(), "checksum_file"));
            }
            return entityBuilder.build();
        }
    }
}
