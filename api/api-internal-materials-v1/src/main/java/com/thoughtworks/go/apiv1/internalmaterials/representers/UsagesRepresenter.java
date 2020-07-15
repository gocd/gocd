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
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.spark.Routes;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;

public class UsagesRepresenter {
    public static void toJSON(OutputWriter outputWriter, String fingerprint, Map<String, List<String>> usages) {
        outputWriter.addLinks(links(fingerprint));
        if (usages.isEmpty()) {
            outputWriter.addChildList("usages", emptyList());
            return;
        }
        outputWriter.addChildList("usages", usagesWriter -> usages.forEach((grp, pipelines) -> addGrpUsages(usagesWriter, grp, pipelines)));
    }

    private static void addGrpUsages(OutputListWriter usagesWriter, String group, List<String> pipelines) {
        usagesWriter.addChild(childWriter -> childWriter.add("group", group)
                .addChildList("pipeline", pipelines));
    }

    private static Consumer<OutputLinkWriter> links(String fingerprint) {
        return outputLinkWriter -> outputLinkWriter.addLink("self", Routes.MaterialConfig.usages(fingerprint))
                .addAbsoluteLink("doc", Routes.MaterialConfig.DOC);
    }
}
