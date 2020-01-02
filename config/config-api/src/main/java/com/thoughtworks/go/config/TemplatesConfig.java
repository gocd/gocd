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
package com.thoughtworks.go.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

/**
 * @understands a collection of configuration abstractions
 */
@ConfigTag("templates")
@ConfigCollection(PipelineTemplateConfig.class)
public class TemplatesConfig extends BaseCollection<PipelineTemplateConfig> implements Validatable {
    public static final String PIPELINE_TEMPLATES_FAKE_GROUP_NAME = "Pipeline Templates";
    private final ConfigErrors configErrors = new ConfigErrors();

    public TemplatesConfig() {
    }

    public TemplatesConfig(PipelineTemplateConfig... templates) {
        Collections.addAll(this, templates);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateNameUniqueness();
    }

    private void validateNameUniqueness() {
        Map<String, PipelineTemplateConfig> templateList = new HashMap<>();
        for (PipelineTemplateConfig pipelineTemplateConfig : this) {
            pipelineTemplateConfig.validateNameUniquness(templateList);
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (that == null) { return false; }
        if (this.getClass() != that.getClass()) { return false; }
        return super.equals(that);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public void removeTemplateNamed(CaseInsensitiveString name) {
        PipelineTemplateConfig toBeRemoved = null;
        for (PipelineTemplateConfig templateConfig : this) {
            if (templateConfig.matches(name)) {
                toBeRemoved = templateConfig;
            }
        }
        this.remove(toBeRemoved);
    }

    public boolean hasTemplateNamed(CaseInsensitiveString name) {
        for (PipelineTemplateConfig templateConfig : this) {
            if (templateConfig.matches(name)) {
                return true;
            }
        }
        return false;
    }

    public PipelineTemplateConfig templateByName(CaseInsensitiveString foo) {
        for (PipelineTemplateConfig templateConfig : this) {
            if (templateConfig.name().equals(foo)) {
                return templateConfig;
            }
        }
        return null;
    }

    public boolean canViewAndEditTemplate(CaseInsensitiveString username, List<Role> roles) {
        for (PipelineTemplateConfig templateConfig : this) {
            if (canUserEditTemplate(templateConfig, username, roles)) {
                return true;
            }
        }
        return false;
    }

    public boolean canUserViewTemplates(CaseInsensitiveString username, List<Role> roles, boolean isGroupAdministrator) {
        for (PipelineTemplateConfig templateConfig : this) {
            if (hasViewAccessToTemplate(templateConfig, username, roles, isGroupAdministrator)) {
                return true;
            }
        }
        return false;
    }

    public boolean canUserEditTemplate(PipelineTemplateConfig template, CaseInsensitiveString username, List<Role> roles) {
        return template.getAuthorization().isUserAnAdmin(username, roles);
    }

    public boolean hasViewAccessToTemplate(PipelineTemplateConfig template, CaseInsensitiveString username, List<Role> roles, boolean isGroupAdministrator) {
        boolean hasViewAccessToTemplate = template.getAuthorization().isViewUser(username, roles);
        hasViewAccessToTemplate = hasViewAccessToTemplate || (template.isAllowGroupAdmins() && isGroupAdministrator);
        return hasViewAccessToTemplate;
    }
}
