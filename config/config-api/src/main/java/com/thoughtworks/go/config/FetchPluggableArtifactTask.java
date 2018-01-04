/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.SecureKeyInfoProvider;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@ConfigTag(value = "fetchPluggableArtifact")
public class FetchPluggableArtifactTask extends AbstractFetchTask implements SecureKeyInfoProvider {
    public static final String FETCH_PLUGGABLE_ARTIFACT = "Fetch Pluggable Artifact";
    @ConfigAttribute(value = "artifactId", optional = false)
    private String artifactId;
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    public FetchPluggableArtifactTask() {
    }

    public FetchPluggableArtifactTask(CaseInsensitiveString pipelineName, CaseInsensitiveString stage, CaseInsensitiveString job, String artifactId, ConfigurationProperty... configurations) {
        super(pipelineName, stage, job);
        this.artifactId = artifactId;
        configuration.addAll(Arrays.asList(configurations));
    }

    public FetchPluggableArtifactTask(CaseInsensitiveString stage, CaseInsensitiveString job, String artifactId, ConfigurationProperty... configurations) {
        super(stage, job);
        this.artifactId = artifactId;
        configuration.addAll(Arrays.asList(configurations));
    }

    public FetchPluggableArtifactTask(CaseInsensitiveString pipelineName, CaseInsensitiveString stage, CaseInsensitiveString job, String artifactId, Configuration configuration) {
        super(pipelineName, stage, job);
        this.artifactId = artifactId;
        this.configuration = configuration;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    protected void validateAttributes(ValidationContext validationContext) {
        if (!new NameTypeValidator().isNameValid(artifactId)) {
            errors.add("artifactId", NameTypeValidator.errorMessage("fetch artifact artifactId", artifactId));
        }

        if (isNotBlank(artifactId) && validationContext.isWithinPipelines()) {
            final PathFromAncestor pipelineNamePathFromAncestor = getPipelineNamePathFromAncestor();
            final PipelineConfig pipelineConfig = validationContext.getPipelineConfigByName(pipelineNamePathFromAncestor.getAncestorName());
            final JobConfig jobConfig = pipelineConfig.getStage(getStage()).jobConfigByConfigName(getJob());
            final PluggableArtifactConfig pluggableArtifactConfig = jobConfig.artifactConfigs().findByArtifactId(artifactId);

            if (pluggableArtifactConfig == null) {
                addError("artifactId", format("Pluggable artifact with id `%s` does not exist in [%s/%s/%s].", artifactId, pipelineNamePathFromAncestor.getAncestorName(), getStage(), getJob()));
            }
        }

        configuration.validateTree();
        configuration.validateUniqueness("Fetch pluggable artifact");
    }

    @Override
    protected void setFetchTaskAttributes(Map attributeMap) {
        configuration.setConfigAttributes(attributeMap, this);
    }

    @Override
    public boolean isSecure(String key) {
        return false;
    }

    @Override
    public String getTaskType() {
        return "fetch_pluggable_artifact";
    }

    @Override
    public String getTypeForDisplay() {
        return FETCH_PLUGGABLE_ARTIFACT;
    }

    @Override
    protected File destOnAgent(String pipelineName) {
        return new File(format("pipelines/%s", pipelineName));
    }

    @Override
    public String describe() {
        return String.format("fetch pluggable artifact using [%s] from [%s/%s/%s]", getArtifactId(), getPipelineName(), getStage(), getJob());
    }
}
