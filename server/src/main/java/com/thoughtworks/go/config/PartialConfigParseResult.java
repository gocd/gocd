/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;

import java.util.Objects;

public class PartialConfigParseResult {
    private final String revision;
    private PartialConfig lastSuccess;
    private Exception lastFailure;

    public PartialConfigParseResult(String revision, PartialConfig newPart) {
        this.revision = revision;
        this.lastSuccess = newPart;
    }

    public PartialConfigParseResult(String revision, Exception ex) {
        this.revision = revision;
        this.lastFailure = ex;
    }

    public boolean isSuccessful() {
        return null == getLastFailure();
    }

    public PartialConfig getLastSuccess() {
        return lastSuccess;
    }

    public Exception getLastFailure() {
        return lastFailure;
    }

    public String getRevision() {
        return revision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialConfigParseResult that = (PartialConfigParseResult) o;
        return Objects.equals(revision, that.revision) &&
                Objects.equals(lastSuccess, that.lastSuccess) &&
                Objects.equals(lastFailure, that.lastFailure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, lastSuccess, lastFailure);
    }
}
