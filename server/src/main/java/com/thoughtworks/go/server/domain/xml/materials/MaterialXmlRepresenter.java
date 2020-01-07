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
package com.thoughtworks.go.server.domain.xml.materials;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;
import org.dom4j.Document;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;

public abstract class MaterialXmlRepresenter implements XmlRepresentable {
    private final String pipelineName;
    private final Integer pipelineCounter;
    protected final MaterialRevision materialRevision;

    public MaterialXmlRepresenter(String pipelineName, Integer pipelineCounter, MaterialRevision materialRevision) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.materialRevision = materialRevision;
    }

    public final void populate(ElementBuilder builder, XmlWriterContext ctx) {
        Material material = materialRevision.getMaterial();
        builder.node("material", materialBuilder -> populateMaterial(ctx, material, materialBuilder));
    }

    public void populateMaterial(XmlWriterContext ctx, Material material, ElementBuilder materialBuilder) {
        materialBuilder.attr("materialUri", ctx.materialUri(pipelineName, pipelineCounter, material.getPipelineUniqueFingerprint()));
        material.getAttributesForXml().forEach((key, value) -> materialBuilder.attr(key, value.toString()));

        materialBuilder.node("modifications", modificationBuilder -> {
            materialRevision.getModifications()
                .forEach(modification -> populateModification(modificationBuilder, modification, ctx));
        });
    }

    protected abstract void populateModification(ElementBuilder builder, Modification modification, XmlWriterContext ctx);

    public static MaterialXmlRepresenter representerFor(String pipelineName, Integer pipelineCounter, MaterialRevision revision) {
        Material material = revision.getMaterial();
        if (material instanceof ScmMaterial || material instanceof PluggableSCMMaterial) {
            return new ScmMaterialXmlRepresenter(pipelineName, pipelineCounter, revision);
        }

        if (material instanceof DependencyMaterial) {
            return new DependencyMaterialXmlRepresenter(pipelineName, pipelineCounter, revision);
        }

        if (material instanceof PackageMaterial) {
            return new PackageMaterialXmlRepresenter(pipelineName, pipelineCounter, revision);
        }

        throw new RuntimeException("Unknown material type");
    }

    @Override
    public Document toXml(XmlWriterContext ctx) {
        DOMElement rootElement = new DOMElement("material");
        ElementBuilder builder = new ElementBuilder(rootElement);
        populateMaterial(ctx, materialRevision.getMaterial(), builder);
        return new DOMDocument(rootElement);
    }

    @Override
    public String httpUrl(String baseUrl) {
        return null;
    }
}
