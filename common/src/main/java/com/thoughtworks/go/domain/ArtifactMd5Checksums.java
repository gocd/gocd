/*
 * Copyright Thoughtworks, Inc.
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

import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.util.Objects;
import java.util.Properties;

public class ArtifactMd5Checksums implements Serializable {

    private final Properties checksumProperties;

    public ArtifactMd5Checksums(File checksumProperties) {
        this.checksumProperties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(checksumProperties))) {
            this.checksumProperties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(String.format("[Checksum Verification] Could not load the MD5 from the checksum file '%s'", checksumProperties), e);
        }
    }

    @TestOnly
    public ArtifactMd5Checksums(Properties checksumProperties) {
        this.checksumProperties = checksumProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ArtifactMd5Checksums that && Objects.equals(checksumProperties, that.checksumProperties);
    }

    @Override
    public int hashCode() {
        return checksumProperties != null ? checksumProperties.hashCode() : 0;
    }

    public String md5For(String artifactPath) {
        return checksumProperties.getProperty(artifactPath);
    }
}
