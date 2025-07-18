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
package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.domain.materials.Modification;
import org.apache.commons.lang3.Strings;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Understands objects required by domain entities to render xml representation
 */
public class XmlWriterContext {
    private final String baseUrl;
    private final ArtifactUrlReader artifactUrlReader;
    private final JobPlanLoader jobPlanLoader;

    public XmlWriterContext(String baseUrl,
                            ArtifactUrlReader artifactUrlReader,
                            JobPlanLoader jobPlanLoader) {
        this.baseUrl = stripEndSlashIfPresent(baseUrl);
        this.artifactUrlReader = artifactUrlReader;
        this.jobPlanLoader = jobPlanLoader;
    }

    public String artifactBaseUrl(JobIdentifier identifier) {
        return relative(artifactUrlReader.findArtifactUrl(identifier));
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

    public String relative(String path) {
        if (Strings.CS.startsWith(path, "/")) {
            return this.baseUrl + path;
        }
        return this.baseUrl + "/" + path;
    }

    private String stripEndSlashIfPresent(String baseUrl) {
        return Strings.CS.removeEnd(trimToEmpty(baseUrl), "/");
    }

    public String stagesXmlLink(String pipelineName) {
        return relative(format("/api/feed/pipelines/%s/stages.xml", pipelineName));
    }

    public String stagesXmlLink(String pipelineName, long beforePipelineCounter) {
        return format("%s?before=%s", stagesXmlLink(pipelineName), beforePipelineCounter);
    }

    public String stageDetailsPageLink(String stageLocator) {
        return relative(format("/pipelines/%s", stageLocator));
    }

    public String stageXmlLink(StageIdentifier stageIdentifier) {
        return stageXmlByLocator(stageIdentifier.getStageLocator());
    }

    public String stageXmlLink(Modification modification) {
        return stageXmlByLocator(modification.getRevision());
    }

    public String stageXmlByLocator(String stageLocator) {
        return relative(format("/api/feed/pipelines/%s.xml", stageLocator));
    }

    public String pipelineXmlLink(String pipelineName, long pipelineCounter) {
        return relative(format("/api/feed/pipelines/%s/%s.xml", pipelineName, pipelineCounter));
    }

    public String materialUri(String pipelineName, Integer pipelineCounter, String revision) {
        return relative(format("/api/feed/materials/%s/%s/%s.xml", pipelineName, pipelineCounter, revision));
    }

    public String jobXmlLink(JobIdentifier identifier) {
        return relative(format("/api/feed/pipelines/%s.xml", identifier.buildLocator()));
    }

    public String jobDetailsLink(JobIdentifier identifier) {
        return relative(identifier.webUrl());
    }
}
