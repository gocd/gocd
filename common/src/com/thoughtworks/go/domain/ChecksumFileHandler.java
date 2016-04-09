/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.log4j.Logger;

import static org.apache.commons.io.FileUtils.deleteQuietly;

public class ChecksumFileHandler implements FetchHandler {

    private final File checksumFile;
    private static final Logger LOG = Logger.getLogger(ChecksumFileHandler.class);

    public ChecksumFileHandler(File destination) {
        checksumFile = destination;
    }

    public String url(String remoteHost, String workingUrl) throws IOException {
        return String.format("%s/remoting/files/%s/%s/%s",remoteHost, workingUrl, ArtifactLogUtil.CRUISE_OUTPUT_FOLDER, ArtifactLogUtil.MD5_CHECKSUM_FILENAME);
    }

    public void handle(InputStream stream) throws IOException {
        FileUtil.writeToFile(stream, checksumFile);
    }

    public boolean handleResult(int returncode, GoPublisher goPublisher) {
        if (returncode == HttpServletResponse.SC_NOT_FOUND) {
            deleteQuietly(checksumFile);
            goPublisher.consumeLineWithPrefix("[WARN] The md5checksum property file was not found on the server. Hence, Go can not verify the integrity of the artifacts.");
            return true;
        }
        if (returncode == HttpServletResponse.SC_NOT_MODIFIED) {
            LOG.info("[Agent Fetch Artifact] Not downloading checksum file as it has not changed");
            return true;
        }
        if (returncode == HttpServletResponse.SC_OK) {
            LOG.info("[Agent Fetch Artifact] Saved checksum property file [" + checksumFile + "]");
            return true;
        }
        return returncode < HttpServletResponse.SC_BAD_REQUEST;
    }

    public void useArtifactMd5Checksums(ArtifactMd5Checksums artifactMd5Checksums) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BuildCommand toDownloadCommand(String locator, String checksumUrl, File checksumPath) {
        throw new UnsupportedOperationException("not supported for checksum handler");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChecksumFileHandler)) {
            return false;
        }

        ChecksumFileHandler that = (ChecksumFileHandler) o;

        if (checksumFile != null ? !checksumFile.equals(that.checksumFile) : that.checksumFile != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return checksumFile != null ? checksumFile.hashCode() : 0;
    }

    public ArtifactMd5Checksums getArtifactMd5Checksums() {
        return checksumFile.exists() ? new ArtifactMd5Checksums(checksumFile) : null;
    }

    public File getChecksumFile() {
        return checksumFile;
    }
}
