/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.addon.businesscontinuity.standby.service;

import com.google.gson.Gson;
import com.thoughtworks.go.addon.businesscontinuity.AuthToken;
import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.addon.businesscontinuity.primary.ServerStatusResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Function;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class PrimaryServerCommunicationService {
    private final CloseableHttpClient httpClient;
    private PrimaryServerEndPoint primaryServerEndPoint;
    private final AuthToken authToken;
    private static final Logger LOG = LoggerFactory.getLogger(PrimaryServerCommunicationService.class);

    @Autowired
    public PrimaryServerCommunicationService(CloseableHttpClient httpClient, PrimaryServerEndPoint primaryServerEndPoint, AuthToken authToken) {
        this.httpClient = httpClient;
        this.primaryServerEndPoint = primaryServerEndPoint;
        this.authToken = authToken;
    }

    public ServerStatusResponse getLatestFileStatus() {
        return invokeHttp(primaryServerEndPoint.configFileStatus(authToken.forHttp()), "Could not fetch config status from primary server", content -> new Gson().fromJson(readWithUncheckedException(content), ServerStatusResponse.class));
    }

    public void downloadConfigFile(ConfigFileType fileType, File fileOnStandby) {
        HttpGet getMethod = primaryServerEndPoint.downloadConfigFile(fileType, authToken.forHttp());
        invokeHttp(getMethod, String.format("Could not download file '%s' from primary server", getMethod.getURI()), new CopyInputStreamToFile(fileOnStandby));
    }

    public String latestDatabaseWalLocation() {
        try {
            return invokeHttp(primaryServerEndPoint.latestDatabaseWalLocation(authToken.forHttp()), "Could not get database WAL location", this::readWithUncheckedException);
        } catch (Exception e) {
            return String.format("Not Available. Reason, %s", e.getMessage());
        }
    }

    public String primaryServerUrl() {
        return primaryServerEndPoint.primaryServerUrl();
    }

    public Map getLatestPluginsStatus() {
        HttpGet getMethod = primaryServerEndPoint.pluginsListing(authToken.forHttp());
        return invokeHttp(getMethod, "Could not fetch plugin listing from primary server", inputStream -> new Gson().fromJson(readWithUncheckedException(inputStream), Map.class));
    }

    public void downloadPlugin(String folderName, String pluginName, File file) {
        HttpGet getMethod = primaryServerEndPoint.downloadPlugin(folderName, pluginName, authToken.forHttp());
        invokeHttp(getMethod, String.format("Could not download file '%s' from primary server", getMethod.getURI()), new CopyInputStreamToFile(file));
    }

    public boolean ableToConnect() {
        try {
            return invokeHttp(primaryServerEndPoint.healthCheck(authToken.forHttp()), "Unable to connect to the health check endpoint", inputStream -> true);
        } catch (Exception e) {
            LOG.error("[Business-Continuity] Unable to connect to the primary server.", e);
            return false;
        }
    }

    private String readWithUncheckedException(InputStream content) {
        try {
            return IOUtils.toString(content, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    <T> T invokeHttp(HttpRequestBase request, String errorMessage, Function<InputStream, T> callback) {
        try (CloseableHttpResponse response = httpClient.execute(request);
             InputStream content = response.getEntity().getContent()) {
            int statusCode = response.getStatusLine().getStatusCode();
            bombIf(statusCode != HttpStatus.SC_OK, () -> errorMessage(content, 200, statusCode, errorMessage));
            return callback.apply(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String errorMessage(InputStream content, int expectedStatusCode, int statusCode, String message) {
        String responseBody;
        responseBody = readWithUncheckedException(content);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(message)
                .append(String.format(", expected primary server to respond with the HTTP status code %d but got ", expectedStatusCode))
                .append(statusCode);

        if (StringUtils.isNotBlank(responseBody)) {
            messageBuilder.append(" Error: ").append(responseBody);
        }

        return messageBuilder.toString();
    }

    private static class CopyInputStreamToFile implements Function<InputStream, Object> {
        private final File fileOnStandby;

        CopyInputStreamToFile(File fileOnStandby) {
            this.fileOnStandby = fileOnStandby;
        }

        @Override
        public Object apply(InputStream inputStream) {
            try {
                FileUtils.copyInputStreamToFile(inputStream, fileOnStandby);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        }
    }
}
