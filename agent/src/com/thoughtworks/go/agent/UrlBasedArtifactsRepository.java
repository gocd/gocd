/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.agent;

import com.thoughtworks.go.buildsession.ArtifactsRepository;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.domain.exception.ArtifactPublishingException;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.UrlUtil;
import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.util.command.StreamConsumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.zip.Deflater;

import static com.thoughtworks.go.util.CachedDigestUtils.md5Hex;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.FileUtil.normalizePath;
import static com.thoughtworks.go.util.GoConstants.PUBLISH_MAX_RETRIES;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.removeStart;

// This class is a replacement for GoArtifactsManipulator, so bear with the duplication for now
public class UrlBasedArtifactsRepository implements ArtifactsRepository {
    private static final Logger LOGGER = Logger.getLogger(UrlBasedArtifactsRepository.class);
    private final HttpService httpService;
    private final String artifactsBaseUrl;
    private String propertyBaseUrl;
    private ZipUtil zipUtil;

    public UrlBasedArtifactsRepository(HttpService httpService, String artifactsBaseUrl, String propertyBaseUrl, ZipUtil zipUtil) {
        this.httpService = httpService;
        this.artifactsBaseUrl = artifactsBaseUrl;
        this.propertyBaseUrl = propertyBaseUrl;
        this.zipUtil = zipUtil;
    }

    @Override
    public void upload(StreamConsumer console, File file, String destPath, String buildId) {
        if (!file.exists()) {
            String message = "Failed to find " + file.getAbsolutePath();
            consumeLineWithPrefix(console, message);
            throw bomb(message);
        }

        int publishingAttempts = 0;
        Throwable lastException = null;
        while (publishingAttempts < PUBLISH_MAX_RETRIES) {
            File tmpDir = null;
            try {
                publishingAttempts++;

                tmpDir = FileUtil.createTempFolder();
                File dataToUpload = new File(tmpDir, file.getName() + ".zip");
                zipUtil.zip(file, dataToUpload, Deflater.BEST_SPEED);

                long size;
                if (file.isDirectory()) {
                    size = FileUtils.sizeOfDirectory(file);
                } else {
                    size = file.length();
                }

                consumeLineWithPrefix(console,
                        format("Uploading artifacts from %s to %s", file.getAbsolutePath(), getDestPath(destPath)));

                String normalizedDestPath = normalizePath(destPath);
                String url = getUploadUrl(buildId, normalizedDestPath, publishingAttempts);

                int statusCode = httpService.upload(url, size, dataToUpload, artifactChecksums(file, normalizedDestPath));

                if (statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE) {
                    String message = format("Artifact upload for file %s (Size: %s) was denied by the server. This usually happens when server runs out of disk space.",
                            file.getAbsolutePath(), size);
                    consumeLineWithPrefix(console, message);
                    LOGGER.error("[Artifact Upload] Artifact upload was denied by the server. This usually happens when server runs out of disk space.");
                    publishingAttempts = PUBLISH_MAX_RETRIES;
                    throw bomb(message + ".  HTTP return code is " + statusCode);
                }
                if (statusCode < HttpServletResponse.SC_OK || statusCode >= HttpServletResponse.SC_MULTIPLE_CHOICES) {
                    throw bomb("Failed to upload " + file.getAbsolutePath() + ".  HTTP return code is " + statusCode);
                }
                return;
            } catch (Throwable e) {
                String message = "Failed to upload " + file.getAbsolutePath();
                LOGGER.error(message, e);
                consumeLineWithPrefix(console, message);
                lastException = e;
            } finally {
                FileUtil.deleteFolder(tmpDir);
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        }
    }

    @Override
    public void setProperty(Property property) {
        try {
            httpService.postProperty(getPropertiesUrl(property.getKey()), property.getValue());
        } catch (Exception e) {
            throw new ArtifactPublishingException(format("Failed to set property %s with value %s", property.getKey(), property.getValue()), e);
        }
    }

    private String getPropertiesUrl(String propertyName) {
        return UrlUtil.concatPath(propertyBaseUrl, UrlUtil.encodeInUtf8(propertyName));
    }

    private String getUploadUrl(String buildId, String normalizedDestPath, int publishingAttempts) {
        String path = format("%s?attempt=%d&buildId=%s", UrlUtil.encodeInUtf8(normalizedDestPath), publishingAttempts, buildId);
        return UrlUtil.concatPath(artifactsBaseUrl, path);
    }

    private void consumeLineWithPrefix(StreamConsumer console, String message) {
        console.consumeLine(format("[%s] %s", GoConstants.PRODUCT_NAME, message));
    }

    private String getDestPath(String file) {
        if (StringUtils.isEmpty(file)) {
            return "[defaultRoot]";
        } else {
            return file;
        }
    }

    private Properties artifactChecksums(File source, String destPath) throws IOException {
        if (source.isDirectory()) {
            return computeChecksumForContentsOfDirectory(source, destPath);
        }

        Properties properties;
        try (FileInputStream inputStream = new FileInputStream(source)) {
            properties = computeChecksumForFile(source.getName(), md5Hex(inputStream), destPath);
        }
        return properties;
    }

    private Properties computeChecksumForContentsOfDirectory(File directory, String destPath) throws IOException {
        Collection<File> fileStructure = FileUtils.listFiles(directory, null, true);
        Properties checksumProperties = new Properties();
        for (File file : fileStructure) {
            String filePath = removeStart(file.getAbsolutePath(), directory.getParentFile().getAbsolutePath());
            try (FileInputStream inputStream = new FileInputStream(file)) {
                checksumProperties.setProperty(getEffectiveFileName(destPath, normalizePath(filePath)), md5Hex(inputStream));
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
}
