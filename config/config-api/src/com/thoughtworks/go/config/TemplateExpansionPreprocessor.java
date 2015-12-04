/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

/**
 * @understands de-referencing template body into pipeline
 */
public class TemplateExpansionPreprocessor implements GoConfigPreprocessor {
    public void process(CruiseConfig cruiseConfig) {
        for (PipelineConfig pipelineConfig : cruiseConfig.getAllPipelineConfigs()) {
            if (pipelineConfig.hasTemplate()) {
                CaseInsensitiveString templateName = pipelineConfig.getTemplateName();
                PipelineTemplateConfig pipelineTemplate = cruiseConfig.findTemplate(templateName);
                pipelineConfig.validateTemplate(pipelineTemplate);
                if (pipelineConfig.errors().isEmpty() && !pipelineConfig.hasTemplateApplied()) {
                    pipelineConfig.usingTemplate(pipelineTemplate);
                }
            }
        }
    }
}
