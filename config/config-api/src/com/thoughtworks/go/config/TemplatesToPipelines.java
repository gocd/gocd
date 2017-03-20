/*
 * Copyright 2017 ThoughtWorks, Inc.
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
public class TemplatesToPipelines {
    private CaseInsensitiveString templateName;
    private Map<CaseInsensitiveString, Boolean> editablePipelines = new HashMap<>();

    public TemplatesToPipelines(CaseInsensitiveString name) {
        this.templateName = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemplatesToPipelines that = (TemplatesToPipelines) o;

        if (templateName != null ? !templateName.equals(that.templateName) : that.templateName != null) {
            return false;
        }
        return editablePipelines != null ? editablePipelines.equals(that.editablePipelines) : that.editablePipelines == null;
    }

    @Override
    public int hashCode() {
        int result = templateName != null ? templateName.hashCode() : 0;
        result = 31 * result + (editablePipelines != null ? editablePipelines.hashCode() : 0);
        return result;
    }

    public CaseInsensitiveString getTemplateName() {
        return templateName;
    }

    public void addPipeline(CaseInsensitiveString name, Boolean isEditable) {
        editablePipelines.put(name, isEditable);
    }

    public List<CaseInsensitiveString> getPipelines() {
        ArrayList<CaseInsensitiveString> pipelineNames = new ArrayList<>();
        pipelineNames.addAll(editablePipelines.keySet());
        return pipelineNames;
    }

    public Map<CaseInsensitiveString, Boolean> getEditablePipelines() {
        return editablePipelines;
    }
}
