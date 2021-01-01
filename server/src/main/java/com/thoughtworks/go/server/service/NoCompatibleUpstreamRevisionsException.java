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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.materials.MaterialConfig;

public class NoCompatibleUpstreamRevisionsException extends RuntimeException {
    private NoCompatibleUpstreamRevisionsException(CaseInsensitiveString pipelineName, String message) {
        super(String.format("Failed resolution of pipeline %s : Cause : %s", pipelineName, message));
    }

    public static NoCompatibleUpstreamRevisionsException failedToFindCompatibleRevision(CaseInsensitiveString pipelineName, MaterialConfig materialConfig) {
        return new NoCompatibleUpstreamRevisionsException(pipelineName, "Could not find compatible revision for material: " + materialConfig);
    }

    public static NoCompatibleUpstreamRevisionsException noValidRevisionsForUpstream(CaseInsensitiveString pipelineName, MaterialConfig materialConfig) {
        return new NoCompatibleUpstreamRevisionsException(pipelineName, "No valid revisions found for the upstream dependency: " + materialConfig);
    }

    public static NoCompatibleUpstreamRevisionsException doesNotHaveValidRevisions(CaseInsensitiveString pipelineName, MaterialConfig materialConfig) {
        return new NoCompatibleUpstreamRevisionsException(pipelineName, String.format("Dependency material: %s does not have any valid revisions", materialConfig));
    }
}
