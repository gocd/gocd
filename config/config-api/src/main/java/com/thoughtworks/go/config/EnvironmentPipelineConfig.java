/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.util.List;

import com.thoughtworks.go.domain.ConfigErrors;

/**
 * @understands a reference to an existing pipeline that is associated to an Environment
 */
@ConfigTag("pipeline")
public class EnvironmentPipelineConfig implements Validatable {
    public static final String ORIGIN = "origin";

    @ConfigAttribute(value = "name", optional = false)
    private CaseInsensitiveString name;
    private ConfigErrors configErrors = new ConfigErrors();

    public EnvironmentPipelineConfig() {
    }

    public EnvironmentPipelineConfig(CaseInsensitiveString pipelineName) {
        this.name = pipelineName;
    }

    public EnvironmentPipelineConfig(String pipelineName) {
        this.name = new CaseInsensitiveString(pipelineName);
    }

    public CaseInsensitiveString getName() {
        return name;
    }

    public void setName(String pipelineName) {
        setName(new CaseInsensitiveString(pipelineName));
    }

    public void setName(CaseInsensitiveString pipelineName) {
        this.name = pipelineName;
    }

    public boolean isReferenceOf(List<CaseInsensitiveString> pipelineNames) {
        boolean matched = false;
        for (CaseInsensitiveString name : pipelineNames) {
            matched = sameAs(name) || matched;
        }
        return matched;
    }

    private boolean sameAs(final CaseInsensitiveString pipelineName) {
        return this.name.equals(pipelineName);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvironmentPipelineConfig that = (EnvironmentPipelineConfig) o;
        return name == null ? that.name == null : sameAs(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }
}
