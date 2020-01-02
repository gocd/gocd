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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class ArtifactMd5Checksums implements Serializable {

    private final Properties checksumProperties;

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactMd5Checksums.class);

    public ArtifactMd5Checksums(File checksumProperties) {
        this.checksumProperties = new Properties();
        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader(checksumProperties);
            reader = new BufferedReader(fileReader);
            this.checksumProperties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(String.format("[Checksum Verification] Could not load the MD5 from the checksum file '%s'", checksumProperties), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close buffered reader for checksum file: {}", checksumProperties.getAbsolutePath(), e);
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close file-reader for checksum file: {}", checksumProperties.getAbsolutePath(), e);
                }
            }
        }
    }

    //Used only in tests
    public ArtifactMd5Checksums(Properties checksumProperties) {
        this.checksumProperties = checksumProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArtifactMd5Checksums)) {
            return false;
        }

        ArtifactMd5Checksums that = (ArtifactMd5Checksums) o;

        if (checksumProperties != null ? !checksumProperties.equals(that.checksumProperties) : that.checksumProperties != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return checksumProperties != null ? checksumProperties.hashCode() : 0;
    }

    public String md5For(String artifactPath) {
        return checksumProperties.getProperty(artifactPath);
    }
}
