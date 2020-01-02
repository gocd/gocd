/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.ZipUtil;
import com.thoughtworks.go.validation.ChecksumValidator;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.CachedDigestUtils.md5Hex;
import static java.lang.String.format;

public class DirHandler implements FetchHandler {
    private final String srcFile;
    private final File destOnAgent;
    private static final Logger LOG = LoggerFactory.getLogger(DirHandler.class);
    private ArtifactMd5Checksums artifactMd5Checksums;
    private ChecksumValidationPublisher checksumValidationPublisher;

    public DirHandler(String srcFile, File destOnAgent) {
        this.srcFile = srcFile;
        this.destOnAgent = destOnAgent;
        checksumValidationPublisher = new ChecksumValidationPublisher();
    }

    @Override
    public String url(String remoteHost, String workingUrl) {
        return format("%s/%s/%s/%s.zip", remoteHost, "remoting", "files", workingUrl);
    }

    @Override
    public void handle(InputStream stream) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(stream)) {
            LOG.info("[Agent Fetch Artifact] Downloading from '{}' to '{}'. Will read from Socket stream to compute MD5 and write to file", srcFile, destOnAgent.getAbsolutePath());

            long before = System.currentTimeMillis();
            new ZipUtil((entry, stream1) -> {
                LOG.info("[Agent Fetch Artifact] Downloading a directory from '{}' to '{}'. Handling the entry: '{}'", srcFile, destOnAgent.getAbsolutePath(), entry.getName());
                new ChecksumValidator(artifactMd5Checksums).validate(getSrcFilePath(entry), md5Hex(stream1), checksumValidationPublisher);
            }).unzip(zipInputStream, destOnAgent);
            LOG.info("[Agent Fetch Artifact] Downloading a directory from '{}' to '{}'. Took: {}ms", srcFile, destOnAgent.getAbsolutePath(), System.currentTimeMillis() - before);
        }
    }

    private String getSrcFilePath(ZipEntry entry) {
        String parent = new File(srcFile).getParent();
        return FilenameUtils.separatorsToUnix(new File(parent, entry.getName()).getPath());
    }

    @Override
    public boolean handleResult(int httpCode, GoPublisher goPublisher) {
        checksumValidationPublisher.publish(httpCode, destOnAgent, goPublisher);
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
        if (!(o instanceof DirHandler)) {
            return false;
        }

        DirHandler that = (DirHandler) o;

        if (destOnAgent != null ? !destOnAgent.equals(that.destOnAgent) : that.destOnAgent != null) {
            return false;
        }
        if (srcFile != null ? !srcFile.equals(that.srcFile) : that.srcFile != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = srcFile != null ? srcFile.hashCode() : 0;
        result = 31 * result + (destOnAgent != null ? destOnAgent.hashCode() : 0);
        return result;
    }
}
