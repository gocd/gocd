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

import com.google.gson.Gson;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@AttributeAwareConfigTag(value = "artifact", attribute = "type", attributeValue = "external")
public class PluggableArtifactConfig implements ArtifactConfig {
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "id", allowNull = true)
    protected String id;
    @ConfigAttribute(value = "storeId", allowNull = true)
    private String storeId;
    @ConfigSubtag
    private Configuration configuration = new Configuration();

    @IgnoreTraversal
    @ConfigReferenceElement(referenceAttribute = "storeId", referenceCollection = "artifactStores")
    private ArtifactStore artifactStore;

    public PluggableArtifactConfig() {
    }

    public PluggableArtifactConfig(String id, String storeId, ConfigurationProperty... configurationProperties) {
        this.configuration.addAll(Arrays.asList(configurationProperties));
        this.id = id;
        this.storeId = storeId;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    @Override
    public ArtifactType getArtifactType() {
        return ArtifactType.plugin;
    }

    @Override
    public String getArtifactTypeValue() {
        return "Pluggable Artifact";
    }

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return !hasErrors();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateMandatoryAttributes();
        if (hasErrors()) {
            return;
        }

        configuration.validateUniqueness(getArtifactTypeValue());
        if (!new NameTypeValidator().isNameValid(storeId)) {
            errors.add("storeId", NameTypeValidator.errorMessage("pluggable artifact storeId", storeId));
        }

        if (artifactStore == null) {
            addError("storeId", String.format("Artifact store with id `%s` does not exist.", storeId));
        }


        if (!new NameTypeValidator().isNameValid(id)) {
            errors.add("id", NameTypeValidator.errorMessage("pluggable artifact id", id));
        }
    }

    private void validateMandatoryAttributes() {
        if (StringUtils.isBlank(this.id)) {
            errors.add("id", "\"Id\" is required for PluggableArtifact");
        }

        if (StringUtils.isBlank(this.storeId)) {
            errors.add("storeId", "\"Store id\" is required for PluggableArtifact");
        }
    }

    @Override
    public void validateUniqueness(List<ArtifactConfig> existingArtifactConfigList) {
        for (ArtifactConfig existingArtifactConfig : existingArtifactConfigList) {
            if (existingArtifactConfig instanceof PluggableArtifactConfig) {
                final PluggableArtifactConfig pluggableArtifactConfig = (PluggableArtifactConfig) existingArtifactConfig;

                if (this.getId().equalsIgnoreCase(pluggableArtifactConfig.getId())) {
                    this.addError("id", String.format("Duplicate pluggable artifacts  with id `%s` defined.", getId()));
                    existingArtifactConfig.addError("id", String.format("Duplicate pluggable artifacts  with id `%s` defined.", getId()));
                }
                if (this.getStoreId().equalsIgnoreCase(pluggableArtifactConfig.getStoreId())) {
                    if (configuration.size() == pluggableArtifactConfig.getConfiguration().size() && this.configuration.containsAll(pluggableArtifactConfig.getConfiguration())) {
                        this.addError("id", "Duplicate pluggable artifacts  configuration defined.");
                        existingArtifactConfig.addError("id", "Duplicate pluggable artifacts  configuration defined.");
                    }
                }
                return;
            }
        }
        existingArtifactConfigList.add(this);
    }

    public String toJSON() {
        final HashMap<String, Object> artifactStoreAsHashMap = new HashMap<>();
        artifactStoreAsHashMap.put("id", getId());
        artifactStoreAsHashMap.put("storeId", getStoreId());
        artifactStoreAsHashMap.put("configuration", this.getConfiguration().getConfigurationAsMap(true));
        return new Gson().toJson(artifactStoreAsHashMap);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty() || configuration.hasErrors();
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @PostConstruct
    public void encryptSecureConfigurations() {
        if (artifactStore != null && hasPluginInfo()) {
            for (ConfigurationProperty configuration : getConfiguration()) {
                configuration.handleSecureValueConfiguration(isSecure(configuration.getConfigKeyName()));
            }
        }
    }

    public  void setArtifactStore(ArtifactStore artifactStore) {
        this.artifactStore = artifactStore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluggableArtifactConfig that = (PluggableArtifactConfig) o;

        if (!id.equals(that.id)) return false;
        if (!storeId.equals(that.storeId)) return false;
        return configuration != null ? configuration.equals(that.configuration) : that.configuration == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + storeId.hashCode();
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PluggableArtifactConfig{" +
                "id='" + id + '\'' +
                ", storeId='" + storeId + '\'' +
                '}';
    }

    private boolean isSecure(String configKeyName) {
        ArtifactPluginInfo pluginInfo = getPluginInfo();
        return pluginInfo != null
                && pluginInfo.getArtifactConfigSettings() != null
                && pluginInfo.getArtifactConfigSettings().getConfiguration(configKeyName) != null
                && pluginInfo.getArtifactConfigSettings().getConfiguration(configKeyName).isSecure();

    }

    private boolean hasPluginInfo() {
        return getPluginInfo() != null;
    }

    private ArtifactPluginInfo getPluginInfo() {
        return ArtifactMetadataStore.instance().getPluginInfo(artifactStore.getPluginId());
    }
}
