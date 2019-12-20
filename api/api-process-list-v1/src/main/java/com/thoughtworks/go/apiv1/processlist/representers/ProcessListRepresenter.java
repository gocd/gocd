/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.processlist.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.ProcessWrapper;

import java.util.Collection;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class ProcessListRepresenter {
    public static void toJSON(OutputWriter writer, Collection<ProcessWrapper> processList) {
        writer.addLinks(outputLinkWriter -> {
            outputLinkWriter.addLink("self", Routes.ProcessListAPI.BASE);
            outputLinkWriter.addAbsoluteLink("doc", apiDocsUrl("#process-list"));
        });

        writer.addEmbedded(embeddedWriter -> {
            embeddedWriter.addChildList("process-list", listWriter -> {
                processList.forEach(process -> listWriter.addChild(childWriter -> ProcessRepresenter.toJSON(childWriter, process)));
            });
        });
    }
}
