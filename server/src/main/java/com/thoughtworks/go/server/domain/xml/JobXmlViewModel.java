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
package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;

/**
 * @understands rendering xml representation of Job
 */
public class JobXmlViewModel implements XmlRepresentable {
    private final JobInstance jobInstance;

    public JobXmlViewModel(JobInstance jobInstance) {
        this.jobInstance = jobInstance;
    }

    @Override
    public Document toXml(XmlWriterContext writerContext) {
        DOMElement root = new DOMElement("job");
        root.addAttribute("name", jobInstance.getName());
        Document document = new DOMDocument(root);
        root.addElement("link").addAttribute("rel", "self").addAttribute("href", httpUrl(writerContext.getBaseUrl()));

        JobIdentifier identifier = jobInstance.getIdentifier();
        root.addElement("id").addCDATA(identifier.asURN());
        String pipelineName = identifier.getPipelineName();
        StageIdentifier stageId = identifier.getStageIdentifier();

        root.addElement("pipeline").addAttribute("name", pipelineName)
                .addAttribute("counter", String.valueOf(stageId.getPipelineCounter()))
                .addAttribute("label", stageId.getPipelineLabel());

        root.addElement("stage").addAttribute("name", stageId.getStageName()).addAttribute("counter", stageId.getStageCounter()).addAttribute("href", StageXmlViewModel.httpUrlFor(
                writerContext.getBaseUrl(),
                jobInstance.getStageId()));

        root.addElement("result").addText(jobInstance.getResult().toString());

        root.addElement("state").addText(jobInstance.getState().toString());

        Element properties = root.addElement("properties");

        root.addElement("agent").addAttribute("uuid", jobInstance.getAgentUuid());

        root.addComment("artifacts of type `file` will not be shown. See https://github.com/gocd/gocd/pull/2875");
        Element artifacts = root.addElement("artifacts");
        artifacts.addAttribute("baseUri", writerContext.artifactBaseUrl(identifier)).addAttribute("pathFromArtifactRoot", writerContext.artifactRootPath(identifier));

        JobPlan jobPlan = writerContext.planFor(identifier);
        for (ArtifactPlan artifactPlan : jobPlan.getArtifactPlansOfType(ArtifactPlanType.unit)) {
            artifacts.addElement("artifact").addAttribute("src", artifactPlan.getSrc()).addAttribute("dest", artifactPlan.getDest()).addAttribute("type", artifactPlan.getArtifactPlanType().toString());
        }

        // Retain the top level elements for backward-compatibility
        root.addComment("resources are now intentionally left blank. See https://github.com/gocd/gocd/pull/2875");
        root.addElement("resources");
        root.addComment("environmentvariables are now intentionally left blank. See https://github.com/gocd/gocd/pull/2875");
        root.addElement("environmentvariables");

        return document;
    }

    @Override
    public String httpUrl(String baseUrl) {
        return baseUrl + "/api/jobs/" + jobInstance.getId() + ".xml";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobXmlViewModel that = (JobXmlViewModel) o;

        if (jobInstance != null ? !jobInstance.equals(that.jobInstance) : that.jobInstance != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return jobInstance != null ? jobInstance.hashCode() : 0;
    }
}
