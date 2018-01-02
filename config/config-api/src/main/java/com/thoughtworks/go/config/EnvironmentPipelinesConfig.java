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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

/**
 * @understands references to existing pipelines that are associated to an Environment
 */
@ConfigTag("pipelines")
@ConfigCollection(EnvironmentPipelineConfig.class)
public class EnvironmentPipelinesConfig extends BaseCollection<EnvironmentPipelineConfig> implements ParamsAttributeAware, Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public EnvironmentPipelinesConfig() {
    }

    //used only in tests
    public EnvironmentPipelinesConfig(CaseInsensitiveString... pipelineNames) {
        for (CaseInsensitiveString name : pipelineNames) {
            add(new EnvironmentPipelineConfig(name));
        }
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public ArrayList<CaseInsensitiveString> getNames() {
        ArrayList<CaseInsensitiveString> names = new ArrayList<>();
        for (EnvironmentPipelineConfig environmentPipelineConfig : this) {
            names.add(environmentPipelineConfig.getName());
        }
        return names;
    }

    public boolean containsPipelineNamed(final CaseInsensitiveString pipelineName) {
        return contains(new EnvironmentPipelineConfig(pipelineName));
    }

    public void validateContainsOnlyPipelines(CaseInsensitiveString name, List<CaseInsensitiveString> pipelineNames) {
        for (EnvironmentPipelineConfig pipelineConfig : this) {
            if (!pipelineConfig.isReferenceOf(pipelineNames)) {
                throw new RuntimeException(String.format("Environment '%s' refers to an unknown pipeline '%s'.", name, pipelineConfig.getName()));
            }
        }
    }

    public boolean add(EnvironmentPipelineConfig environmentPipelineConfig) {
        validateNotDuplicate(environmentPipelineConfig);
        return super.add(environmentPipelineConfig);
    }

    private void validateNotDuplicate(EnvironmentPipelineConfig environmentPipelineConfig) {
        if (contains(environmentPipelineConfig)) {
            throw new RuntimeException(String.format("Cannot add pipeline '%s' to the environment", environmentPipelineConfig.getName()));
        }
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes != null) {
            List<Map> pipelineAttributes = (List) attributes;
            this.clear();
            for (Map attributeMap : pipelineAttributes) {
                this.add(new EnvironmentPipelineConfig(new CaseInsensitiveString((String) attributeMap.get("name"))));
            }
        }
    }
}
