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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.PipelineTemplateConfig;

public class TemplatesViewModel {
    private PipelineTemplateConfig templateConfig;
    private final boolean authorizedToViewTemplate;
    private final boolean authorizedToEditTemplate;

    public TemplatesViewModel(PipelineTemplateConfig templateConfig, boolean authorizedToViewTemplate, boolean authorizedToEditTemplate) {
        this.templateConfig = templateConfig;
        this.authorizedToViewTemplate = authorizedToViewTemplate;
        this.authorizedToEditTemplate = authorizedToEditTemplate;
    }

    public PipelineTemplateConfig getTemplate() {
        return templateConfig;
    }

    public boolean isAuthorizedToViewTemplate() {
        return authorizedToViewTemplate;
    }

    public boolean isAuthorizedToEditTemplate() {
        return authorizedToEditTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o.getClass() != this.getClass()) {
            return false;
        }
        TemplatesViewModel that = (TemplatesViewModel) o;
        if (templateConfig != null ? !templateConfig.equals(that.templateConfig) : that.templateConfig != null) {
            return false;
        }
        if (authorizedToViewTemplate != that.authorizedToViewTemplate) {
            return false;
        }
        if (authorizedToEditTemplate != that.authorizedToEditTemplate) {
            return false;
        }
        return true;
    }
}
