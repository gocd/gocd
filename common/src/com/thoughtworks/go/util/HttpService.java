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
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.domain.FetchHandler;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.*;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class HttpService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpService.class);
    public static final String GO_ARTIFACT_PAYLOAD_SIZE = "X-GO-ARTIFACT-SIZE";
    private final GoAgentServerHttpClient httpClient;

    public HttpService() {
        this(new GoAgentServerHttpClient(new GoAgentServerHttpClientBuilder(new SystemEnvironment())));
    }

    @Autowired
    public HttpService(GoAgentServerHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public int upload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String absolutePath = artifactFile.getAbsolutePath();
        if (!artifactFile.exists()) {
            String message = "Failed to find file [" + absolutePath + "]";
            LOGGER.error(message);
            throw new FileNotFoundException(message);
        }
        LOGGER.info("Uploading file {} to url {}", absolutePath, url);

        Request filePost = createHttpPostForUpload(url, size, artifactFile, artifactChecksums);
        try {
            ContentResponse response = execute(filePost);
            return response.getStatus();
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            LOGGER.error("Error while uploading file {}", artifactFile.getAbsolutePath(), e);
            throw e;
        }
    }

    public void appendConsoleLog(String consoleUri, String content) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        LOGGER.debug("Appending console to URL -> {}", consoleUri);
        Request request = httpClient.newRequest(consoleUri).method(HttpMethod.PUT);
        request.header("Confirm", "true");
        setSizeHeader(request, content.getBytes().length);
        request.content(new StringContentProvider("text/plain", content, StandardCharsets.UTF_8));
        execute(request);
    }

    private Request createHttpPostForUpload(String url, long size, File artifactFile, Properties artifactChecksums) throws IOException {
        Request filePost = httpClient.newRequest(url).method(HttpMethod.POST);
        setSizeHeader(filePost, size);
        filePost.header("Confirm", "true");
        filePost.content(createMultipartRequestEntity(artifactFile, artifactChecksums));
        return filePost;
    }

    public int download(String url, FetchHandler handler) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        PerfTimer timer = PerfTimer.start(String.format("Downloading from url [%s]", url));
        try {
            Request toGet = httpClient.newRequest(url).method(HttpMethod.GET);
            InputStreamResponseListener listener = new InputStreamResponseListener();
            execute(toGet, listener);
            Response response = listener.get(15, TimeUnit.SECONDS);
            try (InputStream is = listener.getInputStream()) {
                int statusCode = response.getStatus();
                if (statusCode == HttpServletResponse.SC_OK) {
                    handler.handle(is);
                }
                return statusCode;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            LOGGER.error("Error while downloading {}", url, e);
            throw e;
        } finally {
            timer.stop();
        }
    }

    public void postProperty(String url, String value) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        LOGGER.info("Posting property {} to the URL {}", value, url);
        Request post = httpClient.newRequest(url).method(HttpMethod.POST);
        post.header("Confirm", "true");
        Fields fields = new Fields(true);
        fields.put(new Fields.Field("value", value));
        post.content(new FormContentProvider(fields));
        execute(post);
    }

    public ContentResponse execute(Request httpMethod) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ContentResponse response = httpClient.execute(httpMethod);
        LOGGER.debug("Got back {} from server", response.getStatus());
        return response;
    }

    public void execute(Request httpMethod, Response.Listener listener) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        httpMethod.onResponseHeaders(new Response.HeadersListener() {
            @Override
            public void onHeaders(Response response) {
                LOGGER.info("Got back {} from server", response.getStatus());
            }
        });
        httpClient.execute(httpMethod, listener);
    }

    public static void setSizeHeader(Request method, long size) {
        method.header(GO_ARTIFACT_PAYLOAD_SIZE, String.valueOf(size));
    }

    private ContentProvider createMultipartRequestEntity(File artifact, Properties artifactChecksums) throws IOException {
        MultiPartContentProvider contentProvider = new MultiPartContentProvider();
        contentProvider.addFilePart(GoConstants.ZIP_MULTIPART_FILENAME, artifact.getName(), new PathContentProvider(artifact.toPath()), null);

        if (artifactChecksums != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            artifactChecksums.store(outputStream, "");
            contentProvider.addFilePart(GoConstants.CHECKSUM_MULTIPART_FILENAME, "checksum_file", new BytesContentProvider(outputStream.toByteArray()), null);
        }
        return contentProvider;
    }
}
