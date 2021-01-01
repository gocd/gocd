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

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.server.domain.xml.builder.DocumentBuilder;
import org.dom4j.Document;

public class StageXmlRepresenter implements XmlRepresentable {
    private final Stage stage;

    public StageXmlRepresenter(Stage stage) {
        this.stage = stage;
    }

    @Override
    public Document toXml(XmlWriterContext ctx) {
        StageIdentifier identifier = stage.getIdentifier();
        return DocumentBuilder.withRoot("stage")
            .attr("name", stage.getName())
            .attr("counter", stage.getCounter())
            .link(ctx.stageXmlLink(stage.getIdentifier()), "self")
            .cdataNode("id", identifier.asURN())
            .node("pipeline", pipelineBuilder -> pipelineBuilder
                .attr("name", identifier.getPipelineName())
                .attr("counter", identifier.getPipelineCounter())
                .attr("label", identifier.getPipelineLabel())
                .attr("href", ctx.pipelineXmlLink(identifier.getPipelineName(), identifier.getPipelineCounter()))
            )
            .textNode("updated", stage.latestTransitionDate())
            .textNode("result", stage.getResult().toString())
            .textNode("state", stage.status())
            .cdataNode("approvedBy", stage.getApprovedBy())
            .node("jobs", jobsBuilder -> {
                stage.getJobInstances().forEach(job -> {
                    jobsBuilder.node("job", jobBuilder -> jobBuilder
                        .attr("href", ctx.jobXmlLink(job.getIdentifier())));
                });
            }).build();
    }

    @Override
    public String httpUrl(String baseUrl) {
        return null;
    }
}
