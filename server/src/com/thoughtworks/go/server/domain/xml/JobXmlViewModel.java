/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain.xml;

import java.io.IOException;

import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
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

    public Document toXml(XmlWriterContext writerContext) throws DocumentException, IOException {
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

        for (Property property : writerContext.propertiesForJob(jobInstance.getId())) {
            properties.addElement("property").addAttribute("name", property.getKey()).addCDATA(property.getValue());
        }

        root.addElement("agent").addAttribute("uuid", jobInstance.getAgentUuid());

        Element artifacts = root.addElement("artifacts");
        artifacts.addAttribute("baseUri", writerContext.artifactBaseUrl(identifier)).addAttribute("pathFromArtifactRoot", writerContext.artifactRootPath(identifier));

        JobPlan jobPlan = writerContext.planFor(identifier);
        for (ArtifactPlan artifactPlan : jobPlan.getArtifactPlans()) {
            artifacts.addElement("artifact").addAttribute("src", artifactPlan.getSrc()).addAttribute("dest", artifactPlan.getDest()).addAttribute("type", artifactPlan.getArtifactType().toString());
        }

        Element resources = root.addElement("resources");

        for (Resource resource : jobPlan.getResources()) {
            resources.addElement("resource").addText(resource.getName());
        }

        Element envVars = root.addElement("environmentvariables");

        for (EnvironmentVariableConfig environmentVariableConfig : jobPlan.getVariables()) {
            envVars.addElement("variable").addAttribute("name", environmentVariableConfig.getName()).addCDATA(environmentVariableConfig.getDisplayValue());
        }

        return document;
    }

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
