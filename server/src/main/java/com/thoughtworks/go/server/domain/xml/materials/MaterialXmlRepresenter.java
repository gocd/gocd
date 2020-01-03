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
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;

public abstract class MaterialXmlRepresenter {
    protected final MaterialRevision materialRevision;

    public MaterialXmlRepresenter(MaterialRevision materialRevision) {
        this.materialRevision = materialRevision;
    }

    public final void populate(ElementBuilder builder, XmlWriterContext ctx) {
        Material material = materialRevision.getMaterial();
        builder.node("material", materialBuilder -> {
            materialBuilder.attr("materialUri", ctx.materialUri(material.getId()));
            material.getAttributesForXml().forEach((key, value) -> materialBuilder.attr(key, value.toString()));

            materialBuilder.node("modifications", modificationBuilder -> {
                materialRevision.getModifications()
                        .forEach(modification -> populateModification(modificationBuilder, modification, ctx));
            });
        });
    }

    protected abstract void populateModification(ElementBuilder builder, Modification modification, XmlWriterContext ctx);

    public static MaterialXmlRepresenter representerFor(MaterialRevision revision) {
        Material material = revision.getMaterial();
        if (material instanceof ScmMaterial || material instanceof PluggableSCMMaterial) {
            return new ScmMaterialXmlRepresenter(revision);
        }

        if (material instanceof DependencyMaterial) {
            return new DependencyMaterialXmlRepresenter(revision);
        }

        if (material instanceof PackageMaterial) {
            return new PackageMaterialXmlRepresenter(revision);
        }

        throw new RuntimeException("Unknown material type");
    }

}
