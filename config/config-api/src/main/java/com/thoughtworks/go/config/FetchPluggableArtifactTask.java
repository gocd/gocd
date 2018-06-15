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
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@AttributeAwareConfigTag(value = "fetchartifact", attribute = "origin", attributeValue = "external")
public class FetchPluggableArtifactTask extends AbstractFetchTask {
    public static final String FETCH_EXTERNAL_ARTIFACT = "Fetch External Artifact";
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

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setConfiguration(Configuration configuration) {
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

    public void encryptSecureProperties(CruiseConfig cruiseConfig, PipelineConfig pipelineConfig) {
        if (artifactId != null) {
            PluggableArtifactConfig externalArtifact = getSpecificExternalArtifact(cruiseConfig, pipelineConfig);

            if (externalArtifact != null && externalArtifact.getArtifactStore() != null) {
                ArtifactPluginInfo pluginInfo = ArtifactMetadataStore.instance().getPluginInfo(externalArtifact.getArtifactStore().getPluginId());
                if (pluginInfo == null || pluginInfo.getFetchArtifactSettings() == null) {
                    return;
                }
                for (ConfigurationProperty configurationProperty : getConfiguration()) {
                    configurationProperty.handleSecureValueConfiguration(isSecure(configurationProperty.getConfigKeyName(), pluginInfo.getFetchArtifactSettings()));
                }
            }
        }
    }

    public PluggableArtifactConfig getSpecificExternalArtifact(CruiseConfig cruiseConfig, PipelineConfig pipelineConfig) {
        PipelineConfig dependencyMaterial = null;
        PluggableArtifactConfig externalArtifact = null;
        if (pipelineName == null || CaseInsensitiveString.isBlank(pipelineName.getPath()) || pipelineName.getPath().equals(pipelineConfig.name())) {
            dependencyMaterial = pipelineConfig;
        } else {
            try {
                dependencyMaterial = cruiseConfig.pipelineConfigByName(pipelineName.getAncestorName());
            } catch (PipelineNotFoundException e) {
                // ignore
            }
        }
        if (dependencyMaterial != null) {
            StageConfig upstreamStage = dependencyMaterial.getStage(this.stage);
            if (upstreamStage != null) {
                JobConfig jobConfig = upstreamStage.jobConfigByConfigName(this.job);
                if (jobConfig != null) {
                    externalArtifact = jobConfig.artifactConfigs().findByArtifactId(this.artifactId);
                }
            }
        }
        return externalArtifact;
    }


    @Override
    protected void setFetchTaskAttributes(Map attributeMap) {
        // since encryptSecureProperties will be called after deserialization, this need not be updated.

        configuration.setConfigAttributes(attributeMap, new SecureKeyInfoProvider() {
            @Override
            public boolean isSecure(String key) {
                return false;
            }
        });
    }

    public boolean isSecure(String key, PluggableInstanceSettings fetchArtifactSettings) {
        return fetchArtifactSettings.getConfiguration(key) != null && fetchArtifactSettings.getConfiguration(key).isSecure();
    }

    @Override
    public String getTypeForDisplay() {
        return FETCH_EXTERNAL_ARTIFACT;
    }

    @Override
    protected File destOnAgent(String pipelineName) {
        return new File(format("pipelines/%s", pipelineName));
    }

    @Override
    public String getOrigin() {
        return "external";
    }

    @Override
    public String describe() {
        return String.format("fetch pluggable artifact using [%s] from [%s/%s/%s]", getArtifactId(), getPipelineName(), getStage(), getJob());
    }

    public void addConfigurations(List<ConfigurationProperty> configurationProperties) {
        // encrypting of properties will be taken care of later (see encryptSecureProperties) when we have information about which properties to encrypt
        // For now, we can set the properties as is.
        this.getConfiguration().addAll(configurationProperties);
    }
}
