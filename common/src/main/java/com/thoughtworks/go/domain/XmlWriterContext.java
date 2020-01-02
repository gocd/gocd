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

import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
* @understands objects required by domain entities to render xml representation
*/
public class XmlWriterContext {
    private final String baseUrl;
    private final ArtifactUrlReader artifactUrlReader;
    private final JobPlanLoader jobPlanLoader;
    private final StageFinder stageFinder;

    public XmlWriterContext(String baseUrl, ArtifactUrlReader artifactUrlReader, JobPlanLoader jobPlanLoader, StageFinder stageFinder) {
        this.baseUrl = baseUrl;
        this.artifactUrlReader = artifactUrlReader;
        this.jobPlanLoader = jobPlanLoader;
        this.stageFinder = stageFinder;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String artifactBaseUrl(JobIdentifier identifier) {
        return getBaseUrl() + artifactUrlReader.findArtifactUrl(identifier);
    }

    public String artifactRootPath(JobIdentifier identifier) {
        try {
            return artifactUrlReader.findArtifactRoot(identifier);
        } catch (IllegalArtifactLocationException e) {
            throw bomb(e);
        }
    }

    public JobPlan planFor(JobIdentifier jobId) {
        return jobPlanLoader.loadOriginalJobPlan(jobId);
    }

    public long stageIdForLocator(String locator) {
        return stageFinder.findStageWithIdentifier(new StageIdentifier(locator)).getId();
    }
}
