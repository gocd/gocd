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

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;
import com.thoughtworks.go.util.DateUtils;

public class PackageMaterialXmlRepresenter extends MaterialXmlRepresenter {
    public PackageMaterialXmlRepresenter(MaterialRevision materialRevision) {
        super(materialRevision);
    }

    @Override
    protected void populateModification(ElementBuilder builder, Modification modification, XmlWriterContext ctx) {
        builder.node("changeset", cb -> cb
                .attr("changesetUri", ctx.changesetUri(modification, materialRevision.getMaterial().getId()))
                .cdataNode("user", modification.getUserDisplayName())
                .cdataNode("revision", modification.getRevision())
                .cdataNode("message", modification.getComment())
                .textNode("checkinTime", DateUtils.formatISO8601(modification.getModifiedTime()))
        );
    }
}
