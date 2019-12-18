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
package com.thoughtworks.go.apiv6.admin.templateconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.TemplateToPipelines;
import com.thoughtworks.go.spark.Routes;

import java.util.List;

public class TemplatesConfigRepresenter {

    public static void toJSON(OutputWriter jsonWriter, List<TemplateToPipelines> templatesList) {
        jsonWriter.addLinks(
                outputLinkWriter -> outputLinkWriter.addLink("self", Routes.PipelineTemplateConfig.BASE)
                        .addAbsoluteLink("doc", Routes.PipelineTemplateConfig.DOC))

                .addChild("_embedded", embeddedWriter -> embeddedWriter.addChildList("templates", templatesWriter -> templatesList.forEach(templateSummary -> templatesWriter.addChild(templateSummaryWriter -> TemplateSummaryRepresenter.toJSON(templateSummaryWriter, templateSummary)))));
    }
}
