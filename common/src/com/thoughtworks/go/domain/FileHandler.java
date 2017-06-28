/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.validation.ChecksumValidator;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Date;

import static com.thoughtworks.go.util.CachedDigestUtils.md5Hex;
import static com.thoughtworks.go.util.MapBuilder.map;
import static java.lang.String.format;

public class FileHandler implements FetchHandler {

    private final File artifact;
    private final String srcFile;
    private static final Logger LOG = LoggerFactory.getLogger(FileHandler.class);
    private ArtifactMd5Checksums artifactMd5Checksums;
    private ChecksumValidationPublisher checksumValidationPublisher;

    public FileHandler(File artifact, String srcFile) {
        this.artifact = artifact;
        this.srcFile = srcFile;
        checksumValidationPublisher = new ChecksumValidationPublisher();
    }

    public String url(String remoteHost, String workingUrl) throws IOException {
        boolean fileExist = artifact.exists();
        LOG.debug("Requesting the file [{}], exist? [{}]", artifact.getAbsolutePath(), fileExist);
        if (fileExist && artifact.isFile()) {
            String sha1 = StringUtil.sha1Digest(artifact);
            return format("%s/%s/%s/%s?sha1=%s", remoteHost, "remoting", "files", workingUrl,
                    URLEncoder.encode(sha1, "UTF-8"));
        } else {
            return format("%s/%s/%s/%s", remoteHost, "remoting", "files", workingUrl);
        }
    }

    public void handle(InputStream stream) throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = FileUtils.openOutputStream(artifact);
            LOG.info("[Artifact File Download] [{}] Download of artifact {} started", new Date(), artifact.getName());
            IOUtils.copyLarge(stream, fileOutputStream);
            LOG.info("[Artifact File Download] [{}] Download of artifact {} ended", new Date(), artifact.getName());
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(artifact);
            LOG.info("[Artifact File Download] [{}] Checksum computation of artifact {} started", new Date(), artifact.getName());
            String artifactMD5 = md5Hex(inputStream);
            new ChecksumValidator(artifactMd5Checksums).validate(srcFile, artifactMD5, checksumValidationPublisher);
            LOG.info("[Artifact File Download] [{}] Checksum computation of artifact {} ended", new Date(), artifact.getName());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public boolean handleResult(int httpCode, GoPublisher goPublisher) {
        checksumValidationPublisher.publish(httpCode, artifact, goPublisher);

        return httpCode < HttpServletResponse.SC_BAD_REQUEST;
    }

    public void useArtifactMd5Checksums(ArtifactMd5Checksums artifactMd5Checksums) {
        this.artifactMd5Checksums = artifactMd5Checksums;
    }

    @Override
    public BuildCommand toDownloadCommand(String locator, String checksumUrl, File checksumPath) {
        return BuildCommand.downloadFile(map(
                "url", format("/remoting/files/%s", locator),
                "checksumUrl", checksumUrl,
                "checksumFile", checksumPath.getPath(),
                "dest", artifact.getPath(),
                "src", srcFile
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileHandler)) {
            return false;
        }

        FileHandler that = (FileHandler) o;

        if (artifact != null ? !artifact.equals(that.artifact) : that.artifact != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return artifact != null ? artifact.hashCode() : 0;
    }
}
