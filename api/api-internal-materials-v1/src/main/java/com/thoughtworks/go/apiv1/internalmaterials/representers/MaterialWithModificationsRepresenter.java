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

package com.thoughtworks.go.apiv1.internalmaterials.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv1.internalmaterials.representers.materials.MaterialsRepresenter;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class MaterialWithModificationsRepresenter {
    public static void toJSON(OutputWriter outputWriter, Map<MaterialConfig, Modification> modificationsMap) {
        outputWriter.addLinks(links());
        if (modificationsMap.isEmpty()) {
            outputWriter.addChildList("materials", Collections.emptyList());
            return;
        }
        outputWriter.addChildList("materials", materialWriter -> {
            modificationsMap.forEach((config, mod) -> materialWriter.addChild(childWriter -> {
                childWriter.addChild("config", MaterialsRepresenter.toJSON(config));
                if (mod == null) {
                    childWriter.renderNull("modification");
                } else {
                    childWriter.addChild("modification", modWriter -> modificationWriter(modWriter, mod));
                }
            }));
        });
    }

    private static void modificationWriter(OutputWriter writer, Modification mod) {
        writer.addIfNotNull("username", mod.getUserName())
                .addIfNotNull("email_address", mod.getEmailAddress())
                .addIfNotNull("revision", mod.getRevision())
                .addIfNotNull("modified_time", mod.getModifiedTime())
                .addIfNotNull("comment", mod.getComment());
    }

    private static Consumer<OutputLinkWriter> links() {
        return outputLinkWriter -> outputLinkWriter.addLink("self", Routes.MaterialConfig.INTERNAL_BASE)
                .addAbsoluteLink("doc", Routes.MaterialConfig.DOC);
    }
}
