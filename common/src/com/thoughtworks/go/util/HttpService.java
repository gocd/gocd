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
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

@Component
public class HttpService {
    private HttpClientFactory httpClientFactory;
    private static final Log LOGGER = LogFactory.getLog(HttpService.class);
    public static final String GO_ARTIFACT_PAYLOAD_SIZE = "X-GO-ARTIFACT-SIZE";

    public HttpService() {
        this(new HttpClientFactory(new HttpClient()));
    }

    @Autowired(required = false)
    public HttpService(HttpClient httpClient) {
        this(new HttpClientFactory(httpClient));
    }

    HttpService(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public int upload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException {
        String absolutePath = artifactFile.getAbsolutePath();
        if (!artifactFile.exists()) {
            LOGGER.error("Failed to find file [" + absolutePath + "]");
            return 0;
        }
        LOGGER.info(String.format("Uploading file [%s] to url [%s]", absolutePath, url));

        PostMethod filePost = createPostMethodForUpload(url, size, artifactFile, artifactChecksums);
        try {
            return execute(filePost);
        } catch (IOException e) {
            LOGGER.error("Error while uploading file [" + artifactFile.getAbsolutePath() + "]", e);
            throw e;
        } finally {
            filePost.releaseConnection();
        }
    }

    private PostMethod createPostMethodForUpload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException {
        PostMethod filePost = httpClientFactory.createPost(url);
        setSizeHeader(filePost, size);
        filePost.setRequestEntity(httpClientFactory.createMultipartRequestEntity(artifactFile, artifactChecksums, filePost.getParams()));
        return filePost;
    }

    public int download(String url, FetchHandler handler) throws IOException {
        GetMethod toGet = null;
        InputStream is = null;
        try {
            toGet = httpClientFactory.createGet(url);
            PerfTimer timer = PerfTimer.start(String.format("Downloading from url [%s]", url));
            execute(toGet);
            timer.stop();
            int statusCode = toGet.getStatusCode();

            if (statusCode == HttpServletResponse.SC_OK) {
                is = toGet.getResponseBodyAsStream();
                handler.handle(is);
            }
            return statusCode;
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
        PostMethod post = httpClientFactory.createPost(url);
        try {
            post.setRequestHeader("Confirm", "true");
            post.setRequestBody(new NameValuePair[]{new NameValuePair("value", value)});
            execute(post);
        } finally {
            post.releaseConnection();
        }
    }

    private int execute(HttpMethod httpMethod) throws IOException {
        HttpClient client = httpClientFactory.httpClient();
        int httpStatus = client.executeMethod(httpMethod);
        LOGGER.info("Got back " + httpStatus + " from server");
        return httpStatus;
    }

    public HttpClient httpClient() {
        return httpClientFactory.httpClient();
    }

    public static void setSizeHeader(HttpMethod method, long size) {
        method.setRequestHeader(GO_ARTIFACT_PAYLOAD_SIZE, String.valueOf(size));
    }

    /**
     * Used to wrap the constructors in order to mock them out.
     */
    public static class HttpClientFactory {
        private final HttpClient httpClient;

        public HttpClientFactory(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        public HttpClient httpClient() {
            return httpClient;
        }

        public PostMethod createPost(String url) {
            PostMethod postMethod = new PostMethod(url);
            postMethod.getParams().setParameter("http.tcp.nodelay", true);
            return postMethod;
        }

        public GetMethod createGet(String url) {
            GetMethod getMethod = new GetMethod(url);
            getMethod.getParams().setParameter("http.tcp.nodelay", true);
            return getMethod;
        }

        public MultipartRequestEntity createMultipartRequestEntity(File artifact, Properties artifactChecksums, HttpMethodParams methodParams) throws IOException {
            ArrayList<Part> parts = new ArrayList<>();
            parts.add(new FilePart(GoConstants.ZIP_MULTIPART_FILENAME, artifact));
            if (artifactChecksums != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                artifactChecksums.store(outputStream,"");
                parts.add(new FilePart(GoConstants.CHECKSUM_MULTIPART_FILENAME,new ByteArrayPartSource("checksum_file",outputStream.toByteArray())));
            }
            return new MultipartRequestEntity(parts.toArray(new Part[]{}), methodParams);
        }
    }
}
