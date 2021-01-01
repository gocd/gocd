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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@AttributeAwareConfigTag(value = "fetchartifact", attribute = "artifactOrigin", attributeValue = "external")
public class FetchPluggableArtifactTask extends AbstractFetchTask {
    public static final String ARTIFACT_ID = "artifactId";
    public static final String CONFIGURATION = "configuration";
    public static final String FETCH_EXTERNAL_ARTIFACT = "Fetch External Artifact";

    @ConfigAttribute(value = "artifactId", optional = false)
    private String artifactId;
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    public FetchPluggableArtifactTask() {
    }

    public FetchPluggableArtifactTask(CaseInsensitiveString pipelineName,
                                      CaseInsensitiveString stage,
                                      CaseInsensitiveString job,
                                      String artifactId,
                                      ConfigurationProperty... configurations) {
        super(pipelineName, stage, job);
        this.artifactId = artifactId;
        configuration.addAll(Arrays.asList(configurations));
    }

    public FetchPluggableArtifactTask(CaseInsensitiveString stage,
                                      CaseInsensitiveString job,
                                      String artifactId,
                                      ConfigurationProperty... configurations) {
        super(stage, job);
        this.artifactId = artifactId;
        configuration.addAll(Arrays.asList(configurations));
    }

    public FetchPluggableArtifactTask(CaseInsensitiveString pipelineName,
                                      CaseInsensitiveString stage,
                                      CaseInsensitiveString job,
                                      String artifactId,
                                      Configuration configuration) {
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

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    @Override
    protected void validateAttributes(ValidationContext validationContext) {
        if (StringUtils.isBlank(artifactId)) {
            errors.add("artifactId", "Artifact Id cannot be blank.");
        }

        if (isNotBlank(artifactId) && validationContext.isWithinPipelines()) {
            final PathFromAncestor pipelineNamePathFromAncestor = getPipelineNamePathFromAncestor();
            final PipelineConfig pipelineConfig = validationContext.getPipelineConfigByName(pipelineNamePathFromAncestor.getAncestorName());
            final JobConfig jobConfig = pipelineConfig.getStage(getStage()).jobConfigByConfigName(getJob());
            final PluggableArtifactConfig pluggableArtifactConfig = jobConfig.artifactTypeConfigs().findByArtifactId(artifactId);

            if (pluggableArtifactConfig == null) {
                addError("artifactId", format("Pluggable artifact with id `%s` does not exist in [%s/%s/%s].", artifactId, pipelineNamePathFromAncestor.getAncestorName(), getStage(), getJob()));
            }
        }

        configuration.validateTree();
        configuration.validateUniqueness("Fetch pluggable artifact");
    }

    public void encryptSecureProperties(CruiseConfig preprocessedCruiseConfig,
                                        PipelineConfig preprocessedPipelineConfig,
                                        FetchPluggableArtifactTask preprocessedTask) {
        if (isNotBlank(artifactId)) {
            PluggableArtifactConfig externalArtifact = getSpecifiedExternalArtifact(preprocessedCruiseConfig, preprocessedPipelineConfig, preprocessedTask, true, preprocessedPipelineConfig.name());
            encryptSecurePluginConfiguration(preprocessedCruiseConfig, externalArtifact);
        }
    }

    public void encryptSecureProperties(CruiseConfig preprocessedCruiseConfig, PipelineTemplateConfig pipelineTemplateConfig) {
        if (isNotBlank(artifactId)) {
            PluggableArtifactConfig externalArtifact = getSpecifiedExternalArtifact(preprocessedCruiseConfig, pipelineTemplateConfig, this, false, pipelineTemplateConfig.name());
            encryptSecurePluginConfiguration(preprocessedCruiseConfig, externalArtifact);
        }
    }

    private void encryptSecurePluginConfiguration(CruiseConfig preprocessedCruiseConfig, PluggableArtifactConfig externalArtifact) {
        if (externalArtifact != null) {
            ArtifactStore artifactStore = preprocessedCruiseConfig.getArtifactStores().find(externalArtifact.getStoreId());
            if (artifactStore != null) {
                ArtifactPluginInfo pluginInfo = ArtifactMetadataStore.instance().getPluginInfo(artifactStore.getPluginId());
                if (pluginInfo == null || pluginInfo.getFetchArtifactSettings() == null) {
                    return;
                }
                for (ConfigurationProperty configurationProperty : getConfiguration()) {
                    configurationProperty.handleSecureValueConfiguration(isSecure(configurationProperty.getConfigKeyName(), pluginInfo.getFetchArtifactSettings()));
                }

            }
        }
    }

    public PluggableArtifactConfig getSpecifiedExternalArtifact(CruiseConfig cruiseConfig, BaseCollection<StageConfig> pipelineOrTemplate, FetchPluggableArtifactTask preprocessedTask, boolean isPipeline, CaseInsensitiveString pipelineOrTemplateName) {
        BaseCollection<StageConfig> dependencyMaterial = null;
        PluggableArtifactConfig externalArtifact = null;
        boolean isUpstreamAPipeline = isPipeline;
        if (preprocessedTask.getPipelineName() == null || CaseInsensitiveString.isBlank(preprocessedTask.getTargetPipelineName()) || preprocessedTask.getTargetPipelineName().equals(pipelineOrTemplateName)) {
            dependencyMaterial = pipelineOrTemplate;
        } else {
            try {
                dependencyMaterial = cruiseConfig.pipelineConfigByName(preprocessedTask.getTargetPipelineName());
                isUpstreamAPipeline = true;
            } catch (RecordNotFoundException e) {
                // ignore
            }
        }
        StageConfig upstreamStage;
        if (dependencyMaterial != null && !dependencyMaterial.isEmpty()) {
            if (isUpstreamAPipeline) {
                upstreamStage = ((PipelineConfig) dependencyMaterial).getStage(preprocessedTask.getStage());
            } else {
                upstreamStage = ((PipelineTemplateConfig) dependencyMaterial).getStage(preprocessedTask.getStage());
            }
            if (upstreamStage != null) {
                JobConfig jobConfig = upstreamStage.jobConfigByConfigName(preprocessedTask.getJob());
                if (jobConfig != null) {
                    externalArtifact = jobConfig.artifactTypeConfigs().findByArtifactId(preprocessedTask.getArtifactId());
                }
            }
        }
        return externalArtifact;
    }

    @Override
    protected void setFetchTaskAttributes(Map attributeMap) {
        this.artifactId = (String) attributeMap.get(ARTIFACT_ID);
        if (StringUtils.isBlank(this.artifactId)) {
            return;
        }

        final Map<String, Object> userSpecifiedConfiguration = (Map<String, Object>) attributeMap.get(CONFIGURATION);
        if (userSpecifiedConfiguration == null) {
            return;
        }

        final String pluginId = (String) attributeMap.get("pluginId");
        if (StringUtils.isBlank(pluginId)) {
            for (String key : userSpecifiedConfiguration.keySet()) {
                Map<String, String> configurationMetadata = (Map<String, String>) userSpecifiedConfiguration.get(key);
                if (configurationMetadata != null) {
                    boolean isSecure = Boolean.parseBoolean(configurationMetadata.get("isSecure"));
                    if (configuration.getProperty(key) == null) {
                        configuration.addNewConfiguration(key, isSecure);
                    }
                    if (isSecure) {
                        configuration.getProperty(key).setEncryptedValue(new EncryptedConfigurationValue(configurationMetadata.get("value")));
                    } else {
                        configuration.getProperty(key).setConfigurationValue(new ConfigurationValue(configurationMetadata.get("value")));
                    }
                }
            }

        } else {
            for (Map.Entry<String, Object> configuration : userSpecifiedConfiguration.entrySet()) {
                this.configuration.addNewConfigurationWithValue(configuration.getKey(), String.valueOf(configuration.getValue()), false);
            }
        }
    }

    public boolean isSecure(String key, PluggableInstanceSettings fetchArtifactSettings) {
        return fetchArtifactSettings.getConfiguration(key) != null && fetchArtifactSettings.getConfiguration(key).isSecure();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FetchPluggableArtifactTask that = (FetchPluggableArtifactTask) o;
        return Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (null != artifactId ? artifactId.hashCode() : 0);
        result = 31 * result + (null != configuration ? configuration.hashCode() : 0);
        return result;
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
    public String getArtifactOrigin() {
        return "external";
    }

    @Override
    public String describe() {
        return String.format("fetch pluggable artifact using [%s] from [%s/%s/%s]", getArtifactId(), getPipelineName(), getStage(), getJob());
    }

    public void addConfigurations(List<ConfigurationProperty> configurationProperties) {
        this.getConfiguration().addAll(configurationProperties);
    }
}
