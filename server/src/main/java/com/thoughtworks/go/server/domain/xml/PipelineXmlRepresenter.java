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

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.domain.xml.builder.DocumentBuilder;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;
import com.thoughtworks.go.server.domain.xml.materials.MaterialXmlRepresenter;
import com.thoughtworks.go.util.DateUtils;
import org.dom4j.Document;

public class PipelineXmlRepresenter implements XmlRepresentable {
    private final PipelineInstanceModel instance;

    public PipelineXmlRepresenter(PipelineInstanceModel instance) {
        this.instance = instance;
    }

    @Override
    public Document toXml(XmlWriterContext ctx) {
        String self = ctx.pipelineXmlLink(instance.getName(), instance.getCounter());

        DocumentBuilder builder = DocumentBuilder.withRoot("pipeline")
            .attr("name", instance.getName())
            .attr("counter", instance.getCounter())
            .link(self, "self")
            .cdataNode("id", instance.getPipelineIdentifier().asURN())
            .textNode("scheduleTime", DateUtils.formatISO8601(instance.getScheduledDate()));

        PipelineTimelineEntry pipelineAfter = instance.getPipelineAfter();
        if (pipelineAfter != null) {
            builder.link(ctx.pipelineXmlLink(pipelineAfter.getPipelineName(), pipelineAfter.getId()), "insertedBefore");
        }

        PipelineTimelineEntry pipelineBefore = instance.getPipelineBefore();
        if (pipelineBefore != null) {
            builder.link(ctx.pipelineXmlLink(pipelineBefore.getPipelineName(), pipelineBefore.getId()), "insertedAfter");
        }

        builder.node("materials", materialBuilder -> {
            instance.getLatestRevisions().forEach(revision -> {
                this.populateMaterials(materialBuilder, revision, ctx);
            });
        });

        builder.node("stages", stagesBuilder -> {
            instance.getStageHistory().stream()
                .filter(stage -> !(stage instanceof NullStageHistoryItem))
                .forEach(stage -> stagesBuilder.node("stage", stageBuilder -> stageBuilder
                    .attr("href", ctx.stageXmlLink(stage.getIdentifier())))
                );
        });

        builder.cdataNode("approvedBy", instance.getApprovedBy());
        return builder.build();
    }

    private void populateMaterials(ElementBuilder builder, MaterialRevision revision, XmlWriterContext ctx) {
        builder.node("material", materialBuilder -> MaterialXmlRepresenter
            .representerFor(instance.getName(), instance.getCounter(), revision)
            .populate(materialBuilder, ctx)
        );
    }

    @Override
    public String httpUrl(String baseUrl) {
        return null;
    }
}
