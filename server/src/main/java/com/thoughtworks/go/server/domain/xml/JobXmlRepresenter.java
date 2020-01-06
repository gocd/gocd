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
import com.thoughtworks.go.server.domain.xml.builder.DocumentBuilder;
import org.dom4j.Document;

public class JobXmlRepresenter implements XmlRepresentable {
    private final JobInstance jobInstance;

    public JobXmlRepresenter(JobInstance jobInstance) {
        this.jobInstance = jobInstance;
    }

    @Override
    public Document toXml(XmlWriterContext ctx) {
        JobIdentifier identifier = jobInstance.getIdentifier();
        StageIdentifier stageIdentifier = identifier.getStageIdentifier();
        JobPlan jobPlan = ctx.planFor(identifier);

        return DocumentBuilder.withRoot("job")
            .attr("name", jobInstance.getName())
            .link(ctx.jobXmlLink(jobInstance.getIdentifier()), "self")
            .cdataNode("id", identifier.asURN())
            .node("pipeline", pb -> pb
                .attr("name", stageIdentifier.getPipelineName())
                .attr("counter", stageIdentifier.getPipelineCounter())
                .attr("label", stageIdentifier.getPipelineLabel())
            )
            .node("stage", sb -> sb
                .attr("name", stageIdentifier.getStageName())
                .attr("counter", stageIdentifier.getStageCounter())
                .attr("href", ctx.stageXmlLink(jobInstance.getIdentifier().getStageIdentifier()))
            )
            .textNode("result", jobInstance.getResult().toString())
            .textNode("state", jobInstance.getState().toString())
            .node("agent", ab -> ab.attr("uuid", jobInstance.getAgentUuid()))
            .comment("Artifacts of type `file` will not be shown. See https://github.com/gocd/gocd/pull/2875")
            .node("artifacts", ab -> {
                ab.attr("baseUri", ctx.artifactBaseUrl(identifier))
                    .attr("pathFromArtifactRoot", ctx.artifactRootPath(identifier));

                jobPlan.getArtifactPlansOfType(ArtifactPlanType.unit).forEach(artifactPlan -> {
                    ab.node("artifact", artifactBuilder -> artifactBuilder
                        .attr("src", artifactPlan.getSrc())
                        .attr("dest", artifactPlan.getDest())
                        .attr("type", artifactPlan.getArtifactPlanType())
                    );
                });
            })
            .comment("Resources are now intentionally left blank. See https://github.com/gocd/gocd/pull/2875")
            .emptyNode("resources")
            .comment("Environment variables are now intentionally left blank. See https://github.com/gocd/gocd/pull/2875")
            .emptyNode("environmentVariables")
            .build();
    }

    @Override
    public String httpUrl(String baseUrl) {
        return null;
    }
}
