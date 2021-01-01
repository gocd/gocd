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
package com.thoughtworks.go.server.domain;

import java.util.Objects;

public class MaterialForScheduling {
    private String fingerprint;
    private String revision;

    public MaterialForScheduling(String fingerprint, String revision) {
        this.fingerprint = fingerprint;
        this.revision = revision;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getRevision() {
        return revision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaterialForScheduling that = (MaterialForScheduling) o;
        return Objects.equals(fingerprint, that.fingerprint) &&
            Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fingerprint, revision);
    }
}
