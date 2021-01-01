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

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.server.domain.xml.builder.DocumentBuilder;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;
import org.dom4j.Document;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;

public class FeedEntriesRepresenter implements XmlRepresentable {
    private final String pipelineName;
    private final FeedEntries feedEntries;

    public FeedEntriesRepresenter(String pipelineName, FeedEntries feedEntries) {
        this.pipelineName = pipelineName;
        this.feedEntries = feedEntries;
    }

    @Override
    public Document toXml(XmlWriterContext ctx) {
        String selfUrl = ctx.stagesXmlLink(pipelineName);
        DocumentBuilder documentBuilder = DocumentBuilder
            .withRoot("feed", "http://www.w3.org/2005/Atom")
            .encoding("UTF-8")
            .additionalNamespace("go", "http://www.thoughtworks-studios.com/ns/go")
            .cdataNode("title", pipelineName)
            .textNode("id", selfUrl)
            .node("author", builder -> builder.textNode("name", "Go"))
            .textNode("updated", feedEntries.lastUpdatedDate())
            .link(selfUrl, "self");

        if (feedEntries.last() != null) {
            StageFeedEntry last = (StageFeedEntry) feedEntries.last();
            documentBuilder.link(ctx.stagesXmlLink(pipelineName, last.getStageIdentifier().getPipelineCounter()), "next");
        }

        feedEntries.forEach(feed -> documentBuilder.node("entry", builder -> {
            this.addEntry((StageFeedEntry) feed, builder, ctx);
        }));

        return documentBuilder.build();
    }

    private void addEntry(StageFeedEntry feed, ElementBuilder builder, XmlWriterContext ctx) {
        StageIdentifier identifier = feed.getStageIdentifier();
        String entryUrl = ctx.stageDetailsPageLink(identifier.getStageLocator());
        builder.cdataNode("title", feed.getTitle())
            .textNode("updated", feed.getUpdatedDate())
            .textNode("id", entryUrl);

        if (feed.isManuallyTriggered()) {
            builder.node("go:author", childBuilder -> childBuilder.cdataNode("go:name", feed.getApprovedBy()));
        }

        feed.getAuthors().forEach(author -> {
            builder.node("author", child -> {
                child.cdataNode("name", author.getName());
                if (isNotBlank(author.getEmail())) {
                    child.textNode("email", author.getEmail());
                }
            });
        });

        if (isNotBlank(feed.getCancelledBy())) {
            builder.node("cancelledBy", child -> child.cdataNode("go:name", feed.getCancelledBy()));
        }

        String stageTitle = identifier.getStageName() + " Stage Detail";
        String pipelineTitle = identifier.getPipelineName() + " Pipeline Detail";
        String stageXmlHref = ctx.stageXmlLink(feed.getStageIdentifier());
        String pipelineXmlHref = ctx.pipelineXmlLink(this.pipelineName, identifier.getPipelineCounter());
        String goRelationsUrl = "http://www.thoughtworks-studios.com/ns/relations/go/pipeline";

        builder.link(stageXmlHref, "alternate", stageTitle, "application/vnd.go+xml")
            .link(entryUrl, "alternate", stageTitle, "text/html")
            .link(pipelineXmlHref, goRelationsUrl, pipelineTitle, "application/vnd.go+xml")
            .link(entryUrl, goRelationsUrl, pipelineTitle, "text/html");

        addCategory(builder, "stage", "Stage");
        addCategory(builder, "completed", "Completed");
        addCategory(builder, lowerCase(feed.getResult()), feed.getResult());
    }

    private void addCategory(ElementBuilder builder, String term, String label) {
        builder.node("category", nodeBuilder -> {
            nodeBuilder.attr("scheme", "http://www.thoughtworks-studios.com/ns/categories/go")
                .attr("term", term)
                .attr("label", label);
        });
    }

    @Override
    public String httpUrl(String baseUrl) {
        return null;
    }
}
