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

import java.util.*;

/**
 * @understands a custom data structure for templates to editable pipelines.
 */

public class TemplateToPipelines {
    private  List<PipelineEditabilityInfo> pipelines = new ArrayList<>();
    private CaseInsensitiveString templateName;
    private boolean canEdit;
    private boolean isAdmin;

    public TemplateToPipelines(CaseInsensitiveString templateName, boolean canEdit, boolean isAdmin) {
        this.templateName = templateName;
        this.canEdit = canEdit;
        this.isAdmin = isAdmin;
    }

    public void add(PipelineEditabilityInfo pipelineEditabilityInfo) {
        pipelines.add(pipelineEditabilityInfo);
    }

    public List<PipelineEditabilityInfo> getPipelines() {
        return pipelines;
    }

    public CaseInsensitiveString getTemplateName() {
        return templateName;
    }

    public boolean canEditTemplate() {
        return canEdit;
    }

    public boolean isAdminUser() {
        return isAdmin;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemplateToPipelines that = (TemplateToPipelines) o;

        if (canEdit != that.canEdit) return false;
        if (isAdmin != that.isAdmin) return false;
        if (!pipelines.equals(that.pipelines)) return false;
        return templateName.equals(that.templateName);
    }

    @Override
    public int hashCode() {
        int result = pipelines.hashCode();
        result = 31 * result + templateName.hashCode();
        result = 31 * result + (canEdit ? 1 : 0);
        result = 31 * result + (isAdmin ? 1 : 0);
        return result;
    }
}
