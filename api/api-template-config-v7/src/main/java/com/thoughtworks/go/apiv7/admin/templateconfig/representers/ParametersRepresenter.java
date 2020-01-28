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

package com.thoughtworks.go.apiv7.admin.templateconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.ParamsConfig;
import com.thoughtworks.go.spark.Routes;

public class ParametersRepresenter {
    public static void toJSON(OutputWriter outputWriter, String templateName, ParamsConfig paramConfigs) {
        outputWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.PipelineTemplateConfig.name(templateName))
                .addAbsoluteLink("doc", Routes.PipelineTemplateConfig.DOC)
                .addLink("find", Routes.PipelineTemplateConfig.find()));
        outputWriter.add("name", templateName)
                .addChildList("parameters", paramConfigs.getNames());
    }
}
