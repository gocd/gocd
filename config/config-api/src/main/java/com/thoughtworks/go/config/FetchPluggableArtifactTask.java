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

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@AttributeAwareConfigTag(value = "fetchartifact", attribute = "origin", attributeValue = "external")
public class FetchPluggableArtifactTask extends AbstractFetchTask {
    public static final String FETCH_PLUGGABLE_ARTIFACT_DISPLAY_NAME = "Fetch Pluggable Artifact";
    public static final String ARTIFACT_ID = "artifactId";

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

    public Map<String, Map<String, String>> getConfigAsMap() {
        Map<String, Map<String, String>> configMap = new HashMap<>();
        for (ConfigurationProperty property : getConfiguration()) {
            Map<String, String> mapValue = new HashMap<>();
            mapValue.put("value", property.getValue());
            if (!property.errors().isEmpty()) {
                mapValue.put("errors", StringUtils.join(property.errors().getAll(), ", "));
            }
            configMap.put(property.getConfigKeyName(), mapValue);
        }
        return configMap;
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
            final PluggableArtifactConfig pluggableArtifactConfig = jobConfig.artifactConfigs().findByArtifactId(artifactId);

            if (pluggableArtifactConfig == null) {
                addError("artifactId", format("Pluggable artifact with id `%s` does not exist in [%s/%s/%s].", artifactId, pipelineNamePathFromAncestor.getAncestorName(), getStage(), getJob()));
            }
        }

        configuration.validateTree();
        configuration.validateUniqueness("Fetch pluggable artifact");
    }

    public void encryptSecureProperties(CruiseConfig preprocessedCruiseConfig, PipelineConfig preprocessedPipelineConfig, FetchPluggableArtifactTask preprocessedTask) {
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
            } catch (PipelineNotFoundException e) {
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
                    externalArtifact = jobConfig.artifactConfigs().findByArtifactId(preprocessedTask.getArtifactId());
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
        String pluginId = (String) attributeMap.get("pluginId");
        if (StringUtils.isBlank(pluginId)) {
            return;
        }

        ArtifactPluginInfo artifactPluginInfo = getArtifactPluginInfo(pluginId);
        if (artifactPluginInfo == null) {
            return;
        }

        for (PluginConfiguration pluginConfiguration : artifactPluginInfo.getFetchArtifactSettings().getConfigurations()) {
            String key = pluginConfiguration.getKey();
            if (attributeMap.containsKey(key)) {
                if (configuration.getProperty(key) == null) {
                    configuration.addNewConfiguration(pluginConfiguration.getKey(), pluginConfiguration.isSecure());
                }
                configuration.getProperty(key).setConfigurationValue(new ConfigurationValue((String) attributeMap.get(key)));
                configuration.getProperty(key).handleSecureValueConfiguration(pluginConfiguration.isSecure());
            }
        }
    }

    public boolean isSecure(String key, PluggableInstanceSettings fetchArtifactSettings) {
        return fetchArtifactSettings.getConfiguration(key) != null && fetchArtifactSettings.getConfiguration(key).isSecure();
    }

    @Override
    public String getTaskType() {
        return "fetch_pluggable_artifact";
    }

    @Override
    public String getTypeForDisplay() {
        return FETCH_PLUGGABLE_ARTIFACT_DISPLAY_NAME;
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

    private ArtifactPluginInfo getArtifactPluginInfo(String pluginId) {
        ArtifactMetadataStore artifactMetadataStore = ArtifactMetadataStore.instance();
        return artifactMetadataStore.getPluginInfo(pluginId);
    }

    public void addConfigurations(List<ConfigurationProperty> configurationProperties) {
        this.getConfiguration().addAll(configurationProperties);
    }
}
