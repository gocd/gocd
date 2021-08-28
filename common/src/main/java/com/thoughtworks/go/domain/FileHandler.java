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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.validation.ChecksumValidator;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

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

    @Override
    public String url(String remoteHost, String workingUrl) throws IOException {
        boolean fileExist = artifact.exists();
        LOG.debug("Requesting the file [{}], exist? [{}]", artifact.getAbsolutePath(), fileExist);
        if (fileExist && artifact.isFile()) {
            String sha1 = FileUtil.sha1Digest(artifact);
            return format("%s/%s/%s/%s?sha1=%s", remoteHost, "remoting", "files", workingUrl,
                    URLEncoder.encode(sha1, StandardCharsets.UTF_8));
        } else {
            return format("%s/%s/%s/%s", remoteHost, "remoting", "files", workingUrl);
        }
    }

    @Override
    public void handle(InputStream stream) throws IOException {
        MessageDigest digest = getMd5();
        try (DigestInputStream digestInputStream = new DigestInputStream(stream, digest)) {
            LOG.info("[Artifact File Download] [{}] Download of artifact {} started", new Date(), artifact.getName());
            FileUtils.copyInputStreamToFile(digestInputStream, artifact);
            LOG.info("[Artifact File Download] [{}] Download of artifact {} ended", new Date(), artifact.getName());
        }

        String artifactMD5 = Hex.encodeHexString(digest.digest());
        new ChecksumValidator(artifactMd5Checksums).validate(srcFile, artifactMD5, checksumValidationPublisher);
    }

    private MessageDigest getMd5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean handleResult(int httpCode, GoPublisher goPublisher) {
        checksumValidationPublisher.publish(httpCode, artifact, goPublisher);

        return httpCode < HttpServletResponse.SC_BAD_REQUEST;
    }

    @Override
    public void useArtifactMd5Checksums(ArtifactMd5Checksums artifactMd5Checksums) {
        this.artifactMd5Checksums = artifactMd5Checksums;
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
