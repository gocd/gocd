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

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.util.DateUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;

/**
 * @understands rendering xml representation of Stage
 */
public class StageXmlViewModel implements XmlRepresentable {
    private final Stage stage;

    public StageXmlViewModel(Stage stage) {
        this.stage = stage;
    }

    public static String httpUrlFor(String baseUrl, final long id) {
        return baseUrl + "/api/stages/" + id + ".xml";
    }

    @Override
    public Document toXml(XmlWriterContext writerContext) {
        DOMElement root = new DOMElement("stage");
        root.addAttribute("name", stage.getName()).addAttribute("counter", String.valueOf(stage.getCounter()));
        Document document = new DOMDocument(root);
        root.addElement("link").addAttribute("rel", "self").addAttribute("href", httpUrl(writerContext.getBaseUrl()));

        StageIdentifier stageId = stage.getIdentifier();
        root.addElement("id").addCDATA(stageId.asURN());
        String pipelineName = stageId.getPipelineName();
        root.addElement("pipeline").addAttribute("name", pipelineName)
                .addAttribute("counter", String.valueOf(stageId.getPipelineCounter()))
                .addAttribute("label", stageId.getPipelineLabel())
        .addAttribute("href", writerContext.getBaseUrl() + "/api/pipelines/" + pipelineName + "/" + stage.getPipelineId() + ".xml");

        root.addElement("updated").addText(DateUtils.formatISO8601(stage.latestTransitionDate()));

        root.addElement("result").addText(stage.getResult().toString());

        root.addElement("state").addText(stage.status());

        root.addElement("approvedBy").addCDATA(stage.getApprovedBy());

        Element jobs = root.addElement("jobs");
        for (JobInstance jobInstance : stage.getJobInstances()) {
            jobs.addElement("job").addAttribute("href", writerContext.getBaseUrl() + "/api/jobs/" + jobInstance.getId() + ".xml");
        }

        return document;
    }

    @Override
    public String httpUrl(String baseUrl) {
        return httpUrlFor(baseUrl, stage.getId());
    }
}
