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
package com.thoughtworks.go.server.service.dd;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import static java.lang.String.format;

public class NoCompatibleUpstreamRevisionsException extends RuntimeException {
    private NoCompatibleUpstreamRevisionsException(String message) {
        super(message);
    }

    public static NoCompatibleUpstreamRevisionsException failedToFindCompatibleRevision(CaseInsensitiveString pipelineName, DependencyMaterialConfig materialConfig) {
        return new NoCompatibleUpstreamRevisionsException(format("Failed resolution of pipeline %s as could not find compatible revision for material %s", pipelineName, materialConfig.getPipelineStageName()));
    }

    public static NoCompatibleUpstreamRevisionsException noValidRevisionsForUpstream(CaseInsensitiveString pipelineName, DependencyMaterialConfig materialConfig) {
        return new NoCompatibleUpstreamRevisionsException(format("Failed resolution of pipeline %s as no valid revisions were found for the upstream dependency %s", pipelineName, materialConfig.getPipelineStageName()));
    }

    public static NoCompatibleUpstreamRevisionsException doesNotHaveValidRevisions(CaseInsensitiveString pipelineName, MaterialConfig materialConfig) {
        return new NoCompatibleUpstreamRevisionsException(format("Failed resolution of pipeline %s as %s does not have any valid revisions.", pipelineName, materialConfig));
    }
}
