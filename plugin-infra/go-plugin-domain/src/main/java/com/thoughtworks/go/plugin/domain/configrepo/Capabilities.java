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
package com.thoughtworks.go.plugin.domain.configrepo;

import java.util.Objects;

public class Capabilities {
    private boolean supportsPipelineExport;
    private boolean supportsParseContent;

    public Capabilities() {
        this(false, false);
    }

    public Capabilities(boolean supportsPipelineExport, boolean supportsParseContent) {
        this.supportsPipelineExport = supportsPipelineExport;
        this.supportsParseContent = supportsParseContent;
    }

    public boolean isSupportsPipelineExport() {
        return supportsPipelineExport;
    }

    public void setSupportsPipelineExport(boolean supportsPipelineExport) {
        this.supportsPipelineExport = supportsPipelineExport;
    }

    public boolean isSupportsParseContent() {
        return supportsParseContent;
    }

    public void setSupportsParseContent(boolean supportsParseContent) {
        this.supportsParseContent = supportsParseContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Capabilities that = (Capabilities) o;
        return supportsPipelineExport == that.supportsPipelineExport &&
                supportsParseContent == that.supportsParseContent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(supportsPipelineExport, supportsParseContent);
    }
}
