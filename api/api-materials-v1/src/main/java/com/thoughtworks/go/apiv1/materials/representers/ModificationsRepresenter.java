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
package com.thoughtworks.go.apiv1.materials.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.PaginationRepresenter;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.spark.Routes;

import java.util.function.Consumer;

public class ModificationsRepresenter {

    public static void toJSON(OutputWriter outputWriter, Modifications modifications, Pagination pagination, String fingerprint) {
        outputWriter.addLinks(links(fingerprint))
                .addEmbedded(embedded(modifications, pagination));
    }

    private static Consumer<OutputWriter> embedded(Modifications modifications, Pagination pagination) {
        return outputWriter -> outputWriter
                .addChildList("modifications", modifications(modifications))
                .addChild("pagination", PaginationRepresenter.toJSON(pagination));
    }

    private static Consumer<OutputListWriter> modifications(Modifications modifications) {
        if (modifications == null) {
            return outputListWriter -> {
            };
        }
        return modificationsWriter -> modifications.forEach(modification ->
                modificationsWriter.addChild(ModificationRepresenter.toJSON(modification)));
    }

    private static Consumer<OutputLinkWriter> links(String fingerprint) {
        return outputLinkWriter -> outputLinkWriter.addLink("self", Routes.MaterialModifications.modification(fingerprint))
                .addAbsoluteLink("doc", Routes.MaterialConfig.DOC)
                .addLink("find", Routes.MaterialModifications.FIND);
    }
}
