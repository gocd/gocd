/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.StageConfig;

public class PipelineTemplateConfigMother {
    public static PipelineTemplateConfig createTemplate(String templateName) {
        return createTemplate(templateName, StageConfigMother.custom("defaultStage", "defaultJob"));
    }

    public static PipelineTemplateConfig createTemplate(String templateName, Authorization authorization, StageConfig... stageConfigs) {
        return new PipelineTemplateConfig(new CaseInsensitiveString(templateName), authorization, stageConfigs);
    }

    public static PipelineTemplateConfig createTemplate(String templateName, StageConfig... stageConfigs) {
        return createTemplate(templateName, new Authorization(), stageConfigs);
    }

    public static PipelineTemplateConfig createTemplateWithParams(String templateName, String... paramNameAndValue) {
        PipelineTemplateConfig template = createTemplate(templateName);
        for (String nameAndValue : paramNameAndValue) {
            template.get(0).getJobs().get(0).addVariable(String.format("name-%s", nameAndValue), String.format("value-#{%s}", nameAndValue));
        }
        return template;
    }
}
