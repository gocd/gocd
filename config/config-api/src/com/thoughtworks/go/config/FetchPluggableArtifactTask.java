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

import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.Configuration;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@ConfigTag(value = "fetchPluggableArtifact")
public class FetchPluggableArtifactTask extends AbstractTask implements Serializable {
    public static final String PIPELINE_NAME = "pipelineName";
    public static final String PIPELINE = "pipeline";
    public static final String STAGE = "stage";
    public static final String JOB = "job";

    @ConfigAttribute(value = "pipeline", allowNull = true)
    private PathFromAncestor pipelineName;
    @ConfigAttribute(value = "stage")
    private CaseInsensitiveString stage;
    @ConfigAttribute(value = "job")
    private CaseInsensitiveString job;
    @ConfigAttribute(value = "storeId", optional = false)
    private String storeId;
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    @Override
    protected void setTaskConfigAttributes(Map attributes) {

    }

    @Override
    protected void validateTask(ValidationContext validationContext) {

    }

    @Override
    public String getTaskType() {
        return null;
    }

    @Override
    public String getTypeForDisplay() {
        return null;
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        return null;
    }

    public PathFromAncestor getPipelineName() {
        return pipelineName;
    }

    public CaseInsensitiveString getStage() {
        return stage;
    }

    public CaseInsensitiveString getJob() {
        return job;
    }

    public String getStoreId() {
        return storeId;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
