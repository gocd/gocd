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

package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.WaitingJobPlan;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.server.domain.xml.builder.DocumentBuilder;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;
import org.dom4j.Document;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class JobPlanXmlRepresenter implements XmlRepresentable {
    private final List<WaitingJobPlan> jobPlans;

    public JobPlanXmlRepresenter(List<WaitingJobPlan> jobPlans) {
        this.jobPlans = jobPlans;
    }

    @Override
    public Document toXml(XmlWriterContext ctx) {
        DocumentBuilder builder = DocumentBuilder.withRoot("scheduledJobs");

        jobPlans.forEach(waitingJobPlan -> {
            builder.node("job", jb -> populateJob(jb, ctx, waitingJobPlan));
        });

        return builder.build();
    }

    public void populateJob(ElementBuilder builder, XmlWriterContext ctx, WaitingJobPlan waitingJobPlan) {
        JobPlan jobPlan = waitingJobPlan.jobPlan();
        builder.attr("name", jobPlan.getName())
            .link(ctx.jobXmlLink(jobPlan.getIdentifier()), "self")
            .link(ctx.jobDetailsLink(jobPlan.getIdentifier()), "alternate", jobPlan.getName() + " Job Detail", "text/html")
            .textNode("buildLocator", jobPlan.getIdentifier().buildLocator());

        if (isNotBlank(waitingJobPlan.envName())) {
            builder.textNode("environment", waitingJobPlan.envName());
        }

        if (!jobPlan.getResources().isEmpty()) {
            builder.node("resources", rb -> jobPlan.getResources().forEach(resource -> {
                rb.cdataNode("resource", resource.getName());
            }));
        }

        if (!jobPlan.getVariables().isEmpty()) {
            builder.node("environmentVariables", eb -> jobPlan.getVariables().forEach(variable -> {
                eb.node("variable", vb -> vb
                    .attr("name", variable.getName())
                    .text(variable.getDisplayValue()));
            }));
        }
    }

    @Override
    public String httpUrl(String baseUrl) {
        return null;
    }
}
