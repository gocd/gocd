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

import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.util.DateUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;

/**
 * @understands rendering xml representation of Pipeline
 */
public class PipelineXmlViewModel implements XmlRepresentable {
    private final PipelineInstanceModel pipeline;

    public PipelineXmlViewModel(PipelineInstanceModel pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public Document toXml(XmlWriterContext writerContext) {
        DOMElement root = new DOMElement("pipeline");
        root.addAttribute("name", pipeline.getName()).addAttribute("counter", String.valueOf(pipeline.getCounter())).addAttribute("label", pipeline.getLabel());
        Document document = new DOMDocument(root);
        String baseUrl = writerContext.getBaseUrl();
        root.addElement("link").addAttribute("rel", "self").addAttribute("href", httpUrl(baseUrl));

        root.addElement("id").addCDATA(pipeline.getPipelineIdentifier().asURN());
        PipelineTimelineEntry pipelineAfter = pipeline.getPipelineAfter();
        if (pipelineAfter != null) {
            addTimelineLink(root, baseUrl, "insertedBefore", pipelineAfter);
        }
        PipelineTimelineEntry pipelineBefore = pipeline.getPipelineBefore();
        if (pipelineBefore != null) {
            addTimelineLink(root, baseUrl, "insertedAfter", pipelineBefore);
        }

        root.addElement("scheduleTime").addText(DateUtils.formatISO8601(pipeline.getScheduledDate()));

        Element materials = root.addElement("materials");

        for (MaterialRevision materialRevision : pipeline.getCurrentRevisions()) {
            populateXml(materials, materialRevision, writerContext);

        }

        Element stages = root.addElement("stages");
        for (StageInstanceModel stage : pipeline.getStageHistory()) {
            if (! (stage instanceof NullStageHistoryItem)) {
                stages.addElement("stage").addAttribute("href", StageXmlViewModel.httpUrlFor(writerContext.getBaseUrl(), stage.getId()));
            }
        }

        root.addElement("approvedBy").addCDATA(pipeline.getApprovedBy());
        return document;
    }

    public abstract static class MaterialXmlViewModel {
        final AbstractMaterial material;

        static MaterialXmlViewModel viewModelFor(Material material) {
            if(material instanceof ScmMaterial || material instanceof PluggableSCMMaterial) return new ScmXmlViewModel(material);
            if(material instanceof DependencyMaterial) return new DependencyXmlViewModel(material);
            if(material instanceof PackageMaterial) return new PackageXmlViewModel(material);
            throw new RuntimeException("Unknown material type");
        }

        MaterialXmlViewModel(Material material) {
            this.material = (AbstractMaterial) material;
        }

        void populateXml(Element materials, Modifications modifications, XmlWriterContext writerContext) {
            Element materialElement = materials.addElement("material");
            materialElement.addAttribute("materialUri", writerContext.getBaseUrl() + "/api/materials/" + material.getId() + ".xml");
            for (Map.Entry<String, Object> criterion : material.getAttributesForXml().entrySet()) {
                if (criterion.getValue() != null) {
                    materialElement.addAttribute(criterion.getKey(), criterion.getValue().toString());
                }
            }
            Element modificationsTag = materialElement.addElement("modifications");
            populateXmlForModifications(modifications, writerContext, modificationsTag);
        }

        abstract void populateXmlForModifications(Modifications modifications, XmlWriterContext writerContext, Element modificationsTag);
    }

    public static class ScmXmlViewModel extends MaterialXmlViewModel {

        ScmXmlViewModel(Material material) {
            super(material);
        }

        @Override
        void populateXmlForModifications(Modifications modifications, XmlWriterContext writerContext, Element modificationsTag) {
            for (Modification modification : modifications) {
                Element changeset = modificationsTag.addElement("changeset");
                changeset.addAttribute("changesetUri", ScmMaterial.changesetUrl(modification, writerContext.getBaseUrl(), material.getId()));
                changeset.addElement("user").addCDATA(modification.getUserDisplayName());
                changeset.addElement("checkinTime").addText(DateUtils.formatISO8601(modification.getModifiedTime()));
                changeset.addElement("revision").addCDATA(modification.getRevision());
                changeset.addElement("message").addCDATA(modification.getComment());
                List<ModifiedFile> modifiedFiles = modification.getModifiedFiles();
                for (ModifiedFile modifiedFile : modifiedFiles) {
                    changeset.addElement("file").addAttribute("name", modifiedFile.getFileName()).addAttribute("action", modifiedFile.getAction().toString());
                }
            }
        }
    }

    static class PackageXmlViewModel extends  MaterialXmlViewModel {
        PackageXmlViewModel(Material material) {
            super(material);
        }

        @Override
        void populateXmlForModifications(Modifications modifications, XmlWriterContext writerContext, Element modificationsTag) {
            for (Modification modification : modifications) {
                Element changeset = modificationsTag.addElement("changeset");
                changeset.addAttribute("changesetUri", ScmMaterial.changesetUrl(modification, writerContext.getBaseUrl(), material.getId()));
                changeset.addElement("user").addCDATA(modification.getUserDisplayName());
                changeset.addElement("checkinTime").addText(DateUtils.formatISO8601(modification.getModifiedTime()));
                changeset.addElement("revision").addCDATA(modification.getRevision());
                changeset.addElement("message").addCDATA(modification.getComment());
            }
        }
    }
    private static class DependencyXmlViewModel extends  MaterialXmlViewModel {

        DependencyXmlViewModel(Material material) {
            super(material);
        }

        @Override
        void populateXmlForModifications(Modifications modifications, XmlWriterContext writerContext, Element modificationsTag) {
            Modification firstModification = modifications.first();
            Element changeset = modificationsTag.addElement("changeset");
            String revision = firstModification.getRevision();
            changeset.addAttribute("changesetUri", StageXmlViewModel.httpUrlFor(writerContext.getBaseUrl(), writerContext.stageIdForLocator(revision)));
            changeset.addElement("checkinTime").addText(DateUtils.formatISO8601(firstModification.getModifiedTime()));
            changeset.addElement("revision").addText(revision);
        }
    }

    private void populateXml(Element materials, MaterialRevision materialRevision, XmlWriterContext writerContext) {
        MaterialXmlViewModel.viewModelFor(materialRevision.getMaterial()).populateXml(materials, materialRevision.getModifications(), writerContext);
    }

    @Override
    public String httpUrl(String baseUrl) {
        return httpUrlForPipeline(baseUrl, pipeline.getId(), pipeline.getName());
    }

    private void addTimelineLink(DOMElement root, String baseUrl, final String rel, final PipelineTimelineEntry entry) {
        root.addElement("link").addAttribute("rel", rel).addAttribute("href", httpUrlForPipeline(baseUrl, entry.getId(), pipeline.getName()));
    }

    public static String httpUrlForPipeline(String baseUrl, long id, final String name) {
        return baseUrl + "/api/pipelines/" + name + "/" + id + ".xml";
    }
}
