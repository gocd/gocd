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
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;

import java.util.List;

public class JobPlanXmlViewModel implements XmlRepresentable {
    private final List<WaitingJobPlan> jobPlans;

    public JobPlanXmlViewModel(List<WaitingJobPlan> jobPlans) {
        this.jobPlans = jobPlans;
    }

    @Override
    public Document toXml(XmlWriterContext writerContext) {
        DOMElement root = new DOMElement("scheduledJobs");
        for (WaitingJobPlan jobPlan : jobPlans) {
            DOMElement jobElement = getXmlForJobPlan(writerContext, jobPlan);
            root.add(jobElement);
        }
        DOMDocument domDocument = new DOMDocument(root);
        return domDocument;
    }

    private DOMElement getXmlForJobPlan(XmlWriterContext writerContext, WaitingJobPlan waitingJobPlan) {
        JobPlan jobPlan = waitingJobPlan.jobPlan();
        DOMElement root = new DOMElement("job");
        root.addAttribute("name", jobPlan.getName()).addAttribute("id", String.valueOf(jobPlan.getJobId()));
        root.addElement("link").addAttribute("rel", "self").addAttribute("href", httpUrlFor(writerContext.getBaseUrl(), jobPlan.getIdentifier()));
        root.addElement("buildLocator").addText(jobPlan.getIdentifier().buildLocator());
        if (!StringUtils.isBlank(waitingJobPlan.envName())) {
            root.addElement("environment").addText(waitingJobPlan.envName());
        }

        if (!jobPlan.getResources().isEmpty()) {
            DOMElement resources = new DOMElement("resources");

            for (Resource resource : jobPlan.getResources()) {
                resources.addElement("resource").addCDATA(resource.getName());
            }
            root.add(resources);
        }

        if (!jobPlan.getVariables().isEmpty()) {
            DOMElement envVars = new DOMElement("environmentVariables");
            for (EnvironmentVariable environmentVariable : jobPlan.getVariables()) {
                envVars.addElement("variable").addAttribute("name", environmentVariable.getName()).addText(environmentVariable.getDisplayValue());
            }
            root.add(envVars);
        }

        return root;
    }

    @Override
    public String httpUrl(String baseUrl) {
        throw new RuntimeException("Unimplemented");
    }

    private String httpUrlFor(String baseUrl, final JobIdentifier identifier) {
        return baseUrl + "/tab/build/detail/" + identifier.buildLocator();
    }
}
