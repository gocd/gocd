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

import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.domain.xml.builder.DocumentBuilder;
import org.dom4j.Document;

public class PipelinesXmlRepresenter implements XmlRepresentable {
    public static final String SELF = "api/feed/pipelines.xml";
    private final PipelineInstanceModels pipelineInstanceModels;

    public PipelinesXmlRepresenter(PipelineInstanceModels pipelineInstanceModels) {
        this.pipelineInstanceModels = pipelineInstanceModels;
    }

    @Override
    public Document toXml(XmlWriterContext ctx) {
        DocumentBuilder builder = DocumentBuilder
            .withRoot("pipelines")
            .encoding("UTF-8")
            .link(ctx.relative(SELF), "self");

        pipelineInstanceModels.forEach(pipeline -> builder.node("pipeline", nodeBuilder -> {
            nodeBuilder.attr("href", ctx.stagesXmlLink(pipeline.getName()));
        }));

        return builder.build();
    }

    @Override
    public String httpUrl(String baseUrl) {
        return null;
    }
}
