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

import com.thoughtworks.go.domain.DownloadAction;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.exception.ArtifactPublishingException;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.ConsoleOutputTransmitter;
import com.thoughtworks.go.remote.work.RemoteConsoleAppender;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.zip.Deflater;

import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileNameUrl;
import static com.thoughtworks.go.util.CachedDigestUtils.md5Hex;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.FileUtil.normalizePath;
import static com.thoughtworks.go.util.GoConstants.PUBLISH_MAX_RETRIES;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.removeStart;

@Component
public class GoArtifactsManipulator {
    private final HttpService httpService;
    private final URLService urlService;
    private final ZipUtil zipUtil;
    private static final Logger LOGGER = Logger.getLogger(GoArtifactsManipulator.class);

    @Autowired
    public GoArtifactsManipulator(HttpService httpService, URLService urlService, ZipUtil zipUtil) {
        this.httpService = httpService;
        this.urlService = urlService;
        this.zipUtil = zipUtil;
    }

    public void publish(DefaultGoPublisher goPublisher, String destPath, File source, JobIdentifier jobIdentifier) {
        if (!source.exists()) {
            String message = "Failed to find " + source.getAbsolutePath();
            goPublisher.consumeLineWithPrefix(message);
            bomb(message);
        }

        int publishingAttempts = 0;
        Throwable lastException = null;
        while (publishingAttempts < PUBLISH_MAX_RETRIES) {
            File tmpDir = null;
            try {
                publishingAttempts++;

                tmpDir = FileUtil.createTempFolder();
                File dataToUpload = new File(tmpDir, source.getName() + ".zip");
                zipUtil.zip(source, dataToUpload, Deflater.BEST_SPEED);

                long size = 0;
                if (source.isDirectory()) {
                    size = FileUtils.sizeOfDirectory(source);
                } else {
                    size = source.length();
                }

                goPublisher.consumeLineWithPrefix("Uploading artifacts from " + source.getAbsolutePath() + " to " + getDestPath(destPath));

                String normalizedDestPath = normalizePath(destPath);
                String url = urlService.getUploadUrlOfAgent(jobIdentifier, normalizedDestPath, publishingAttempts);

                int statusCode = httpService.upload(url, size, dataToUpload, artifactChecksums(source, normalizedDestPath));

                if (statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE) {
                    String message = String.format("Artifact upload for file %s (Size: %s) was denied by the server. This usually happens when server runs out of disk space.",
                            source.getAbsolutePath(), size);
                    goPublisher.consumeLineWithPrefix(message);
                    LOGGER.error("[Artifact Upload] Artifact upload was denied by the server. This usually happens when server runs out of disk space.");
                    publishingAttempts = PUBLISH_MAX_RETRIES;
                    bomb(message + ".  HTTP return code is " + statusCode);
                }
                if (statusCode < HttpServletResponse.SC_OK || statusCode >= HttpServletResponse.SC_MULTIPLE_CHOICES) {
                    bomb("Failed to upload " + source.getAbsolutePath() + ".  HTTP return code is " + statusCode);
                }
                return;
            } catch (Throwable e) {
                String message = "Failed to upload " + source.getAbsolutePath();
                LOGGER.error(message, e);
                goPublisher.consumeLineWithPrefix(message);
                lastException = e;
            } finally {
                FileUtil.deleteFolder(tmpDir);
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        }
    }

    private Properties artifactChecksums(File source, String destPath) throws IOException {
        if (source.isDirectory()) {
            return computeChecksumForContentsOfDirectory(source, destPath);
        }

        FileInputStream inputStream = null;
        Properties properties = null;
        try {
            inputStream = new FileInputStream(source);
            properties = computeChecksumForFile(source.getName(), md5Hex(inputStream), destPath);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return properties;
    }

    private Properties computeChecksumForContentsOfDirectory(File directory, String destPath) throws IOException {
        Collection<File> fileStructure = FileUtils.listFiles(directory, null, true);
        Properties checksumProperties = new Properties();
        for (File file : fileStructure) {
            String filePath = removeStart(file.getAbsolutePath(), directory.getParentFile().getAbsolutePath());
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                checksumProperties.setProperty(getEffectiveFileName(destPath, normalizePath(filePath)), md5Hex(inputStream));
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        return checksumProperties;
    }

    private Properties computeChecksumForFile(String sourceName, String md5, String destPath) throws IOException {
        String effectiveFileName = getEffectiveFileName(destPath, sourceName);
        Properties properties = new Properties();
        properties.setProperty(effectiveFileName, md5);
        return properties;
    }

    private String getEffectiveFileName(String computedDestPath, String filePath) {
        File artifactDest = computedDestPath.isEmpty() ? new File(filePath) : new File(computedDestPath, filePath);
        return removeLeadingSlash(artifactDest);
    }

    private String removeLeadingSlash(File artifactDest) {
        return removeStart(normalizePath(artifactDest.getPath()), "/");
    }


    public void fetch(DefaultGoPublisher goPublisher, FetchArtifactBuilder fetchArtifactBuilder) {
        try {
            String fetchMsg = String.format("Fetching artifact [%s] from [%s]", fetchArtifactBuilder.getSrc(),
                    fetchArtifactBuilder.jobLocatorForDisplay());
            goPublisher.consumeLineWithPrefix(fetchMsg);
            fetchArtifactBuilder.fetch(new DownloadAction(httpService, goPublisher, new SystemTimeClock()), urlService);
        } catch (Exception e) {
            String fetchMsg = String.format("Failed to save artifact [%s] to [%s]",
                    fetchArtifactBuilder.getSrc(), fetchArtifactBuilder.getDest());
            LOGGER.error(fetchMsg, e);
            goPublisher.consumeLineWithPrefix(fetchMsg);
            throw new RuntimeException(e);
        }
    }

    private String getDestPath(String file) {
        if (StringUtils.isEmpty(file)) {
            return "[defaultRoot]";
        } else {
            return file;
        }
    }

    public void setProperty(JobIdentifier jobIdentifier, Property property) {
        try {
            String propertiesUrl = urlService.getPropertiesUrl(jobIdentifier, property.getKey());
            httpService.postProperty(propertiesUrl, property.getValue());
        } catch (Exception e) {
            throw new ArtifactPublishingException(format("Failed to set property %s with value %s", property.getKey(), property.getValue()), e);
        }
    }


    public ConsoleOutputTransmitter createConsoleOutputTransmitter(JobIdentifier jobIdentifier,
                                                                   AgentIdentifier agentIdentifier) {
        String consoleUrl = urlService.getUploadUrlOfAgent(jobIdentifier, getConsoleOutputFolderAndFileNameUrl());
        return new ConsoleOutputTransmitter(new RemoteConsoleAppender(consoleUrl, httpService));
    }

}
