/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

/**
 * @understands de-referencing template body into pipeline
 */
public class TemplateExpansionPreprocessor implements GoConfigPreprocessor {
    public void process(CruiseConfig cruiseConfig) {
         for (PipelineConfig pipelineConfig : cruiseConfig.getAllPipelineConfigs()) {
            if (pipelineConfig.hasTemplate()) {
                if (!pipelineConfig.hasTemplateApplied() && !pipelineConfig.isEmpty()) {
                    throw new RuntimeException(String.format("Pipeline '%s' must not reference a template and define stages.", pipelineConfig.name()));
                }
                CaseInsensitiveString templateName = pipelineConfig.getTemplateName();
                PipelineTemplateConfig pipelineTemplate = cruiseConfig.findTemplate(templateName);
                bombIfNull(pipelineTemplate, String.format("Pipeline '%s' refers to non-existent template '%s'.", pipelineConfig.name(), templateName));
                if (!pipelineConfig.hasTemplateApplied()) {
                    pipelineConfig.usingTemplate(pipelineTemplate);
                }
            } else {
                if (pipelineConfig.isEmpty()) {
                    throw new RuntimeException("Pipeline '" + pipelineConfig.name() + "' does not have any stages configured. A pipeline must have at least one stage.");
                }
            }
        }
    }
}
